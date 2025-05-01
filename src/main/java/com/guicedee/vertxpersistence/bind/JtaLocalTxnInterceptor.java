package com.guicedee.vertxpersistence.bind;

import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.TransactionalCallable;
import com.guicedee.vertxpersistence.implementations.VertxPersistenceModule;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.reactive.mutiny.Mutiny;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 * Updated to work with Hibernate Reactive and Mutiny threads.
 */
@CallScope
@Slf4j
class JtaLocalTxnInterceptor implements MethodInterceptor
{

    private JtaPersistService emProvider;
    private final ConnectionBaseInfo connectionBaseInfo;
    private UnitOfWork unitOfWork;

    @Transactional
    private static class Internal
    {
    }

    // Key for storing reactive UnitOfWork state in CallScopeProperties
    private static final String IS_REACTIVE_KEY = "isReactiveUnitOfWork" ;

    public JtaLocalTxnInterceptor(JtaPersistService emProvider, ConnectionBaseInfo connectionBaseInfo)
    {
        this.emProvider = emProvider;
        this.connectionBaseInfo = connectionBaseInfo;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable
    {
        String name = connectionBaseInfo.getPersistenceUnitName();
        if (methodInvocation.getMethod().isAnnotationPresent(Named.class))
        {
            name = methodInvocation.getMethod().getAnnotation(Named.class).value();
        }
        if (methodInvocation.getMethod().isAnnotationPresent(jakarta.inject.Named.class))
        {
            name = methodInvocation.getMethod().getAnnotation(jakarta.inject.Named.class).value();
        }

        // Get the package name of the calling class to determine which SqlClient to use
        String callingPackage = methodInvocation.getThis().getClass().getPackage().getName();

        // Get the UnitOfWork based on the package name and entity manager name
        if (methodInvocation.getMethod().isAnnotationPresent(com.guicedee.vertxpersistence.annotations.EntityManager.class))
        {
            // If the method has @EntityManager annotation, use it to get the UnitOfWork
            unitOfWork = IGuiceContext.get(Key.get(UnitOfWork.class, com.guicedee.vertxpersistence.annotations.EntityManager.class));
        }
        else if (unitOfWork == null)
        {
            // Otherwise, use the named UnitOfWork
            unitOfWork = IGuiceContext.get(Key.get(UnitOfWork.class, Names.named(name)));
        }

        // Check if the UnitOfWork is reactive
        boolean isReactive = false;
        if (unitOfWork instanceof JtaUnitOfWork)
        {
            isReactive = ((JtaUnitOfWork) unitOfWork).isReactive();
            // Store reactive state in CallScopeProperties
            CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
            csp.getProperties().put(IS_REACTIVE_KEY, isReactive);
        }
        // Check if we're in a call scope and handle transaction context transfer
        CallScoper callScoper = IGuiceContext.get(CallScoper.class);
        if (callScoper.isStartedScope())
        {
            CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
            if (csp.getProperties().containsKey("startedOnThisThread"))
            {
                boolean startedOnThisThread = (boolean) csp.getProperties().get("startedOnThisThread");

                // Begin the unit of work if needed, replacing didWeStartWork with direct unit of work management
                if (startedOnThisThread && !isUnitOfWorkActive())
                {
                    unitOfWork.begin();
                }
            }
        }

        // Begin the unit of work if needed, replacing didWeStartWork with direct unit of work management
        if (!isUnitOfWorkActive())
        {
            unitOfWork.begin();
        }

        // Read transaction metadata
        Transactional transactional = readTransactionMetadata(methodInvocation);

        // Get the entity manager or Mutiny.Session based on reactivity
        Object emOrSession;
        if (isReactive)
        {
            // For reactive UnitOfWork, get the Mutiny.Session from CallScopeProperties
            CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
            emOrSession = csp.getProperties().get(JtaPersistService.ENTITY_MANAGER_KEY);
            if (emOrSession instanceof Uni<?> uni)
            {
                emOrSession = uni.await().indefinitely();
            }
            if (!(emOrSession instanceof Mutiny.Session))
            {
                throw new IllegalStateException("Expected Mutiny.Session for reactive UnitOfWork but got: " +
                        (emOrSession != null ? emOrSession.getClass().getName() : "null"));
            }
        }
        else
        {
            // For non-reactive UnitOfWork, get the EntityManager
            emOrSession = IGuiceContext.get(Key.get(EntityManager.class, Names.named(name)));
        }

        // Check if the method returns a reactive type (CompletionStage or CompletableFuture)
        Class<?> returnType = methodInvocation.getMethod().getReturnType();
        boolean isReactiveReturn = CompletionStage.class.isAssignableFrom(returnType) ||
                CompletableFuture.class.isAssignableFrom(returnType);

        // If this is a reactive method or we have a reactive UnitOfWork, handle it differently
        if (isReactiveReturn || isReactive)
        {
            Future<Object> result =  handleReactiveMethod(methodInvocation, emOrSession, transactional);
            return result.await();
        }
        else
        {
            // Handle synchronous method (traditional approach)
            return handleSynchronousMethod(methodInvocation, (EntityManager) emOrSession, transactional);
        }
    }

    /**
     * Handles methods that return reactive types (CompletionStage or CompletableFuture)
     * or methods that use a reactive UnitOfWork.
     * Uses TransactionalCallable to ensure the transaction is properly managed in reactive context.
     */
    private Future<Object> handleReactiveMethod(MethodInvocation methodInvocation, Object emOrSession, Transactional transactional) throws Throwable
    {
        // Get reactive state from CallScopeProperties
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        boolean isReactive = csp.getProperties().containsKey(IS_REACTIVE_KEY) &&
                (boolean) csp.getProperties().get(IS_REACTIVE_KEY);

        // For reactive UnitOfWork with Mutiny.Session
        if (isReactive && emOrSession instanceof Mutiny.Session mutinySession)
        {

            var cc = VertXPreStartup.getVertx().getOrCreateContext();
            // Create a TransactionalCallable that will execute the method in a reactive transaction
            Promise<Object> promise = Promise.promise();
            CallScoper callScoper = IGuiceContext.get(CallScoper.class);
            var values = callScoper.getValues();
            cc.runOnContext((a) -> {
                mutinySession.withTransaction((tx) -> {
                    CallScoper scoper = IGuiceContext.get(CallScoper.class);
                    scoper.setValues(values);
                    scoper.enter();
                    try
                    {
                        System.out.println("Execute in transaction");
                        CallScopeProperties csp2 = IGuiceContext.get(CallScopeProperties.class);
                        if(csp2.getProperties().containsKey(JtaPersistService.ENTITY_MANAGER_KEY))
                        {
                            var ifs = csp2.getProperties().get(JtaPersistService.ENTITY_MANAGER_KEY);
                            if(ifs instanceof Uni<?> uni)
                            {
                                log.debug("Waiting for entity manager to be ready ");
                                uni.onItem().invoke(em -> {
                                    callScoper.setValues(values);
                                    callScoper.enter();
                                    CallScopeProperties csp3 = IGuiceContext.get(CallScopeProperties.class);
                                    csp3.getProperties().put(JtaPersistService.ENTITY_MANAGER_KEY, em);
                                    csp2.getProperties().put(JtaPersistService.ENTITY_MANAGER_KEY, em);
                                    csp.getProperties().put(JtaPersistService.ENTITY_MANAGER_KEY, em);
                                    try
                                    {
                                        Object out = methodInvocation.proceed();
                                        promise.complete(out);
                                    }
                                    catch (Throwable e)
                                    {
                                        log.error("Error in transaction ", e);
                                        tx.markForRollback();
                                        if(!promise.future().isComplete())
                                        promise.fail(e);
                                        throw new RuntimeException(e);
                                    }finally
                                    {
                                        callScoper.exit();
                                    }
                                }).log().subscribe().with(res -> {
                                    System.out.println("Transaction completed ");
                                });
                            }else {
                                try
                                {
                                    Object out = methodInvocation.proceed();
                                    promise.complete(out);
                                }
                                catch (Throwable e)
                                {
                                    log.error("Error in transaction ", e);
                                    tx.markForRollback();
                                    promise.fail(e);
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                    }catch (Throwable e)
                    {
                        log.error("Error in transaction ", e);
                        tx.markForRollback();
                        if(!promise.future().isComplete())
                        promise.fail(e);
                        throw new RuntimeException(e);
                    }
                    finally
                    {
                        scoper.exit();
                    }
                    return null;
                }).log().subscribe().with(res -> {
                    System.out.println("Transaction completed ");
                });
                //return null;
            });
            return promise.future();
        }

        throw new UnsupportedOperationException("Only reactive UnitOfWork with Mutiny.Session is supported at this time.");
    }

    /**
     * Handles synchronous methods using the traditional JPA transaction approach.
     */
    private Object handleSynchronousMethod(MethodInvocation methodInvocation, EntityManager em, Transactional transactional) throws Throwable
    {
        // Allow 'joining' of transactions if there is an enclosing @Transactional method.
        if (em.getTransaction().isActive())
        {
            return methodInvocation.proceed();
        }

        final EntityTransaction txn = em.getTransaction();
        txn.begin();

        Object result;
        try
        {
            result = methodInvocation.proceed();
        }
        catch (Exception e)
        {
            // commit transaction only if rollback didn't occur
            if (rollbackIfNecessary(transactional, e, txn))
            {
                txn.commit();
            }
            // propagate whatever exception is thrown anyway
            throw e;
        }
        finally
        {
            // End the unit of work if necessary (guarded so this code doesn't run unless catch fired).
            if (!txn.isActive())
            {
                unitOfWork.end();
            }
        }

        // everything was normal so commit the txn
        try
        {
            if (txn.isActive())
            {
                if (txn.getRollbackOnly())
                {
                    txn.rollback();
                }
                else
                {
                    txn.commit();
                }
            }
        }
        finally
        {
            // End the unit of work
            unitOfWork.end();
        }

        // return result
        return result;
    }

    private Transactional readTransactionMetadata(MethodInvocation methodInvocation)
    {
        Transactional transactional = null;
        Method method = methodInvocation.getMethod();
        Class<?> targetClass = methodInvocation.getThis().getClass();
        if (methodInvocation.getMethod().isAnnotationPresent(jakarta.transaction.Transactional.class))
        {
            transactional = transform(methodInvocation.getMethod()
                    .getAnnotation(jakarta.transaction.Transactional.class));
        }
      if (null == transactional)
      {
        transactional = method.getAnnotation(Transactional.class);
      }
        if (null == transactional)
        {
            // If none on method, try the class.
            transactional = targetClass.getAnnotation(Transactional.class);
        }
        if (null == transactional)
        {
            // If there is no transactional annotation present, use the default
            transactional = Internal.class.getAnnotation(Transactional.class);
        }

        return transactional;
    }

    private Transactional transform(jakarta.transaction.Transactional transactional)
    {
        return new Transactional()
        {
            @Override
            public Class<? extends Annotation> annotationType()
            {
                return Transactional.class;
            }

            @Override
            public Class<? extends Exception>[] rollbackOn()
            {
                return transactional.rollbackOn();
            }

            @Override
            public Class<? extends Exception>[] ignore()
            {
                return transactional.dontRollbackOn();
            }
        };
    }

    /**
     * Checks if the unit of work is currently active.
     * This replaces the didWeStartWork variable with direct unit of work state checking.
     *
     * @return true if the unit of work is active, false otherwise
     */
    private boolean isUnitOfWorkActive()
    {
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        return csp.getProperties().get(JtaPersistService.ENTITY_MANAGER_KEY) != null;
    }

    /**
     * Returns True if rollback DID NOT HAPPEN (i.e. if commit should continue).
     *
     * @param transactional The metadata annotation of the method
     * @param e             The exception to test for rollback
     * @param txn           A JPA Transaction to issue rollbacks on
     */
    private boolean rollbackIfNecessary(
            Transactional transactional, Exception e, EntityTransaction txn)
    {
        boolean commit = true;

        // check rollback clauses
        for (Class<? extends Exception> rollBackOn : transactional.rollbackOn())
        {

            // if one matched, try to perform a rollback
            if (rollBackOn.isInstance(e))
            {
                commit = false;

                // check ignore clauses (supercedes rollback clause)
                for (Class<? extends Exception> exceptOn : transactional.ignore())
                {
                    // An exception to the rollback clause was found, DON'T rollback
                    // (i.e. commit and throw anyway)
                    if (exceptOn.isInstance(e))
                    {
                        commit = true;
                        break;
                    }
                }

                // rollback only if nothing matched the ignore check
                if (!commit)
                {
                    txn.rollback();
                }
                // otherwise continue to commit

                break;
            }
        }

        return commit;
    }
}

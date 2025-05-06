package com.guicedee.vertxpersistence;

import com.google.inject.Key;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.CallScopeProperties;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * A transactional callable wrapper that ensures the provided {@link Callable} runs within a transactional context.
 * This class allows for automatic transaction management and optional scope transfers while enabling the callable
 * to return results.
 *
 * Updated to work with Hibernate Reactive and Mutiny threads.
 *
 * @param <T> The generic type parameter for the result of the {@link Callable}.
 */
@Slf4j
public class TransactionalCallable<T> implements Callable<T> {

    /**
     * The {@link Callable} instance to be executed.
     */
    private Callable<T> callable;

    /**
     * The stack trace captured during instantiation to assist in debugging if errors occur.
     */
    private Exception stackTrace;

    /**
     * Contextual values to be transferred during execution.
     */
    private Map<Key<?>, Object> values;

    /**
     * Indicates if the transaction context is being transferred.
     */
    private boolean transferTransaction;

    /**
     * Default constructor that initializes the stack trace for debugging purposes.
     */
    public TransactionalCallable() {
        this.stackTrace = new Exception();
    }

    /**
     * Creates a new {@link TransactionalCallable} with the specified {@link Callable}.
     *
     * @param supplier The {@link Callable} to be executed within a transactional context.
     * @param <T>      The generic type parameter for the result of the callable.
     * @return An instance of {@link TransactionalCallable}.
     */
    public static <T> TransactionalCallable<T> of(Callable<T> supplier) {
        return of(supplier, false);
    }

    /**
     * Creates a new {@link TransactionalCallable} with the specified {@link Callable}, optionally transferring the scope.
     *
     * @param supplier      The {@link Callable} to be executed within a transactional context.
     * @param transferScope If {@code true}, transfers the current scope's context values to the callable.
     * @param <T>           The generic type parameter for the result of the callable.
     * @return An instance of {@link TransactionalCallable}.
     */
    public static <T> TransactionalCallable<T> of(Callable<T> supplier, boolean transferScope) {
        var tc = IGuiceContext.get(TransactionalCallable.class);
        tc.callable = supplier;
        if (transferScope) {
            var cs = IGuiceContext.get(CallScoper.class);
            tc.values = cs.getValues();
        }
        return tc;
    }

    /**
     * Creates a new {@link TransactionalCallable} with the specified {@link Callable}, optionally transferring the scope
     * and transaction context.
     *
     * @param supplier           The {@link Callable} to be executed within a transactional context.
     * @param transferScope      If {@code true}, transfers the current scope's context values to the callable.
     * @param transferTransaction If {@code true}, attempts to transfer the current transaction context.
     * @param <T>                The generic type parameter for the result of the callable.
     * @return An instance of {@link TransactionalCallable}.
     */
    public static <T> TransactionalCallable<T> of(Callable<T> supplier, boolean transferScope, boolean transferTransaction) {
        var tc = IGuiceContext.get(TransactionalCallable.class);
        tc.callable = supplier;

        if (transferScope) {
            var cs = IGuiceContext.get(CallScoper.class);
            tc.values = cs.getValues();
        }

        if (transferTransaction) {
            tc.transferTransaction = true;

            // For Mutiny/Reactive, we mark that this transaction wasn't started on this thread
            if (tc.values == null) {
                tc.values = new HashMap<>();
            }
            tc.values.put("startedOnThisThread", false);
        } else {
            tc.transferTransaction = false;

            // For Mutiny/Reactive, we mark that this transaction was started on this thread
            if (tc.values == null) {
                tc.values = new HashMap<>();
            }
            tc.values.put("startedOnThisThread", true);
        }

        return tc;
    }

    /**
     * Executes the callable within a transactional context.
     * This method is annotated with {@link Transactional} to ensure transaction management.
     *
     * @return The result of the executed callable.
     * @throws Exception If an exception occurs during execution.
     */
    @Transactional
    T runOnTransaction() throws Exception {
        return this.callable.call();
    }

    /**
     * Executes the wrapped {@link Callable} within a transactional context.
     * Ensures transactions are managed automatically and resets state after execution.
     *
     * @return The result of the executed {@link Callable}.
     * @throws Exception If an exception occurs during execution, it is propagated with debugging information.
     */
    @Override
    public T call() throws Exception {
        if (this.callable != null) {
            try {
                // Transfer the scope's context values if present
                if (values != null && !values.isEmpty()) {
                    IGuiceContext.get(CallScoper.class).setValues(values);
                    IGuiceContext.get(CallScoper.class).enter();

                    // Set the transaction context in the CallScopeProperties
                    if (transferTransaction) {
                        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
                        csp.getProperties().putAll(values);
                    }
                }

                // Execute the callable within a transaction
                return runOnTransaction();
            } catch (Throwable e) {
                if (stackTrace != null) {
                    e.addSuppressed(stackTrace);
                }
                throw e;
            } finally {
                // Clean up resources
                IGuiceContext.get(CallScoper.class).exit();
                values = null;
                callable = null;
            }
        }
        return null;
    }

    /**
     * Sets the {@link Callable} to be executed.
     *
     * @param callable The {@link Callable} instance to be executed.
     * @return The current {@link TransactionalCallable} instance for method chaining.
     */
    public TransactionalCallable<T> setCallable(Callable<T> callable) {
        this.callable = callable;
        return this;
    }
}

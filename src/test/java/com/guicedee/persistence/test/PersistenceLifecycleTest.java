package com.guicedee.persistence.test;

import com.guicedee.persistence.PersistenceShutdown;
import com.guicedee.persistence.PersistService;
import com.guicedee.persistence.bind.JtaPersistService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.*;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceLifecycleTest {
    static final class Service extends JtaPersistService {
        final EntityManagerFactory factory;
        final AtomicInteger creates=new AtomicInteger();
        final CountDownLatch entered=new CountDownLatch(1),release;
        Service(EntityManagerFactory factory,boolean paused) {
            super("fixture",Map.of());this.factory=factory;release=new CountDownLatch(paused?1:0);
        }
        @Override protected EntityManagerFactory createFactory() {
            creates.incrementAndGet();entered.countDown();
            try {if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Fixture create timed out");}
            catch(InterruptedException failed) {Thread.currentThread().interrupt();throw new IllegalStateException("Fixture interrupted");}
            return factory;
        }
    }
    EntityManagerFactory factory() {
        var factory=mock(EntityManagerFactory.class);when(factory.isOpen()).thenReturn(true);
        when(factory.unwrap(Mutiny.SessionFactory.class)).thenReturn(mock(Mutiny.SessionFactory.class));return factory;
    }
    static void await(Uni<Void> operation) throws Exception {operation.subscribeAsCompletionStage().toCompletableFuture().get(5,TimeUnit.SECONDS);}
    @Test void repeatedStartsAndStopsCreateAndCloseExactlyOnce() throws Exception {
        var factory=factory();var service=new Service(factory,false);
        await(service.start());await(service.start());await(service.stop());await(service.stop());
        assertEquals(1,service.creates.get());verify(factory,times(1)).close();
        assertThrows(ExecutionException.class,() -> await(service.start()));
    }
    @Test void stopBeforeSubscriptionDoesNotCreateAFactory() throws Exception {
        var factory=factory();var service=new Service(factory,false);var deferred=service.start();
        await(service.stop());assertThrows(ExecutionException.class,() -> await(deferred));
        assertEquals(0,service.creates.get());verify(factory,never()).close();
    }
    @Test void stopWaitsForAnInFlightCreationAndRejectsLateReadiness() throws Exception {
        var factory=factory();var service=new Service(factory,true);
        var start=service.start().subscribeAsCompletionStage().toCompletableFuture();
        assertTrue(service.entered.await(3,TimeUnit.SECONDS));
        var stop=service.stop().subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(stop.isDone());service.release.countDown();stop.get(3,TimeUnit.SECONDS);
        assertThrows(ExecutionException.class,() -> start.get(3,TimeUnit.SECONDS));verify(factory,times(1)).close();
    }
    @Test void unwrapFailureClosesThePartiallyCreatedFactory() throws Exception {
        var factory=factory();when(factory.unwrap(Mutiny.SessionFactory.class)).thenThrow(new IllegalStateException("fixture-unwrap"));
        var service=new Service(factory,false);assertThrows(ExecutionException.class,() -> await(service.start()));
        await(service.stop());verify(factory,times(1)).close();
    }
    @Test void closeFailureIsObservableAndNotRetried() throws Exception {
        var factory=factory();doThrow(new IllegalStateException("fixture-close")).when(factory).close();
        var service=new Service(factory,false);await(service.start());
        assertThrows(ExecutionException.class,() -> await(service.stop()));
        assertThrows(ExecutionException.class,() -> await(service.stop()));verify(factory,times(1)).close();
    }
    @Test void persistenceStopRunsEvenWithoutAnInjectorAndClosesOwnedPoolsAfterFailure() throws Exception {
        new PersistenceShutdown().onStartup();Class.forName("com.guicedee.client.scopes.CallScoper");
        var vertx=Vertx.vertx();var pool=PgBuilder.pool().using(vertx).connectingTo(new PgConnectOptions()).build();
        var subscriptions=new AtomicInteger();
        PersistenceShutdown.register(new PersistService() {
            public Uni<Void> start(){return Uni.createFrom().voidItem();}
            public Uni<Void> stop(){return Uni.createFrom().deferred(() -> {
                subscriptions.incrementAndGet();return Uni.createFrom().failure(new IllegalStateException("fixture-stop"));
            });}
        });
        PersistenceShutdown.registerOwnedPool(Map.of("hibernate.vertx.pool",pool,PersistenceShutdown.OWNED_POOL,pool));
        try {
            var stopped=PersistenceShutdown.close();
            assertThrows(ExecutionException.class,() -> stopped.get(3,TimeUnit.SECONDS));
            assertSame(stopped,PersistenceShutdown.close());assertEquals(1,subscriptions.get());
            assertThrows(ExecutionException.class,() -> pool.getConnection().toCompletionStage().toCompletableFuture().get(2,TimeUnit.SECONDS));
            assertEquals(1,vertx.executeBlocking(() -> 1).toCompletionStage().toCompletableFuture().get(2,TimeUnit.SECONDS));
        } finally {pool.close().toCompletionStage().toCompletableFuture().get(3,TimeUnit.SECONDS);vertx.close().toCompletionStage().toCompletableFuture().get(3,TimeUnit.SECONDS);new PersistenceShutdown().onStartup();}
    }
    @Test void borrowedPoolsAreNotClosedAndOwnershipCannotNameAnotherPool() throws Exception {
        new PersistenceShutdown().onStartup();
        var borrowed=mock(io.vertx.sqlclient.Pool.class);
        var other=mock(io.vertx.sqlclient.Pool.class);
        PersistenceShutdown.registerOwnedPool(Map.of("hibernate.vertx.pool",borrowed));
        assertThrows(IllegalArgumentException.class,() -> PersistenceShutdown.registerOwnedPool(
                Map.of("hibernate.vertx.pool",borrowed,PersistenceShutdown.OWNED_POOL,other)));
        try {
            PersistenceShutdown.close().get(3,TimeUnit.SECONDS);
            verify(borrowed,never()).close();verify(other,never()).close();
        } finally {new PersistenceShutdown().onStartup();}
    }
}

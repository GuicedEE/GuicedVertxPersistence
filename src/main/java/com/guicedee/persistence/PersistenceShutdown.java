package com.guicedee.persistence;

import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import java.util.*;
import java.util.concurrent.*;

/** Retains services and explicitly owned pools before injection can fail. Never resolves an injector on shutdown. */
public final class PersistenceShutdown implements IGuicePreStartup<PersistenceShutdown>, IGuicePreDestroy<PersistenceShutdown> {
    public static final String OWNED_POOL = "guicedee.persistence.ownedPool";
    private static State state = new State();
    private static final class State {
        final Set<PersistService> services = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Pool> pools = Collections.newSetFromMap(new IdentityHashMap<>());
        CompletableFuture<Void> closing;
    }
    public static synchronized void register(PersistService service) {
        if (state.closing != null) throw new IllegalStateException("Persistence is stopping");
        state.services.add(Objects.requireNonNull(service));
    }
    public static synchronized void registerOwnedPool(Map<?,?> properties) {
        Object candidate = properties.get(OWNED_POOL);
        if (candidate == null) return;
        if (!(candidate instanceof Pool pool) || candidate != properties.get("hibernate.vertx.pool"))
            throw new IllegalArgumentException("Invalid persistence pool ownership");
        if (state.closing != null) {
            pool.close(); // Late registration cannot retain a resource after shutdown has begun.
            throw new IllegalStateException("Persistence is stopping");
        }
        state.pools.add(pool);
    }
    @Override public List<Future<Boolean>> onStartup() {
        synchronized (PersistenceShutdown.class) {
            if (state.closing != null) {
                if (!state.closing.isDone()) return List.of(Future.failedFuture("Persistence is still stopping"));
                state = new State();
            }
        }
        return List.of(Future.succeededFuture(true));
    }
    @Override public Integer sortOrder() { return Integer.MIN_VALUE + 40; }
    @Override public Integer shutdownSortOrder() { return Integer.MAX_VALUE - 1000; }

    public static synchronized CompletableFuture<Void> close() {
        if (state.closing != null) return state.closing;
        State owned = state;
        owned.closing = new CompletableFuture<>();
        Thread.ofVirtual().name("guicedee-persistence-shutdown").start(() -> {
            Throwable failure = null;
            try {
                var stops = owned.services.stream().map(service -> invoke(() ->
                        service.stop().subscribeAsCompletionStage().toCompletableFuture())).toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(stops).get(10, TimeUnit.SECONDS);
            } catch (Throwable failed) { failure = failed; }
            try {
                var closes = owned.pools.stream().map(pool -> invoke(() ->
                        pool.close().toCompletionStage().toCompletableFuture())).toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(closes).get(10, TimeUnit.SECONDS);
            } catch (Throwable failed) { if (failure == null) failure = failed; }
            owned.services.clear();owned.pools.clear();
            if (failure == null) owned.closing.complete(null);
            else owned.closing.completeExceptionally(new IllegalStateException("Persistence shutdown failed"));
        });
        return owned.closing;
    }
    private static CompletableFuture<Void> invoke(java.util.function.Supplier<CompletableFuture<Void>> operation) {
        var result = new CompletableFuture<Void>();
        Thread.ofVirtual().name("guicedee-persistence-resource-stop").start(() -> {
            try { operation.get().whenComplete((ignored, failure) -> {
                if (failure == null) result.complete(null);else result.completeExceptionally(failure);
            }); } catch (Throwable failed) { result.completeExceptionally(failed); }
        });
        return result;
    }
    @Override public void onDestroy() {
        if (io.vertx.core.Context.isOnEventLoopThread()) throw new IllegalStateException("Shutdown cannot block an event loop");
        try { close().get(22, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt();throw new IllegalStateException("Persistence shutdown interrupted"); }
        catch (Exception failed) { throw new IllegalStateException("Persistence shutdown failed"); }
    }
}

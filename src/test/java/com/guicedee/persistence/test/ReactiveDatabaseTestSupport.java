package com.guicedee.persistence.test;

import com.guicedee.vertx.spi.VertXPreStartup;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ReactiveDatabaseTestSupport {
    private ReactiveDatabaseTestSupport() {
    }

    static void assertQuery(Mutiny.SessionFactory factory, String sql) throws Exception {
        CompletableFuture<List<?>> result = new CompletableFuture<>();
        VertXPreStartup.getVertx().runOnContext(ignored -> {
            try {
                factory.withSession(session -> session.createNativeQuery(sql).getResultList())
                        .subscribe().with(result::complete, result::completeExceptionally);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        // Await on the JUnit thread, so asynchronous query failures fail the test.
        List<?> rows = result.get(30, TimeUnit.SECONDS);
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "The native query should return a row");
    }
}

package com.guicedee.persistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import io.vertx.cassandra.CassandraClient;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Cassandra support using Testcontainers.
 * <p>
 * Starts a Cassandra container, configures the {@link TestCassandraModule} via system properties,
 * boots the GuicedEE context, and verifies that:
 * <ol>
 *   <li>A {@link CassandraClient} is properly bound and injectable</li>
 *   <li>CQL queries can be executed</li>
 *   <li>Data can be inserted and retrieved</li>
 * </ol>
 */
@Testcontainers
@Log4j2
public class CassandraTest {

    @Container
    private static final CassandraContainer<?> cassandraContainer = new CassandraContainer<>("cassandra:4.1")
            .withStartupTimeout(Duration.ofMinutes(3));

    @BeforeAll
    public static void setup() {
        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

        cassandraContainer.start();

        System.setProperty("cassandra.host", cassandraContainer.getHost());
        System.setProperty("cassandra.port", String.valueOf(cassandraContainer.getMappedPort(9042)));

        log.info("Cassandra container started at {}:{}", cassandraContainer.getHost(),
                cassandraContainer.getMappedPort(9042));
    }

    @Test
    public void testCassandraClientBinding() {
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        CassandraClient namedClient = IGuiceContext.get(Key.get(CassandraClient.class, Names.named("testCassandra")));
        assertNotNull(namedClient, "CassandraClient @Named(\"testCassandra\") should not be null");

        CassandraClient defaultClient = IGuiceContext.get(CassandraClient.class);
        assertNotNull(defaultClient, "Default CassandraClient should not be null");

        log.info("✅ CassandraClient successfully injected");
    }

    @Test
    public void testExecuteQuery() throws Exception {
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        CassandraClient client = IGuiceContext.get(CassandraClient.class);
        assertNotNull(client);

        CountDownLatch latch = new CountDownLatch(1);

        // Create a keyspace and table, insert data, then query
        client.execute("CREATE KEYSPACE IF NOT EXISTS test_ks WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
                .onComplete(ksResult -> {
                    if (ksResult.failed()) {
                        log.error("❌ Keyspace creation failed", ksResult.cause());
                        latch.countDown();
                        return;
                    }
                    client.execute("CREATE TABLE IF NOT EXISTS test_ks.books (id text PRIMARY KEY, title text, author text)")
                            .onComplete(tableResult -> {
                                if (tableResult.failed()) {
                                    log.error("❌ Table creation failed", tableResult.cause());
                                    latch.countDown();
                                    return;
                                }
                                client.execute("INSERT INTO test_ks.books (id, title, author) VALUES ('1', 'The Hobbit', 'J. R. R. Tolkien')")
                                        .onComplete(insertResult -> {
                                            if (insertResult.failed()) {
                                                log.error("❌ Insert failed", insertResult.cause());
                                                latch.countDown();
                                                return;
                                            }
                                            client.executeWithFullFetch("SELECT * FROM test_ks.books WHERE id = '1'")
                                                    .onComplete(ar -> {
                                                        if (ar.succeeded()) {
                                                            var rows = ar.result();
                                                            assertFalse(rows.isEmpty(), "Should have at least one row");
                                                            var row = rows.get(0);
                                                            assertEquals("The Hobbit", row.getString("title"));
                                                            assertEquals("J. R. R. Tolkien", row.getString("author"));
                                                            log.info("✅ Query result: title={}, author={}", row.getString("title"), row.getString("author"));
                                                        } else {
                                                            log.error("❌ Query failed", ar.cause());
                                                        }
                                                        latch.countDown();
                                                    });
                                        });
                            });
                });

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Operation should complete within 60 seconds");
    }
}




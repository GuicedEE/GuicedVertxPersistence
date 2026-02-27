package com.guicedee.vertxpersistence.test;

import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import com.guicedee.vertxpersistence.test.vert1.Verticle1Service;
import com.guicedee.vertxpersistence.test.vert2.Verticle2Service;
import com.guicedee.vertxpersistence.test.vert3.Verticle3Service;
import com.guicedee.vertxpersistence.test.vert4.Verticle4Service;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.guicedee.vertxpersistence.test.PostgresTest.*;

@Log4j2
public class TestMultiConnectionsUni {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName(POSTGRES_DATABASE)
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASSWORD);

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject Verticle1Service verticle1Service;
    @Inject Verticle2Service verticle2Service;
    @Inject Verticle3Service verticle3Service;
    @Inject Verticle4Service verticle4Service;

    @BeforeAll
    static void init() {
        log.info("Starting TestMultiConnectionsUni");

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        System.setProperty("log4j.level", "INFO");
        LogUtils.addHighlightedConsoleLogger(Level.DEBUG);

        postgresContainer.start();

        System.setProperty("postgres.host", postgresContainer.getHost());
        POSTGRES_PORT = String.valueOf(postgresContainer.getMappedPort(5432));
        System.setProperty("postgres.port", String.valueOf(postgresContainer.getMappedPort(5432)));
        System.setProperty("postgres.database", postgresContainer.getDatabaseName());
        System.setProperty("postgres.user", postgresContainer.getUsername());
        System.setProperty("postgres.password", postgresContainer.getPassword());

        System.setProperty("system.hibernate.show_sql", "true");
        System.setProperty("system.hibernate.format_sql", "true");
        System.setProperty("system.hibernate.use_sql_comments", "true");
        System.setProperty("system.hazelcast.show_sql", "true");

        // Register test module and bootstrap Guice
        IGuiceContext.registerModule("guiced.vertx.persistence.test");
        IGuiceContext.instance().inject();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testOperationsMultiVerticalUsingUni() throws InterruptedException {
        log.info("TestMultiConnectionsUni started");
        TestMultiConnectionsUni instance = IGuiceContext.get(TestMultiConnectionsUni.class);
        log.info("The session factory is : {}", instance.sessionFactory);

        CountDownLatch latch = new CountDownLatch(4);

        Instant start = Instant.now();

        // Start four Uni-based service calls concurrently via event-bus publishers
        Uni<Boolean> u1 = instance.verticle1Service.perform().invoke(v -> latch.countDown());
        Uni<Boolean> u2 = instance.verticle2Service.perform().invoke(v -> latch.countDown());
        Uni<Boolean> u3 = instance.verticle3Service.perform().invoke(v -> latch.countDown());
        Uni<Boolean> u4 = instance.verticle4Service.perform().invoke(v -> latch.countDown());

        // Subscribe (non-blocking) to begin execution on their respective Vert.x contexts
        u1.subscribe().with(v -> {}, err -> latch.countDown());
        u2.subscribe().with(v -> {}, err -> latch.countDown());
        u3.subscribe().with(v -> {}, err -> latch.countDown());
        u4.subscribe().with(v -> {}, err -> latch.countDown());

        // Take mid-run snapshots while jobs are holding connections
        snapshotPgActivity("mid-run #1");
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        snapshotPgActivity("mid-run #2");

        // Await completion of all
        boolean finished = latch.await(2, TimeUnit.MINUTES);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        log.info("[TEST-UNI] Completed 4 service DB ops in {} ms (finished={})", elapsedMs, finished);

        // Post-run snapshot
        snapshotPgActivity("post-run");
    }

    private void snapshotPgActivity(String label) {
        try {
            PostgresConnectionBaseInfo ci = new PostgresConnectionBaseInfo();
            ci.setServerName(POSTGRES_HOST);
            ci.setPort(String.valueOf(POSTGRES_PORT));
            ci.setDatabaseName(POSTGRES_DATABASE);
            ci.setUsername(POSTGRES_USER);
            ci.setPassword(POSTGRES_PASSWORD);
            ci.setReactive(true);

            io.vertx.sqlclient.SqlClient client = ci.toPooledDatasource();
            java.util.concurrent.CompletableFuture<Map<String, Long>> cf = new java.util.concurrent.CompletableFuture<>();
            String sql = "select coalesce(state,'<null>') as state, count(*) as cnt from pg_stat_activity where usename = current_user group by state";
            client.query(sql).execute().onComplete(ar -> {
                try {
                    if (ar.succeeded()) {
                        io.vertx.sqlclient.RowSet<io.vertx.sqlclient.Row> rows = ar.result();
                        java.util.HashMap<String, Long> map = new java.util.HashMap<>();
                        for (io.vertx.sqlclient.Row r : rows) {
                            String state = r.getString("state");
                            Long cnt = r.getLong("cnt");
                            if (state == null) state = "<null>";
                            map.put(state, cnt == null ? 0L : cnt);
                        }
                        cf.complete(map);
                    } else {
                        cf.completeExceptionally(ar.cause());
                    }
                } finally {
                    try { client.close(); } catch (Throwable ignored) {}
                }
            });

            Map<String, Long> counts = cf.get(5, java.util.concurrent.TimeUnit.SECONDS);
            long active = counts.getOrDefault("active", 0L);
            long idle = counts.getOrDefault("idle", 0L);
            long other = counts.entrySet().stream()
                    .filter(e -> !e.getKey().equals("active") && !e.getKey().equals("idle"))
                    .mapToLong(Map.Entry::getValue).sum();
            log.info("[PG-ACTIVITY] {} -> active={} idle={} other={} detail={}",
                    label, active, idle, other, counts);
        } catch (Throwable t) {
            log.warn("[PG-ACTIVITY] {} -> failed to query pg_stat_activity: {}", label, t.toString());
        }
    }
}

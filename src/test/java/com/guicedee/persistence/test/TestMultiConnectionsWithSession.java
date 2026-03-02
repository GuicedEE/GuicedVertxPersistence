package com.guicedee.persistence.test;

import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import com.guicedee.persistence.implementations.postgres.PostgresConnectionBaseInfo;
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

import static com.guicedee.persistence.test.PostgresTest.*;

@Log4j2
public class TestMultiConnectionsWithSession {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName(POSTGRES_DATABASE)
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASSWORD);

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @BeforeAll
    static void init() {
        log.info("Starting TestMultiConnectionsWithSession");

        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        System.setProperty("log4j.level", "INFO");
        LogUtils.addHighlightedConsoleLogger(Level.DEBUG);
        IGuiceContext.registerModule("guiced.persistence.test");

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

        IGuiceContext.instance().inject();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testWithSessionParallelOps() throws InterruptedException {
        log.info("TestMultiConnectionsWithSession started");
        TestMultiConnectionsWithSession instance = IGuiceContext.get(TestMultiConnectionsWithSession.class);
        log.info("The session factory is : {}", instance.sessionFactory);

        CountDownLatch latch = new CountDownLatch(4);
        Instant start = Instant.now();

        // Build four Unis using sessionFactory.withSession + withTransaction
        Uni<Boolean> u1 = instance.sessionFactory.withSession(s ->
                s.withTransaction(tx -> s.createNativeQuery("select pg_sleep(1)").getSingleResult())
        ).replaceWith(Boolean.TRUE).invoke(v -> latch.countDown());

        Uni<Boolean> u2 = instance.sessionFactory.withSession(s ->
                s.withTransaction(tx -> s.createNativeQuery("select pg_sleep(1)").getSingleResult())
        ).replaceWith(Boolean.TRUE).invoke(v -> latch.countDown());

        Uni<Boolean> u3 = instance.sessionFactory.withSession(s ->
                s.withTransaction(tx -> s.createNativeQuery("select pg_sleep(1)").getSingleResult())
        ).replaceWith(Boolean.TRUE).invoke(v -> latch.countDown());

        Uni<Boolean> u4 = instance.sessionFactory.withSession(s ->
                s.withTransaction(tx -> s.createNativeQuery("select pg_sleep(1)").getSingleResult())
        ).replaceWith(Boolean.TRUE).invoke(v -> latch.countDown());

        // Subscribe (non-blocking) so all four start
        u1.subscribe().with(v -> {}, err -> latch.countDown());
        u2.subscribe().with(v -> {}, err -> latch.countDown());
        u3.subscribe().with(v -> {}, err -> latch.countDown());
        u4.subscribe().with(v -> {}, err -> latch.countDown());

        // Mid-run snapshots
        snapshotPgActivity("mid-run #1");
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        snapshotPgActivity("mid-run #2");

        // Await completion
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        log.info("[TEST-WITH-SESSION] Completed 4 DB ops in {} ms (finished={})", elapsedMs, finished);

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

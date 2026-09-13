package com.guicedee.persistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.persistence.PersistService;
import com.guicedee.persistence.bind.JtaPersistService;
import com.guicedee.vertx.spi.VertXPreStartup;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SQL Server reactive support using Testcontainers and Hibernate Reactive.
 * <p>
 * Starts a SQL Server container, configures the {@link TestModuleMSSQLReactive} via system properties,
 * boots the GuicedEE context, and verifies that:
 * <ol>
 *   <li>The {@link JtaPersistService} is properly bound and can be started</li>
 *   <li>A {@link Mutiny.SessionFactory} is properly bound and injectable</li>
 *   <li>Sessions can be opened and native queries executed</li>
 * </ol>
 */
@Testcontainers
@Log4j2
public class MSSQLReactiveTest {

    static final String MSSQL_PASSWORD = "YourStrong@Passw0rd";

    @SuppressWarnings("resource")
    @Container
    private static final MSSQLServerContainer mssqlContainer = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense()
            .withPassword(MSSQL_PASSWORD)
            .withStartupTimeout(Duration.ofMinutes(2));

    @BeforeAll
    public static void setup() {
        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

        mssqlContainer.start();

        System.setProperty("mssql.host", mssqlContainer.getHost());
        System.setProperty("mssql.port", String.valueOf(mssqlContainer.getMappedPort(1433)));
        System.setProperty("mssql.database", "master");
        System.setProperty("mssql.user", "sa");
        System.setProperty("mssql.password", MSSQL_PASSWORD);

        log.info("MSSQL container started at {}:{}", mssqlContainer.getHost(),
                mssqlContainer.getMappedPort(1433));
    }

    @Test
    public void testReactiveMSSQLConnection() {
        IGuiceContext.registerModule("guiced.persistence.test");
        IGuiceContext.getContext().inject();

        try {
            // Get the PersistService from Guice
            log.info("Attempting to get PersistService from Guice");
            JtaPersistService ps = (JtaPersistService) IGuiceContext.get(
                    Key.get(PersistService.class, Names.named("testMSSQLReactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the SessionFactory from Guice
            log.info("Attempting to get SessionFactory from Guice");
            Mutiny.SessionFactory sessionFactory = IGuiceContext.get(
                    Key.get(Mutiny.SessionFactory.class, Names.named("testMSSQLReactive")));
            assertNotNull(sessionFactory, "SessionFactory should not be null");
            log.info("✅ Successfully got SessionFactory from Guice");

            // Test opening a session and executing a native query on the Vert.x context
            ReactiveDatabaseTestSupport.assertQuery(sessionFactory, "SELECT 1");

            var connectionInfo = com.guicedee.persistence.implementations.VertxPersistenceModule
                    .getConnectionModules().keySet().stream()
                    .filter(info -> "testMSSQLReactive".equals(info.getPersistenceUnitName()))
                    .findFirst().orElseThrow();
            var client = connectionInfo.toPooledDatasource();
            assertNotNull(client, "The native Vert.x MSSQL pool must be created");
            try {
                var rows = client.query("SELECT 1 AS result").execute().toCompletionStage()
                        .toCompletableFuture().get(30, java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(1, rows.iterator().next().getInteger("result"));
            } finally {
                client.close().toCompletionStage().toCompletableFuture()
                        .get(30, java.util.concurrent.TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            log.error("Test failed", e);
            fail("Test failed: " + e.getMessage(), e);
        }
    }
}


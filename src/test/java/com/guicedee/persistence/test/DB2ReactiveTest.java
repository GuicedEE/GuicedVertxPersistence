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
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for DB2 reactive support using Testcontainers and Hibernate Reactive.
 * <p>
 * Starts a DB2 container, configures the {@link TestModuleDB2Reactive} via system properties,
 * boots the GuicedEE context, and verifies that:
 * <ol>
 *   <li>The {@link JtaPersistService} is properly bound and can be started</li>
 *   <li>A {@link Mutiny.SessionFactory} is properly bound and injectable</li>
 *   <li>Sessions can be opened and native queries executed</li>
 * </ol>
 */
@Testcontainers
@Log4j2
@Disabled("Hibernate Reactive does not yet implement InformationExtractor for DB2Dialect — waiting on upstream support")
public class DB2ReactiveTest {

    static final String DB2_DATABASE = "testdb";
    static final String DB2_USER = "db2inst1";
    static final String DB2_PASSWORD = "password";

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> db2Container = new GenericContainer<>("icr.io/db2_community/db2:latest")
            .withExposedPorts(50000)
            .withEnv("DB2INSTANCE", DB2_USER)
            .withEnv("DB2INST1_PASSWORD", DB2_PASSWORD)
            .withEnv("DBNAME", DB2_DATABASE)
            .withEnv("LICENSE", "accept")
            .withEnv("ARCHIVE_LOGS", "false")
            .withEnv("AUTOCONFIG", "false")
            .withPrivilegedMode(true)
            .waitingFor(Wait.forLogMessage(".*Setup has completed.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));

    @BeforeAll
    public static void setup() {
        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

        db2Container.start();

        System.setProperty("db2.host", db2Container.getHost());
        System.setProperty("db2.port", String.valueOf(db2Container.getMappedPort(50000)));
        System.setProperty("db2.database", DB2_DATABASE);
        System.setProperty("db2.user", DB2_USER);
        System.setProperty("db2.password", DB2_PASSWORD);

        log.info("DB2 container started at {}:{}", db2Container.getHost(),
                db2Container.getMappedPort(50000));
    }

    @Test
    public void testReactiveDB2Connection() {
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        try {
            // Get the PersistService from Guice
            log.info("Attempting to get PersistService from Guice");
            JtaPersistService ps = (JtaPersistService) IGuiceContext.get(
                    Key.get(PersistService.class, Names.named("testDB2Reactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the SessionFactory from Guice
            log.info("Attempting to get SessionFactory from Guice");
            Mutiny.SessionFactory sessionFactory = IGuiceContext.get(
                    Key.get(Mutiny.SessionFactory.class, Names.named("testDB2Reactive")));
            assertNotNull(sessionFactory, "SessionFactory should not be null");
            log.info("✅ Successfully got SessionFactory from Guice");

            // Test opening a session and executing a native query on the Vert.x context
            VertXPreStartup.getVertx().runOnContext(handle -> {
                log.info("Testing session open...");
                sessionFactory.openSession()
                        .onItemOrFailure().invoke((session, error) -> {
                            if (error != null) {
                                fail("Error occurred while opening a session", error);
                            } else {
                                log.info("✅ Session opened successfully");
                            }
                            session.close();
                        })
                        .onFailure().invoke(error -> fail("Error occurred while opening a session", error))
                        .await().atMost(Duration.of(30, ChronoUnit.SECONDS));

                log.info("Testing withSession + native query...");
                sessionFactory.withSession(session -> {
                            log.info("Inside withSession");
                            return session.createNativeQuery("VALUES 1")
                                    .getResultList()
                                    .onItem().invoke(result -> {
                                        assertNotNull(result, "Query result should not be null");
                                        assertFalse(result.isEmpty(), "Query result should not be empty");
                                        log.info("✅ Native query result: {}", result);
                                    });
                        }).onFailure().invoke(error -> fail("Error occurred while executing query", error))
                        .await().atMost(Duration.of(30, ChronoUnit.SECONDS));

                log.info("✅ All DB2 reactive tests passed");
            });

        } catch (Exception e) {
            log.error("Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }
}



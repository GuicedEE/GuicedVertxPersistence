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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Oracle reactive support using Testcontainers and Hibernate Reactive.
 * <p>
 * Starts an Oracle Free container, configures the {@link TestModuleOracleReactive} via system properties,
 * boots the GuicedEE context, and verifies that:
 * <ol>
 *   <li>The {@link JtaPersistService} is properly bound and can be started</li>
 *   <li>A {@link Mutiny.SessionFactory} is properly bound and injectable</li>
 *   <li>Sessions can be opened and native queries executed</li>
 * </ol>
 */
@Testcontainers
@Log4j2
public class OracleReactiveTest {

    static final String ORACLE_PASSWORD = "oracle";

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> oracleContainer = new GenericContainer<>("gvenzl/oracle-free:slim-faststart")
            .withExposedPorts(1521)
            .withEnv("ORACLE_PASSWORD", ORACLE_PASSWORD)
            .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*", 1))
            .withStartupTimeout(Duration.ofMinutes(3));

    @BeforeAll
    public static void setup() {
        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

        oracleContainer.start();

        System.setProperty("oracle.host", oracleContainer.getHost());
        System.setProperty("oracle.port", String.valueOf(oracleContainer.getMappedPort(1521)));
        System.setProperty("oracle.database", "FREEPDB1");
        System.setProperty("oracle.user", "system");
        System.setProperty("oracle.password", ORACLE_PASSWORD);

        log.info("Oracle container started at {}:{}", oracleContainer.getHost(),
                oracleContainer.getMappedPort(1521));
    }

    @Test
    public void testReactiveOracleConnection() {
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        try {
            // Get the PersistService from Guice
            log.info("Attempting to get PersistService from Guice");
            JtaPersistService ps = (JtaPersistService) IGuiceContext.get(
                    Key.get(PersistService.class, Names.named("testOracleReactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the SessionFactory from Guice
            log.info("Attempting to get SessionFactory from Guice");
            Mutiny.SessionFactory sessionFactory = IGuiceContext.get(
                    Key.get(Mutiny.SessionFactory.class, Names.named("testOracleReactive")));
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
                            return session.createNativeQuery("SELECT 1 FROM DUAL")
                                    .getResultList()
                                    .onItem().invoke(result -> {
                                        assertNotNull(result, "Query result should not be null");
                                        assertFalse(result.isEmpty(), "Query result should not be empty");
                                        log.info("✅ Native query result: {}", result);
                                    });
                        }).onFailure().invoke(error -> fail("Error occurred while executing query", error))
                        .await().atMost(Duration.of(30, ChronoUnit.SECONDS));

                log.info("✅ All Oracle reactive tests passed");
            });

        } catch (Exception e) {
            log.error("Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }
}


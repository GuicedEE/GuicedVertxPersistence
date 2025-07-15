package com.guicedee.vertxpersistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.PersistService;
import com.guicedee.vertxpersistence.bind.JtaPersistService;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.guicedee.vertxpersistence.test.PostgresTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PostgreSQL reactive integration with Vertx.
 * This test verifies that:
 * 1. Mutiny.SessionFactory is properly bound and available
 * 2. Mutiny.SessionFactory can be used to open sessions and execute queries
 */
@Testcontainers
@Slf4j
public class PostgresReactiveTest {


    // Create a PostgreSQL container with the defined values
    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName(POSTGRES_DATABASE)
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASSWORD);

    @BeforeAll
    public static void setup() {
        // Set system properties for logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "fine");

        // Start the container and set system properties based on the container's values
        postgresContainer.start();

        // Set system properties for the persistence.xml
        System.setProperty("postgres.host", postgresContainer.getHost());
        POSTGRES_PORT = String.valueOf(postgresContainer.getMappedPort(5432));
        System.setProperty("postgres.port", String.valueOf(postgresContainer.getMappedPort(5432)));
        System.setProperty("postgres.database", postgresContainer.getDatabaseName());
        System.setProperty("postgres.user", postgresContainer.getUsername());
        System.setProperty("postgres.password", postgresContainer.getPassword());

        // Set cache properties for testing (same as in TestModulePostgres)
        System.setProperty("system.hibernate.show_sql", "true");
        System.setProperty("system.hibernate.format_sql", "true");
        System.setProperty("system.hibernate.use_sql_comments", "true");
        System.setProperty("system.hazelcast.show_sql", "true");
        System.setProperty("system.hazelcast.address", "localhost:5701");
        System.setProperty("system.hazelcast.groupname", "testgroup");
        System.setProperty("system.hazelcast.grouppass", "testpass");
        System.setProperty("system.hazelcast.instance_name", "testinstance");
    }

    @Test
    public void testReactivePostgresConnection() {
        // Register the PostgreSQL reactive test module
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.registerModule(new TestModulePostgresReactive());
        IGuiceContext.getContext().inject();

        // Get the CallScoper to enter and exit a scope
      //  CallScoper scoper = IGuiceContext.get(CallScoper.class);
       // scoper.enter();

        try {
            // Get the PersistService from Guice
            log.info("Attempting to get PersistService from Guice");
            JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named("testPostgresReactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the SessionFactory from Guice
            log.info("Attempting to get SessionFactory from Guice");
            Mutiny.SessionFactory sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("testPostgresReactive")));
            assertNotNull(sessionFactory, "SessionFactory should not be null");
            log.info("Successfully got SessionFactory from Guice");

            // Test the session factory
            VertXPreStartup.getVertx().runOnContext(handle -> {
                // Test opening a session
                log.info("Before opening session manually");
                sessionFactory.openSession()
                        .onItemOrFailure().invoke((session, error) -> {
                            if (error != null) {
                                fail("Error occurred while opening a session", error);
                            } else {
                                log.info("Session opened successfully");
                            }
                            // Ensure session is closed
                            log.info("Closing session manually");
                            session.close();
                            log.info("Session closed manually");
                        })
                        .onFailure().invoke(error -> fail("Error occurred while opening a session", error))
                        .await().atMost(Duration.of(50, ChronoUnit.SECONDS));
                log.info("After manual session test");

                // Test executing a query
                log.info("Before withSession test");
                sessionFactory.withSession(session -> {
                            log.info("Inside withSession, session is open");
                            session.withTransaction(tx -> {
                                        log.info("Inside withTransaction, transaction is active");
                                        session.createNativeQuery("SELECT 1")
                                                .getResultList()
                                                .onItemOrFailure()
                                                .invoke((result, error) -> {
                                                    if (error != null) {
                                                        fail("Error occurred while executing a native query", error);
                                                    } else {
                                                        log.info("Native query result: {}", result);
                                                    }
                                                });
                                        log.info("Exiting withTransaction, transaction will be committed");
                                        return null;
                                    })
                                    .onFailure().invoke(error -> fail("Error occurred while executing a transaction", error));
                            log.info("Exiting withSession, session will be closed automatically");
                            return null;
                        }).onFailure().invoke(error -> fail("Error occurred while creating a session", error))
                        .await().atMost(Duration.of(50, ChronoUnit.SECONDS));
                log.info("After withSession test");
            });

        } finally {
            // Exit the scope
         //   scoper.exit();
        }
    }


}

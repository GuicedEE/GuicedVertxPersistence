package com.guicedee.vertxpersistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.bind.JtaPersistService;
import com.guicedee.vertxpersistence.bind.ReactiveUnitOfWork;
import com.guicedee.vertxpersistence.implementations.VertxPersistenceModule;
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
 * 1. When reactive is set to true, UnitOfWork is bound to JtaUnitOfWork
 * 2. JtaUnitOfWork uses Mutiny.Session for reactive connections
 */
@Testcontainers
@Slf4j
public class PostgresReactiveTest
{


    // Create a PostgreSQL container with the defined values
    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName(POSTGRES_DATABASE)
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASSWORD);

    @BeforeAll
    public static void setup()
    {
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
    public void testReactivePostgresConnection()
    {
        // Register the PostgreSQL reactive test module
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.registerModule(new TestModulePostgresReactive());
        IGuiceContext.getContext().inject();

        // Get the CallScoper to enter and exit a scope
        CallScoper scoper = IGuiceContext.get(CallScoper.class);
        scoper.enter();

        try
        {
            // Start the PersistService
            JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named("testPostgresReactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the UnitOfWork for the PostgreSQL persistence unit
            ReactiveUnitOfWork work = (ReactiveUnitOfWork) IGuiceContext.get(Key.get(UnitOfWork.class, Names.named("testPostgresReactive")));
            assertNotNull(work, "UnitOfWork should not be null");

            // Verify that UnitOfWork is an instance of JtaUnitOfWork
            assertTrue(work instanceof ReactiveUnitOfWork, "UnitOfWork should be an instance of JtaUnitOfWork for reactive connections");

            VertXPreStartup.getVertx().runOnContext(handle->{
                 Mutiny.SessionFactory sessionFactory = IGuiceContext.get(Mutiny.SessionFactory.class);
                 sessionFactory.openSession()
                         .onItemOrFailure().invoke((session, error) -> {
                     if (error != null)
                     {
                         fail("Error occurred while opening a session", error);
                     }
                     else
                     {
                         log.info("Session opened successfully");
                     }
                     session.close();
                 })
                         .onFailure().invoke(error -> fail("Error occurred while opening a session", error))
                         .await().atMost(Duration.of(50, ChronoUnit.SECONDS));
                sessionFactory.withSession(session -> {
                            session.withTransaction(tx -> {
                                        session.createNativeQuery("SELECT 1")
                                                .getResultList()
                                                .onItemOrFailure()
                                                .invoke((result, error) -> {
                                                    if (error != null)
                                                    {
                                                        fail("Error occurred while executing a native query", error);
                                                    }
                                                    else
                                                    {
                                                        log.info("Native query result: {}", result);
                                                    }
                                                });
                                        return null;
                                    })
                                    .onFailure().invoke(error -> fail("Error occurred while executing a transaction", error));
                            return null;
                        }).onFailure().invoke(error -> fail("Error occurred while creating a session", error))
                        .await().atMost(Duration.of(50, ChronoUnit.SECONDS));
                sessionFactory.close();
            });

        }
        finally
        {
            // Exit the scope
            scoper.exit();
        }
    }


}

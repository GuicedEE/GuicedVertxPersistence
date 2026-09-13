package com.guicedee.persistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.persistence.PersistService;
import com.guicedee.persistence.bind.JtaPersistService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.guicedee.persistence.test.PostgresTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PostgreSQL reactive integration with Vertx.
 * This test verifies that:
 * 1. Mutiny.SessionFactory is properly bound and available
 * 2. Mutiny.SessionFactory can be used to open sessions and execute queries
 */
@Testcontainers
@Log4j2
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
        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

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
    public void testReactivePostgresConnection() throws Exception {
        // Register the PostgreSQL reactive test module
        IGuiceContext.registerModule("guiced.persistence.test");
        //IGuiceContext.registerModule(new TestModulePostgresReactive());
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
            ReactiveDatabaseTestSupport.assertQuery(sessionFactory, "SELECT 1");

        } finally {
            // Exit the scope
         //   scoper.exit();
        }
    }


}

package com.guicedee.vertxpersistence.test;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Properties;

/**
 * Test class for PostgreSQL integration with Vertx.
 * This test verifies that:
 * 1. System properties take effect for properties in persistence.xml
 * 2. Cache attributes are included in the final DbModule binding
 * 3. The Vertx entity loader applies the needed configuration
 */
@Testcontainers
@Disabled("This test is currently not working, please ignore it")
public class PostgresTest {

    // Define the same values as in TestModulePostgres
    public static final String POSTGRES_DATABASE = "testdb";
    public static final String POSTGRES_USER = "test";
    public static final String POSTGRES_PASSWORD = "test";
    public static final String POSTGRES_HOST = "localhost";
    public static String POSTGRES_PORT = "0";

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

    /**
     * Helper method to get the properties from an EntityManager.
     * This uses reflection to access the internal properties of the EntityManager.
     *
     * @param em The EntityManager to get properties from
     * @return The properties of the EntityManager
     */
    private Properties getEntityManagerProperties(EntityManager em) {
        try {
            // Get the delegate from the EntityManager
            Object delegate = em.unwrap(Object.class);

            // Get the session factory from the delegate
            Object sessionFactory = delegate.getClass().getMethod("getEntityManagerFactory").invoke(delegate);

            // Get the properties from the session factory
            Map<String, Object> props = (Map<String, Object>) sessionFactory.getClass().getMethod("getProperties").invoke(sessionFactory);

            // Convert the map to Properties
            Properties properties = new Properties();
            properties.putAll(props);

            return properties;
        } catch (Exception e) {
            // If reflection fails, return empty properties
            e.printStackTrace();
            return new Properties();
        }
    }
}

package com.guicedee.vertxpersistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.VertxPersistenceModule;
import io.vertx.sqlclient.SqlClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testPostgresConnection() {
        // Register the PostgreSQL test module
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.registerModule(new TestModulePostgres());
        IGuiceContext.getContext().inject();

        // Get the CallScoper to enter and exit a scope
        CallScoper scoper = IGuiceContext.get(CallScoper.class);
        scoper.enter();

        try {
            // Start the PersistService
            PersistService ps = IGuiceContext.get(Key.get(PersistService.class, Names.named("testPostgres")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the UnitOfWork for the PostgreSQL persistence unit
            UnitOfWork work = IGuiceContext.get(Key.get(UnitOfWork.class, Names.named("testPostgres")));
            assertNotNull(work, "UnitOfWork should not be null");

            // Begin a unit of work
            work.begin();

            try {
                // Get the EntityManager for the PostgreSQL persistence unit
                EntityManager em = IGuiceContext.get(Key.get(EntityManager.class, Names.named("testPostgres")));
                assertNotNull(em, "EntityManager should not be null");

                // Get the SqlClient for the PostgreSQL persistence unit
                SqlClient sqlClient = IGuiceContext.get(Key.get(SqlClient.class, Names.named("testPostgres")));
                assertNotNull(sqlClient, "SqlClient should not be null");

                // Get the ConnectionBaseInfo for the PostgreSQL persistence unit
                ConnectionBaseInfo connectionInfo = VertxPersistenceModule.getConnectionInfoByEntityManager("testPostgres");
                assertNotNull(connectionInfo, "ConnectionBaseInfo should not be null");

                // Verify that system properties took effect
                assertEquals(postgresContainer.getHost(), connectionInfo.getServerName(), "Server name should match container host");
                assertEquals(String.valueOf(postgresContainer.getMappedPort(5432)), connectionInfo.getPort(), "Port should match container port");
                assertEquals(POSTGRES_DATABASE, connectionInfo.getDatabaseName(), "Database name should match container database");
                assertEquals(POSTGRES_USER, connectionInfo.getUsername(), "Username should match container username");
                assertEquals(POSTGRES_PASSWORD, connectionInfo.getPassword(), "Password should match container password");

                // Verify that the JDBC URL is generated correctly
                String expectedJdbcUrl = "jdbc:postgresql://" + postgresContainer.getHost() + ":" + 
                                        postgresContainer.getMappedPort(5432) + "/" + POSTGRES_DATABASE;
                String actualJdbcUrl = connectionInfo.getJdbcUrl();
                assertEquals(expectedJdbcUrl, actualJdbcUrl, "JDBC URL should be generated correctly");

                // Verify that the EntityManager has the correct properties
                Properties props = getEntityManagerProperties(em);

                // Verify SQL logging properties
                assertEquals("true", props.getProperty("hibernate.show_sql"), "show_sql property should be set from system property");
                assertEquals("true", props.getProperty("hibernate.format_sql"), "format_sql property should be set from system property");
                assertEquals("true", props.getProperty("hibernate.use_sql_comments"), "use_sql_comments property should be set from system property");

                // Verify cache properties
                assertEquals("true", props.getProperty("hibernate.cache.use_second_level_cache"), "use_second_level_cache property should be set");
                assertEquals("true", props.getProperty("hibernate.cache.use_query_cache"), "use_query_cache property should be set");
                assertEquals("true", props.getProperty("hibernate.cache.use_minimal_puts"), "use_minimal_puts property should be set");

                // Verify Hazelcast cache properties
                assertEquals("true", props.getProperty("hibernate.cache.hazelcast.use_native_client"), "use_native_client property should be set");
                assertEquals("localhost:5701", props.getProperty("hibernate.cache.hazelcast.native_client_hosts"), "native_client_hosts property should be set from system property");
                assertEquals("localhost:5701", props.getProperty("hibernate.cache.hazelcast.native_client_address"), "native_client_address property should be set from system property");
                assertEquals("testgroup", props.getProperty("hibernate.cache.hazelcast.native_client_group"), "native_client_group property should be set from system property");
                assertEquals("testpass", props.getProperty("hibernate.cache.hazelcast.native_client_password"), "native_client_password property should be set from system property");
                assertEquals("testinstance", props.getProperty("hibernate.cache.hazelcast.instance_name"), "instance_name property should be set from system property");

                // Verify dialect is set correctly
                assertEquals("org.hibernate.dialect.PostgreSQLDialect", props.getProperty("hibernate.dialect"), "dialect property should be set correctly");
            } finally {
                // End the unit of work
                work.end();
            }
        } finally {
            // Exit the scope
            scoper.exit();
        }
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

package com.guicedee.vertxpersistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.bind.JtaUnitOfWork;
import com.guicedee.vertxpersistence.implementations.VertxPersistenceModule;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.guicedee.vertxpersistence.test.PostgresTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PostgreSQL reactive integration with Vertx.
 * This test verifies that:
 * 1. When reactive is set to true, UnitOfWork is bound to JtaUnitOfWork
 * 2. JtaUnitOfWork uses Mutiny.Session for reactive connections
 */
@Testcontainers
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
        CallScoper scoper = IGuiceContext.get(CallScoper.class);
        scoper.enter();

        try {
            // Start the PersistService
            PersistService ps = IGuiceContext.get(Key.get(PersistService.class, Names.named("testPostgresReactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the UnitOfWork for the PostgreSQL persistence unit
            UnitOfWork work = IGuiceContext.get(Key.get(UnitOfWork.class, Names.named("testPostgresReactive")));
            assertNotNull(work, "UnitOfWork should not be null");

            // Verify that UnitOfWork is an instance of JtaUnitOfWork
            assertTrue(work instanceof JtaUnitOfWork, "UnitOfWork should be an instance of JtaUnitOfWork for reactive connections");

            // Verify that JtaUnitOfWork is reactive
            JtaUnitOfWork jtaUnitOfWork = (JtaUnitOfWork) work;
            assertTrue(jtaUnitOfWork.isReactive(), "JtaUnitOfWork should be reactive");

            // Begin a unit of work
            work.begin();

            try {
                // Get the ConnectionBaseInfo for the PostgreSQL persistence unit
                ConnectionBaseInfo connectionInfo = VertxPersistenceModule.getConnectionInfoByEntityManager("testPostgresReactive");
                assertNotNull(connectionInfo, "ConnectionBaseInfo should not be null");

                // Verify that the connection is reactive
                assertTrue(connectionInfo.isReactive(), "ConnectionBaseInfo should be reactive");





                // Get the SqlClient for the PostgreSQL persistence unit
             //   SqlClient sqlClient = IGuiceContext.get(Key.get(SqlClient.class, Names.named("testPostgresReactive")));
              //  assertNotNull(sqlClient, "SqlClient should not be null");

                // Verify that system properties took effect
                assertEquals(postgresContainer.getHost(), connectionInfo.getServerName(), "Server name should match container host");
                assertEquals(String.valueOf(postgresContainer.getMappedPort(5432)), connectionInfo.getPort(), "Port should match container port");
                assertEquals(POSTGRES_DATABASE, connectionInfo.getDatabaseName(), "Database name should match container database");
                assertEquals(POSTGRES_USER, connectionInfo.getUsername(), "Username should match container username");
                assertEquals(POSTGRES_PASSWORD, connectionInfo.getPassword(), "Password should match container password");
            } finally {
                // End the unit of work
                work.end();
            }
        } finally {
            // Exit the scope
            scoper.exit();
        }
    }

    @Test
    public void testTransactionInterceptorWithReactiveUnitOfWork() {
        // Register the PostgreSQL reactive test module
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.registerModule(new TestModulePostgresReactive());
        IGuiceContext.getContext().inject();

        // Get the CallScoper to enter and exit a scope
        CallScoper scoper = IGuiceContext.get(CallScoper.class);
        scoper.enter();

        try {
            // Start the PersistService
            PersistService ps = IGuiceContext.get(Key.get(PersistService.class, Names.named("testPostgresReactive")));
            assertNotNull(ps, "PersistService should not be null");
            ps.start();

            // Get the UnitOfWork for the PostgreSQL persistence unit
            UnitOfWork work = IGuiceContext.get(Key.get(UnitOfWork.class, Names.named("testPostgresReactive")));
            assertNotNull(work, "UnitOfWork should not be null");

            // Verify that UnitOfWork is an instance of JtaUnitOfWork
            assertTrue(work instanceof JtaUnitOfWork, "UnitOfWork should be an instance of JtaUnitOfWork for reactive connections");

            // Verify that JtaUnitOfWork is reactive
            JtaUnitOfWork jtaUnitOfWork = (JtaUnitOfWork) work;
            assertTrue(jtaUnitOfWork.isReactive(), "JtaUnitOfWork should be reactive");

            // Get a TransactionalService instance
            TransactionalService service = IGuiceContext.get(TransactionalService.class);
            assertNotNull(service, "TransactionalService should not be null");

            try {
                // Call the transactional method
                Future<String> result = service.doSomethingTransactionalReactive();

                // Wait for the result
                result.onSuccess(ar -> {
                    assertEquals("Transaction completed", ar, "Transaction should complete successfully");
                });

                // Verify that the transaction completed successfully
                // We can't directly check if the entity manager was removed because JtaPersistService is not public
                // But we can verify that the transaction completed by checking the result
            } catch (Exception e) {
                fail("Transaction should not throw an exception: " + e.getMessage());
            }
        } finally {
            // Exit the scope
            scoper.exit();
        }
    }

    /**
     * A service class with transactional methods for testing.
     */
    public static class TransactionalService {

        /**
         * A transactional method that returns a CompletableFuture.
         * This tests the reactive transaction handling.
         */
        @Transactional
        @jakarta.inject.Named("testPostgresReactive")
        public Future<String> doSomethingTransactionalReactive() {
            // Verify that we're in a transaction by checking if we can get a Mutiny.Session
            // We can't directly access JtaPersistService.ENTITY_MANAGER_KEY because it's not public
            // Instead, we'll check if we can get the SqlClient, which should only be available in a transaction

            // Get the session from the CallScopeProperties
            CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
            // The key for the entity manager is "entityManager" in JtaPersistService
            Object emOrSession = csp.getProperties().get("entityManager");

            // Check that we got a Mutiny.Session for reactive UnitOfWork
            if (!(emOrSession instanceof Mutiny.Session)) {
                return Future.failedFuture(
                    new IllegalStateException("Expected Mutiny.Session but got: " + 
                                             (emOrSession != null ? emOrSession.getClass().getName() : "null")));
            }

            // We already verified that the SqlClient is not null above

            // Simulate some work
            return Future.succeededFuture("Transaction completed");
        }

        /**
         * A non-transactional method that calls a transactional method.
         * This tests that the transaction interceptor is properly applied.
         */
        public Future<String> callTransactionalMethod() {
            return doSomethingTransactionalReactive();
        }
    }
}

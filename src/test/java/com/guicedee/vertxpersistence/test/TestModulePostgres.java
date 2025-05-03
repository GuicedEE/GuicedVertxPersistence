package com.guicedee.vertxpersistence.test;

import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.DatabaseModule;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

import static com.guicedee.vertxpersistence.test.PostgresTest.*;

/**
 * A test module that uses PostgreSQL testcontainer.
 * This module is used to test the PostgreSQL integration with Vertx.
 */
public class TestModulePostgres extends DatabaseModule<TestModulePostgres>
{
    static {
        // Set system properties for the persistence.xml
        System.setProperty("postgres.host", POSTGRES_HOST);
        System.setProperty("postgres.port", String.valueOf(POSTGRES_PORT));
        System.setProperty("postgres.database", POSTGRES_DATABASE);
        System.setProperty("postgres.user", POSTGRES_USER);
        System.setProperty("postgres.password", POSTGRES_PASSWORD);

        // Set cache properties for testing
        System.setProperty("system.hibernate.show_sql", "true");
        System.setProperty("system.hibernate.format_sql", "true");
        System.setProperty("system.hibernate.use_sql_comments", "true");
        System.setProperty("system.hazelcast.show_sql", "true");
        System.setProperty("system.hazelcast.address", "localhost:5701");
        System.setProperty("system.hazelcast.groupname", "testgroup");
        System.setProperty("system.hazelcast.grouppass", "testpass");
        System.setProperty("system.hazelcast.instance_name", "testinstance");
    }

    @Override
    protected String getPersistenceUnitName() {
        return "testPostgres";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties) {
        PostgresConnectionBaseInfo connectionInfo = new PostgresConnectionBaseInfo();
        connectionInfo.setServerName(POSTGRES_HOST);
        connectionInfo.setPort(String.valueOf(POSTGRES_PORT));
        connectionInfo.setDatabaseName(POSTGRES_DATABASE);
        connectionInfo.setUsername(POSTGRES_USER);
        connectionInfo.setPassword(POSTGRES_PASSWORD);
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(false);
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/testPostgres";
    }
}

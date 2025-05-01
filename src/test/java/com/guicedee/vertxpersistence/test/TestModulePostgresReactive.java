package com.guicedee.vertxpersistence.test;

import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

/**
 * A test module that uses PostgreSQL testcontainer with reactive mode enabled.
 * This module is used to test the reactive PostgreSQL integration with Vertx.
 */
public class TestModulePostgresReactive extends TestModulePostgres {

    @Override
    protected String getPersistenceUnitName() {
        return "testPostgresReactive";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties) {
        PostgresConnectionBaseInfo connectionInfo = (PostgresConnectionBaseInfo) super.getConnectionBaseInfo(unit, filteredProperties);
        // Set reactive to true to test the reactive functionality
        connectionInfo.setReactive(true);
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/testPostgresReactive";
    }
}
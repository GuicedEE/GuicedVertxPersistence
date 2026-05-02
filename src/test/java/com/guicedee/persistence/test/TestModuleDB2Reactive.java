package com.guicedee.persistence.test;

import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.implementations.db2.DB2ConnectionBaseInfo;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Properties;

/**
 * A test module that uses DB2 testcontainer with reactive mode enabled.
 * This module is used to test the reactive DB2 integration with Vert.x.
 */
public class TestModuleDB2Reactive extends DatabaseModule<TestModuleDB2Reactive> {

    @Override
    protected String getPersistenceUnitName() {
        return "testDB2Reactive";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties) {
        DB2ConnectionBaseInfo connectionInfo = new DB2ConnectionBaseInfo();
        connectionInfo.setServerName(System.getProperty("db2.host", "localhost"));
        connectionInfo.setPort(System.getProperty("db2.port", "50000"));
        connectionInfo.setDatabaseName(System.getProperty("db2.database", "testdb"));
        connectionInfo.setUsername(System.getProperty("db2.user", "db2inst1"));
        connectionInfo.setPassword(System.getProperty("db2.password", "password"));
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/testDB2Reactive";
    }
}


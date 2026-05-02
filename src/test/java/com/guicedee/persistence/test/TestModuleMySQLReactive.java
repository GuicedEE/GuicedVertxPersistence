package com.guicedee.persistence.test;

import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.implementations.mysql.MySqlConnectionBaseInfo;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Properties;

/**
 * A test module that uses MySQL testcontainer with reactive mode enabled.
 * This module is used to test the reactive MySQL integration with Vert.x.
 */
public class TestModuleMySQLReactive extends DatabaseModule<TestModuleMySQLReactive> {

    @Override
    protected String getPersistenceUnitName() {
        return "testMySQLReactive";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties) {
        MySqlConnectionBaseInfo connectionInfo = new MySqlConnectionBaseInfo();
        connectionInfo.setServerName(System.getProperty("mysql.host", "localhost"));
        connectionInfo.setPort(System.getProperty("mysql.port", "3306"));
        connectionInfo.setDatabaseName(System.getProperty("mysql.database", "testdb"));
        connectionInfo.setUsername(System.getProperty("mysql.user", "test"));
        connectionInfo.setPassword(System.getProperty("mysql.password", "test"));
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/testMySQLReactive";
    }
}


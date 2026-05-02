package com.guicedee.persistence.test;

import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.implementations.oracle.OracleConnectionBaseInfo;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Properties;

/**
 * A test module that uses Oracle testcontainer with reactive mode enabled.
 * This module is used to test the reactive Oracle integration with Vert.x.
 */
public class TestModuleOracleReactive extends DatabaseModule<TestModuleOracleReactive> {

    @Override
    protected String getPersistenceUnitName() {
        return "testOracleReactive";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties) {
        OracleConnectionBaseInfo connectionInfo = new OracleConnectionBaseInfo();
        connectionInfo.setServerName(System.getProperty("oracle.host", "localhost"));
        connectionInfo.setPort(System.getProperty("oracle.port", "1521"));
        connectionInfo.setDatabaseName(System.getProperty("oracle.database", "FREEPDB1"));
        connectionInfo.setUsername(System.getProperty("oracle.user", "system"));
        connectionInfo.setPassword(System.getProperty("oracle.password", "oracle"));
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        // Use service name format for Oracle Free/XE PDB connections
        connectionInfo.getCustomProperties().put("useServiceName", "true");
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/testOracleReactive";
    }
}


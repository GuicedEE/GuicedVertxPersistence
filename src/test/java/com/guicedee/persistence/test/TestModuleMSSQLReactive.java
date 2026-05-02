package com.guicedee.persistence.test;

import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.implementations.sqlserver.SqlServerConnectionBaseInfo;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Properties;

/**
 * A test module that uses MSSQL testcontainer with reactive mode enabled.
 * This module is used to test the reactive SQL Server integration with Vert.x.
 */
public class TestModuleMSSQLReactive extends DatabaseModule<TestModuleMSSQLReactive> {

    @Override
    protected String getPersistenceUnitName() {
        return "testMSSQLReactive";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties) {
        SqlServerConnectionBaseInfo connectionInfo = new SqlServerConnectionBaseInfo();
        connectionInfo.setServerName(System.getProperty("mssql.host", "localhost"));
        connectionInfo.setPort(System.getProperty("mssql.port", "1433"));
        connectionInfo.setDatabaseName(System.getProperty("mssql.database", "master"));
        connectionInfo.setUsername(System.getProperty("mssql.user", "sa"));
        connectionInfo.setPassword(System.getProperty("mssql.password", "YourStrong@Passw0rd"));
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        connectionInfo.setTrustServerCertificate(true);
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/testMSSQLReactive";
    }
}


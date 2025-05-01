package com.guicedee.vertxpersistence.test;

import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.DatabaseModule;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

public class TestModule extends DatabaseModule<TestModule>
{

    @Override
    protected String getPersistenceUnitName() {
        return "postgres";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties) {
        PostgresConnectionBaseInfo connectionInfo = new PostgresConnectionBaseInfo();
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc/postgres";
    }
}

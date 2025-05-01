package com.guicedee.vertxpersistence.implementations.hibernateproperties;


import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.IPropertiesConnectionInfoReader;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

/**
 * Reads the default connection properties for hibernate and configures the connection accordingly
 */
public class HibernateDefaultConnectionBaseBuilder
        implements IPropertiesConnectionInfoReader<HibernateDefaultConnectionBaseBuilder>
{

    @Override
    public ConnectionBaseInfo populateConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties, ConnectionBaseInfo cbi)
    {
        if (cbi.getPersistenceUnitName() == null)
        {
            cbi.setPersistenceUnitName(unit.getName());
            cbi.setJdbcIdentifier(unit.getName());
        }
        if (cbi.getTransactionIsolation() == null)
        {
            cbi.setTransactionIsolation("READ_COMMITTED");
        }
        if (cbi.getPreparedStatementCacheSize() == null)
        {
            cbi.setPreparedStatementCacheSize(100);
        }
        if (cbi.getMaxIdleTime() == null)
        {
            cbi.setMaxIdleTime(1800000);
        }
        if (cbi.getMaxLifeTime() == null)
        {
            cbi.setMaxLifeTime(3600000);
        }
        if (cbi.getEnableJdbc4ConnectionTest() == null)
        {
            cbi.setEnableJdbc4ConnectionTest(true);
        }

        for (String prop : filteredProperties.stringPropertyNames())
        {
            switch (prop)
            {
                case "hibernate.connection.url":
                {
                    if (cbi.getUrl() == null)
                    {
                        cbi.setUrl(cbi.getJdbcUrl());
                        //cbi.setUrl(filteredProperties.getProperty(prop));
                    }
                    break;
                }
                case "hibernate.connection.user":
                {
                    if (cbi.getUsername() == null)
                    {
                        cbi.setUsername(filteredProperties.getProperty(prop));
                    }
                    break;
                }
                case "hibernate.connection.driver_class":
                {
                    if (cbi.getDriverClass() == null)
                    {
                        cbi.setDriverClass(filteredProperties.getProperty(prop));
                    }
                    break;
                }
                default:
                {
                    break;
                }
            }
        }
        return cbi;
    }

    @Override
    public Integer sortOrder()
    {
        return Integer.MIN_VALUE + 500;
    }
}

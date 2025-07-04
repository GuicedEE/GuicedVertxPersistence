package com.guicedee.vertxpersistence.implementations.hibernateproperties;


import com.google.common.base.Strings;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.IPropertiesConnectionInfoReader;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.Properties;

/**
 * Reads the default connection properties for hibernate and configures the connection accordingly
 */
public class HibernateDefaultConnectionBaseBuilder
        implements IPropertiesConnectionInfoReader<HibernateDefaultConnectionBaseBuilder>
{

    @Override
    public ConnectionBaseInfo populateConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties, ConnectionBaseInfo cbi)
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

        //process into properties
        if (!Strings.isNullOrEmpty(cbi.getUsername()))
        {
            filteredProperties.put("jakarta.persistence.jdbc.user", cbi.getUsername());
        }
        if(!Strings.isNullOrEmpty(cbi.getPassword()))
        {
            filteredProperties.put("jakarta.persistence.jdbc.password", cbi.getPassword());
        }
        return cbi;
    }

    @Override
    public Integer sortOrder()
    {
        return Integer.MIN_VALUE + 500;
    }
}

package com.guicedee.persistence.implementations.hibernateproperties;


import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.IPropertiesConnectionInfoReader;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.Properties;

/**
 * Reads the default connection properties for hibernate and configures the connection accordingly
 */
public class HibernateDefaultConnectionBaseBuilder
        implements IPropertiesConnectionInfoReader<HibernateDefaultConnectionBaseBuilder>
{

    /**
     * Applies default Hibernate connection values and maps standard properties.
     *
     * @param unit the persistence unit descriptor
     * @param filteredProperties the properties scoped to the unit
     * @param cbi the connection info to populate
     * @return the populated connection info
     */
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

        // Placeholders have already been resolved by SystemEnvironmentVariablesPropertiesReader.
        // Copy those values into the connection settings without replacing application overrides.
        if (cbi.getUrl() == null)
            cbi.setUrl(first(filteredProperties, "jakarta.persistence.jdbc.url", "javax.persistence.jdbc.url", "hibernate.connection.url"));
        if (cbi.getUsername() == null)
            cbi.setUsername(first(filteredProperties, "jakarta.persistence.jdbc.user", "javax.persistence.jdbc.user", "hibernate.connection.username", "hibernate.connection.user"));
        if (cbi.getPassword() == null)
            cbi.setPassword(first(filteredProperties, "jakarta.persistence.jdbc.password", "javax.persistence.jdbc.password", "hibernate.connection.password"));
        if (cbi.getDriverClass() == null)
            cbi.setDriverClass(first(filteredProperties, "jakarta.persistence.jdbc.driver", "javax.persistence.jdbc.driver", "hibernate.connection.driver_class"));

        //process into properties
        if (cbi.getUsername() != null)
        {
            filteredProperties.put("jakarta.persistence.jdbc.user", cbi.getUsername());
        }
        if(cbi.getPassword() != null)
        {
            filteredProperties.put("jakarta.persistence.jdbc.password", cbi.getPassword());
        }
        return cbi;
    }

    private static String first(Properties properties, String... names) {
        for (String name : names) {
            String value = properties.getProperty(name);
            if (value != null) return value;
        }
        return null;
    }

    @Override
    public Integer sortOrder()
    {
        return Integer.MIN_VALUE + 500;
    }
}

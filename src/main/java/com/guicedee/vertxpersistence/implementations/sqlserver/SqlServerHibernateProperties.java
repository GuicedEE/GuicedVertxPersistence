package com.guicedee.vertxpersistence.implementations.sqlserver;

import com.guicedee.vertxpersistence.IPropertiesEntityManagerReader;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides SQL Server-specific Hibernate properties.
 * This class sets the appropriate dialect and other SQL Server-specific properties
 * for Hibernate when using SQL Server.
 */
public class SqlServerHibernateProperties implements IPropertiesEntityManagerReader<SqlServerHibernateProperties> {

    /**
     * Process the properties for SQL Server.
     * This method adds SQL Server-specific Hibernate properties to the incoming properties.
     *
     * @param persistenceUnit    The persistence unit descriptor
     * @param incomingProperties The incoming properties to process
     * @return A map of additional properties to add
     */
    @Override
    public Map<String, String> processProperties(PersistenceUnitDescriptor persistenceUnit, Properties incomingProperties) {
        Map<String, String> props = new HashMap<>();

        // Set SQL Server dialect if not already set
        if (!incomingProperties.containsKey("hibernate.dialect")) {
            incomingProperties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        }

        // Set other SQL Server-specific properties
        // These are common settings that work well with SQL Server
        
        // Use JDBC batch updates for better performance
        if (!incomingProperties.containsKey("hibernate.jdbc.batch_size")) {
            incomingProperties.put("hibernate.jdbc.batch_size", "50");
        }
        
        // Order inserts by primary key for better performance
        if (!incomingProperties.containsKey("hibernate.order_inserts")) {
            incomingProperties.put("hibernate.order_inserts", "true");
        }
        
        // Order updates by primary key for better performance
        if (!incomingProperties.containsKey("hibernate.order_updates")) {
            incomingProperties.put("hibernate.order_updates", "true");
        }
        
        // Use SQL Server-specific batch updates
        if (!incomingProperties.containsKey("hibernate.jdbc.batch_versioned_data")) {
            incomingProperties.put("hibernate.jdbc.batch_versioned_data", "true");
        }
        
        // Use SQL Server's RETURNING syntax for generated keys
        if (!incomingProperties.containsKey("hibernate.jdbc.use_get_generated_keys")) {
            incomingProperties.put("hibernate.jdbc.use_get_generated_keys", "true");
        }
        
        // Set the schema generation tool for SQL Server
        if (!incomingProperties.containsKey("hibernate.hbm2ddl.auto") && 
            !incomingProperties.containsKey("jakarta.persistence.schema-generation.database.action")) {
            incomingProperties.put("hibernate.hbm2ddl.auto", "update");
        }
        
        // SQL Server-specific settings
        
        // Use ANSI_NULLS and ANSI_WARNINGS
        if (!incomingProperties.containsKey("hibernate.connection.sendStringParametersAsUnicode")) {
            incomingProperties.put("hibernate.connection.sendStringParametersAsUnicode", "true");
        }
        
        // Set the default schema if not already set
        if (!incomingProperties.containsKey("hibernate.default_schema") && 
            incomingProperties.containsKey("hibernate.connection.username")) {
            incomingProperties.put("hibernate.default_schema", "dbo");
        }
        
        // Use SQL Server's native sequence for ID generation
        if (!incomingProperties.containsKey("hibernate.id.new_generator_mappings")) {
            incomingProperties.put("hibernate.id.new_generator_mappings", "false");
        }

        return props;
    }

    /**
     * Determines if this properties reader is applicable to the given persistence unit.
     * This method checks if the persistence unit is using SQL Server by looking at the driver class
     * or connection URL.
     *
     * @param persistenceUnit The persistence unit descriptor
     * @return true if this properties reader is applicable, false otherwise
     */
    @Override
    public boolean applicable(PersistenceUnitDescriptor persistenceUnit) {
        // Check if the persistence unit is using SQL Server
        Properties props = persistenceUnit.getProperties();
        
        // Check the driver class
        String driverClass = props.getProperty("hibernate.connection.driver_class");
        if (driverClass != null && (driverClass.contains("sqlserver") || driverClass.contains("mssql"))) {
            return true;
        }
        
        // Check the connection URL
        String url = props.getProperty("hibernate.connection.url");
        if (url != null && (url.contains("sqlserver") || url.contains("mssql"))) {
            return true;
        }
        
        // Check the dialect
        String dialect = props.getProperty("hibernate.dialect");
        if (dialect != null && (dialect.contains("SQLServer") || dialect.contains("MSSQLServer"))) {
            return true;
        }
        
        // Check if we're using a SQL Server connection
        String persistenceUnitName = persistenceUnit.getName();
        return persistenceUnitName != null && 
               (persistenceUnitName.toLowerCase().contains("sqlserver") || 
                persistenceUnitName.toLowerCase().contains("mssql"));
    }

    /**
     * Returns the sort order for this properties reader.
     * This ensures that database-specific properties are applied after general properties.
     *
     * @return The sort order
     */
    @Override
    public Integer sortOrder() {
        return 200; // Higher than HibernateEntityManagerProperties (100)
    }
}
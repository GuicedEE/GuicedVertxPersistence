package com.guicedee.vertxpersistence.implementations.db2;

import com.guicedee.vertxpersistence.IPropertiesEntityManagerReader;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides DB2-specific Hibernate properties.
 * This class sets the appropriate dialect and other DB2-specific properties
 * for Hibernate when using DB2.
 */
public class DB2HibernateProperties implements IPropertiesEntityManagerReader<DB2HibernateProperties> {

    /**
     * Process the properties for DB2.
     * This method adds DB2-specific Hibernate properties to the incoming properties.
     *
     * @param persistenceUnit    The persistence unit descriptor
     * @param incomingProperties The incoming properties to process
     * @return A map of additional properties to add
     */
    @Override
    public Map<String, String> processProperties(ParsedPersistenceXmlDescriptor persistenceUnit, Properties incomingProperties) {
        Map<String, String> props = new HashMap<>();

        // Set DB2 dialect if not already set
        if (!incomingProperties.containsKey("hibernate.dialect")) {
            incomingProperties.put("hibernate.dialect", "org.hibernate.dialect.DB2Dialect");
        }

        // Set other DB2-specific properties
        // These are common settings that work well with DB2
        
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
        
        // Use DB2-specific batch updates
        if (!incomingProperties.containsKey("hibernate.jdbc.batch_versioned_data")) {
            incomingProperties.put("hibernate.jdbc.batch_versioned_data", "true");
        }
        
        // Use DB2's RETURNING syntax for generated keys
        if (!incomingProperties.containsKey("hibernate.jdbc.use_get_generated_keys")) {
            incomingProperties.put("hibernate.jdbc.use_get_generated_keys", "true");
        }
        
        // Set the schema generation tool for DB2
        if (!incomingProperties.containsKey("hibernate.hbm2ddl.auto") && 
            !incomingProperties.containsKey("jakarta.persistence.schema-generation.database.action")) {
            incomingProperties.put("hibernate.hbm2ddl.auto", "update");
        }
        
        // DB2-specific settings
        
        // Set the default schema if not already set
        if (!incomingProperties.containsKey("hibernate.default_schema") && 
            incomingProperties.containsKey("hibernate.connection.username")) {
            incomingProperties.put("hibernate.default_schema", 
                                  incomingProperties.getProperty("hibernate.connection.username").toUpperCase());
        }
        
        // Use DB2's native sequence for ID generation
        if (!incomingProperties.containsKey("hibernate.id.new_generator_mappings")) {
            incomingProperties.put("hibernate.id.new_generator_mappings", "false");
        }
        
        // Set DB2-specific query substitutions
        if (!incomingProperties.containsKey("hibernate.query.substitutions")) {
            incomingProperties.put("hibernate.query.substitutions", "true=1,false=0");
        }

        return props;
    }

    /**
     * Determines if this properties reader is applicable to the given persistence unit.
     * This method checks if the persistence unit is using DB2 by looking at the driver class
     * or connection URL.
     *
     * @param persistenceUnit The persistence unit descriptor
     * @return true if this properties reader is applicable, false otherwise
     */
    @Override
    public boolean applicable(ParsedPersistenceXmlDescriptor persistenceUnit) {
        // Check if the persistence unit is using DB2
        Properties props = persistenceUnit.getProperties();
        
        // Check the driver class
        String driverClass = props.getProperty("hibernate.connection.driver_class");
        if (driverClass != null && driverClass.contains("db2")) {
            return true;
        }
        
        // Check the connection URL
        String url = props.getProperty("hibernate.connection.url");
        if (url != null && url.contains("db2")) {
            return true;
        }
        
        // Check the dialect
        String dialect = props.getProperty("hibernate.dialect");
        if (dialect != null && dialect.contains("DB2")) {
            return true;
        }
        
        // Check if we're using a DB2 connection
        String persistenceUnitName = persistenceUnit.getName();
        return persistenceUnitName != null && persistenceUnitName.toLowerCase().contains("db2");
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
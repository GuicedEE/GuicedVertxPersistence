package com.guicedee.vertxpersistence.implementations.mysql;

import com.guicedee.vertxpersistence.IPropertiesEntityManagerReader;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides MySQL-specific Hibernate properties.
 * This class sets the appropriate dialect and other MySQL-specific properties
 * for Hibernate when using MySQL.
 */
public class MySqlHibernateProperties implements IPropertiesEntityManagerReader<MySqlHibernateProperties> {

    /**
     * Process the properties for MySQL.
     * This method adds MySQL-specific Hibernate properties to the incoming properties.
     *
     * @param persistenceUnit    The persistence unit descriptor
     * @param incomingProperties The incoming properties to process
     * @return A map of additional properties to add
     */
    @Override
    public Map<String, String> processProperties(PersistenceUnitDescriptor persistenceUnit, Properties incomingProperties) {
        Map<String, String> props = new HashMap<>();

        // Set MySQL dialect if not already set
        if (!incomingProperties.containsKey("hibernate.dialect")) {
            incomingProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        }

        // Set other MySQL-specific properties
        // These are common settings that work well with MySQL
        
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
        
        // Use MySQL-specific batch updates
        if (!incomingProperties.containsKey("hibernate.jdbc.batch_versioned_data")) {
            incomingProperties.put("hibernate.jdbc.batch_versioned_data", "true");
        }
        
        // Use MySQL's RETURNING syntax for generated keys
        if (!incomingProperties.containsKey("hibernate.jdbc.use_get_generated_keys")) {
            incomingProperties.put("hibernate.jdbc.use_get_generated_keys", "true");
        }
        
        // Set the schema generation tool for MySQL
        if (!incomingProperties.containsKey("hibernate.hbm2ddl.auto") && 
            !incomingProperties.containsKey("jakarta.persistence.schema-generation.database.action")) {
            incomingProperties.put("hibernate.hbm2ddl.auto", "update");
        }
        
        // MySQL-specific settings
        
        // Use InnoDB engine by default
        if (!incomingProperties.containsKey("hibernate.dialect.storage_engine")) {
            incomingProperties.put("hibernate.dialect.storage_engine", "innodb");
        }
        
        // Use UTF-8 character set
        if (!incomingProperties.containsKey("hibernate.connection.characterEncoding")) {
            incomingProperties.put("hibernate.connection.characterEncoding", "utf8");
        }
        
        // Use UTF-8 character set for results
        if (!incomingProperties.containsKey("hibernate.connection.useUnicode")) {
            incomingProperties.put("hibernate.connection.useUnicode", "true");
        }

        return props;
    }

    /**
     * Determines if this properties reader is applicable to the given persistence unit.
     * This method checks if the persistence unit is using MySQL by looking at the driver class
     * or connection URL.
     *
     * @param persistenceUnit The persistence unit descriptor
     * @return true if this properties reader is applicable, false otherwise
     */
    @Override
    public boolean applicable(PersistenceUnitDescriptor persistenceUnit) {
        // Check if the persistence unit is using MySQL
        Properties props = persistenceUnit.getProperties();
        
        // Check the driver class
        String driverClass = props.getProperty("hibernate.connection.driver_class");
        if (driverClass != null && (driverClass.contains("mysql") || driverClass.contains("mariadb"))) {
            return true;
        }
        
        // Check the connection URL
        String url = props.getProperty("hibernate.connection.url");
        if (url != null && (url.contains("mysql") || url.contains("mariadb"))) {
            return true;
        }
        
        // Check the dialect
        String dialect = props.getProperty("hibernate.dialect");
        if (dialect != null && (dialect.contains("MySQL") || dialect.contains("MariaDB"))) {
            return true;
        }
        
        // Check if we're using a MySQL connection
        String persistenceUnitName = persistenceUnit.getName();
        return persistenceUnitName != null && 
               (persistenceUnitName.toLowerCase().contains("mysql") || 
                persistenceUnitName.toLowerCase().contains("mariadb"));
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
package com.guicedee.vertxpersistence.implementations.vertxsql;

import com.guicedee.vertxpersistence.CleanVertxConnectionBaseInfo;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.ConnectionBaseInfoFactory;
import com.guicedee.vertxpersistence.IPropertiesConnectionInfoReader;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

/**
 * Reads Vertx SQL client properties from persistence.xml and configures the connection accordingly.
 * This implementation supports both standard JPA properties and Vertx-specific properties.
 */
public class VertxSqlClientConnectionBaseBuilder
        implements IPropertiesConnectionInfoReader<VertxSqlClientConnectionBaseBuilder>
{
    @Override
    public ConnectionBaseInfo populateConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties, ConnectionBaseInfo cbi)
    {
        // First populate the original ConnectionBaseInfo
        for (String prop : filteredProperties.stringPropertyNames())
        {
            String value = filteredProperties.getProperty(prop);

            // Handle standard JPA properties
            switch (prop)
            {
                // Basic connection properties
                case "jakarta.persistence.jdbc.url":
                case "javax.persistence.jdbc.url":
                case "hibernate.connection.url":
                {
                    cbi.setUrl(value);

                    // Try to extract host, port, and database from URL if not already set
                    if (cbi.getServerName() == null || cbi.getPort() == null || cbi.getDatabaseName() == null) {
                        parseJdbcUrl(value, cbi);
                    }
                    break;
                }
                case "jakarta.persistence.jdbc.user":
                case "javax.persistence.jdbc.user":
                case "hibernate.connection.user":
                {
                    cbi.setUsername(value);
                    break;
                }
                case "jakarta.persistence.jdbc.password":
                case "javax.persistence.jdbc.password":
                case "hibernate.connection.password":
                {
                    cbi.setPassword(value);
                    break;
                }
                case "jakarta.persistence.jdbc.driver":
                case "javax.persistence.jdbc.driver":
                case "hibernate.connection.driver_class":
                {
                    cbi.setDriverClass(value);
                    break;
                }

                // Vertx-specific properties
                case "vertx.sql.host":
                case "hibernate.vertx.host":
                {
                    cbi.setServerName(value);
                    break;
                }
                case "vertx.sql.port":
                case "hibernate.vertx.port":
                {
                    cbi.setPort(value);
                    break;
                }
                case "vertx.sql.database":
                case "hibernate.vertx.database":
                {
                    cbi.setDatabaseName(value);
                    break;
                }

                // Connection pool properties
                case "vertx.sql.pool.max-size":
                case "hibernate.vertx.pool.max-size":
                case "hibernate.hikari.maximumPoolSize":
                {
                    try {
                        cbi.setMaxPoolSize(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Ignore if not a valid number
                    }
                    break;
                }
                case "vertx.sql.pool.min-size":
                case "hibernate.vertx.pool.min-size":
                case "hibernate.hikari.minimumIdle":
                {
                    try {
                        cbi.setMinPoolSize(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Ignore if not a valid number
                    }
                    break;
                }
                case "vertx.sql.pool.idle-timeout":
                case "hibernate.vertx.pool.idle-timeout":
                case "hibernate.hikari.idleTimeout":
                {
                    try {
                        // Convert from milliseconds to seconds if needed
                        int timeout = Integer.parseInt(value);
                        if (timeout > 1000) { // Assume it's in milliseconds if > 1000
                            timeout = timeout / 1000;
                        }
                        cbi.setMaxIdleTime(timeout);
                    } catch (NumberFormatException e) {
                        // Ignore if not a valid number
                    }
                    break;
                }
                case "vertx.sql.pool.max-lifetime":
                case "hibernate.vertx.pool.max-lifetime":
                case "hibernate.hikari.maxLifetime":
                {
                    try {
                        // Convert from milliseconds to seconds if needed
                        int lifetime = Integer.parseInt(value);
                        if (lifetime > 1000) { // Assume it's in milliseconds if > 1000
                            lifetime = lifetime / 1000;
                        }
                        cbi.setMaxLifeTime(lifetime);
                    } catch (NumberFormatException e) {
                        // Ignore if not a valid number
                    }
                    break;
                }
                case "vertx.sql.pool.connection-timeout":
                case "hibernate.vertx.pool.connection-timeout":
                case "hibernate.hikari.connectionTimeout":
                {
                    try {
                        // Convert from milliseconds to seconds if needed
                        int timeout = Integer.parseInt(value);
                        if (timeout > 1000) { // Assume it's in milliseconds if > 1000
                            timeout = timeout / 1000;
                        }
                        cbi.setAcquisitionTimeout(timeout);
                    } catch (NumberFormatException e) {
                        // Ignore if not a valid number
                    }
                    break;
                }

                // Transaction properties
                case "vertx.sql.transaction-isolation":
                case "hibernate.vertx.transaction-isolation":
                case "hibernate.connection.isolation":
                {
                    cbi.setTransactionIsolation(value);
                    break;
                }

                // Connection testing
                case "vertx.sql.test-query":
                case "hibernate.vertx.test-query":
                case "hibernate.hikari.connectionTestQuery":
                {
                    cbi.setTestQuery(value);
                    break;
                }

                // Add any other properties to customProperties
                default:
                {
                    // Add Vertx-specific properties to customProperties
                    if (prop.startsWith("vertx.sql.") || prop.startsWith("hibernate.vertx.")) {
                        String key = prop.startsWith("vertx.sql.") ? 
                                    prop.substring("vertx.sql.".length()) : 
                                    prop.substring("hibernate.vertx.".length());
                        cbi.getCustomProperties().put(key, value);
                    }
                    break;
                }
            }
        }

        // Determine the database type from the driver class or URL
        String databaseType = determineDatabaseType(cbi);

        // Create the appropriate ConnectionBaseInfo implementation based on the database type
        ConnectionBaseInfo specificCbi = ConnectionBaseInfoFactory.createConnectionBaseInfo(databaseType);

        // Copy all properties from the original ConnectionBaseInfo
        copyProperties(cbi, specificCbi);

        // Return the database-specific ConnectionBaseInfo
        return specificCbi;
    }

    /**
     * Determines the database type from the driver class or URL.
     *
     * @param cbi The ConnectionBaseInfo containing driver and URL information
     * @return The database type (e.g., "postgresql", "mysql", "oracle", "sqlserver", "db2")
     */
    private String determineDatabaseType(ConnectionBaseInfo cbi) {
        // First check if the driver is set
        if (cbi.getDriver() != null) {
            return cbi.getDriver();
        }

        // Then check the driver class
        if (cbi.getDriverClass() != null) {
            String driverClass = cbi.getDriverClass().toLowerCase();
            if (driverClass.contains("postgresql")) {
                return "postgresql";
            } else if (driverClass.contains("mysql")) {
                return "mysql";
            } else if (driverClass.contains("mariadb")) {
                return "mysql";
            } else if (driverClass.contains("oracle")) {
                return "oracle";
            } else if (driverClass.contains("sqlserver") || driverClass.contains("mssql")) {
                return "sqlserver";
            } else if (driverClass.contains("db2")) {
                return "db2";
            }
        }

        // Finally check the URL
        if (cbi.getUrl() != null) {
            return ConnectionBaseInfoFactory.createConnectionBaseInfoFromJdbcUrl(cbi.getUrl()).getDriver();
        }

        // If we can't determine the database type, return null
        return null;
    }

    /**
     * Creates a new CleanVertxConnectionBaseInfo with the same properties as the given ConnectionBaseInfo.
     * This method can be used to get a clean version of any ConnectionBaseInfo.
     *
     * @param cbi The original ConnectionBaseInfo
     * @return A new CleanVertxConnectionBaseInfo with the same properties
     */
    public CleanVertxConnectionBaseInfo toCleanVertxConnectionBaseInfo(ConnectionBaseInfo cbi) {
        CleanVertxConnectionBaseInfo cleanCbi = new CleanVertxConnectionBaseInfo();
        copyProperties(cbi, cleanCbi);
        return cleanCbi;
    }

    /**
     * Copies all properties from one ConnectionBaseInfo to another.
     *
     * @param source The source ConnectionBaseInfo
     * @param target The target ConnectionBaseInfo
     */
    private void copyProperties(ConnectionBaseInfo source, ConnectionBaseInfo target) {
        // Copy all properties
        target.setPersistenceUnitName(source.getPersistenceUnitName());
        target.setReactive(source.isReactive());
        target.setXa(source.isXa());
        target.setUrl(source.getUrl());
        target.setServerName(source.getServerName());
        target.setPort(source.getPort());
        target.setInstanceName(source.getInstanceName());
        target.setDriver(source.getDriver());
        target.setDriverClass(source.getDriverClass());
        target.setClassName(source.getClassName());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setTransactionIsolation(source.getTransactionIsolation());
        target.setDatabaseName(source.getDatabaseName());
        target.setJndiName(source.getJndiName());
        target.setJdbcIdentifier(source.getJdbcIdentifier());

        // Copy numeric properties
        if (source.getMinPoolSize() != null) {
            target.setMinPoolSize(source.getMinPoolSize());
        }
        if (source.getMaxPoolSize() != null) {
            target.setMaxPoolSize(source.getMaxPoolSize());
        }
        if (source.getMaxIdleTime() != null) {
            target.setMaxIdleTime(source.getMaxIdleTime());
        }
        if (source.getMaxLifeTime() != null) {
            target.setMaxLifeTime(source.getMaxLifeTime());
        }
        if (source.getPreparedStatementCacheSize() != null) {
            target.setPreparedStatementCacheSize(source.getPreparedStatementCacheSize());
        }
        if (source.getAcquireIncrement() != null) {
            target.setAcquireIncrement(source.getAcquireIncrement());
        }
        if (source.getAcquisitionInterval() != null) {
            target.setAcquisitionInterval(source.getAcquisitionInterval());
        }
        if (source.getAcquisitionTimeout() != null) {
            target.setAcquisitionTimeout(source.getAcquisitionTimeout());
        }

        // Copy boolean properties
        if (source.getPrefill() != null) {
            target.setPrefill(source.getPrefill());
        }
        if (source.getUseStrictMin() != null) {
            target.setUseStrictMin(source.getUseStrictMin());
        }
        if (source.getAllowLocalTransactions() != null) {
            target.setAllowLocalTransactions(source.getAllowLocalTransactions());
        }
        if (source.getApplyTransactionTimeout() != null) {
            target.setApplyTransactionTimeout(source.getApplyTransactionTimeout());
        }
        if (source.getAutomaticEnlistingEnabled() != null) {
            target.setAutomaticEnlistingEnabled(source.getAutomaticEnlistingEnabled());
        }
        if (source.getEnableJdbc4ConnectionTest() != null) {
            target.setEnableJdbc4ConnectionTest(source.getEnableJdbc4ConnectionTest());
        }
        if (source.getIgnoreRecoveryFailures() != null) {
            target.setIgnoreRecoveryFailures(source.getIgnoreRecoveryFailures());
        }
        if (source.getShareTransactionConnections() != null) {
            target.setShareTransactionConnections(source.getShareTransactionConnections());
        }

        // Copy test query
        target.setTestQuery(source.getTestQuery());

        // Copy server instance name property
        target.setServerInstanceNameProperty(source.getServerInstanceNameProperty());

        // Copy default connection flag
        target.setDefaultConnection(source.isDefaultConnection());

        // Copy custom properties
        target.getCustomProperties().putAll(source.getCustomProperties());
    }

    /**
     * Attempts to parse a JDBC URL to extract host, port, and database name.
     * Supports common JDBC URL formats like:
     * - jdbc:postgresql://localhost:5432/mydatabase
     * - jdbc:mysql://localhost:3306/mydatabase
     * - jdbc:sqlserver://localhost:1433;databaseName=mydatabase
     * 
     * @param jdbcUrl The JDBC URL to parse
     * @param cbi The ConnectionBaseInfo to populate
     */
    private void parseJdbcUrl(String jdbcUrl, ConnectionBaseInfo cbi) {
        try {
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                return;
            }

            // Remove jdbc: prefix
            String url = jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl;

            // Extract database type
            int colonIndex = url.indexOf(':');
            if (colonIndex > 0) {
                String dbType = url.substring(0, colonIndex);
                cbi.setDriver(dbType);
                url = url.substring(colonIndex + 1);
            }

            // Remove // if present
            if (url.startsWith("//")) {
                url = url.substring(2);
            }

            // Extract host and port
            int slashIndex = url.indexOf('/');
            int semicolonIndex = url.indexOf(';');
            int endIndex = (slashIndex > 0 && (semicolonIndex < 0 || slashIndex < semicolonIndex)) ? 
                          slashIndex : semicolonIndex;

            if (endIndex < 0) {
                endIndex = url.length();
            }

            String hostPort = url.substring(0, endIndex);
            int portIndex = hostPort.indexOf(':');

            if (portIndex > 0) {
                String host = hostPort.substring(0, portIndex);
                String port = hostPort.substring(portIndex + 1);

                if (cbi.getServerName() == null) {
                    cbi.setServerName(host);
                }

                if (cbi.getPort() == null) {
                    cbi.setPort(port);
                }
            } else if (cbi.getServerName() == null) {
                cbi.setServerName(hostPort);
            }

            // Extract database name
            if (slashIndex > 0 && slashIndex < url.length() - 1) {
                String dbPart = url.substring(slashIndex + 1);
                int paramIndex = dbPart.indexOf('?');
                if (paramIndex > 0) {
                    dbPart = dbPart.substring(0, paramIndex);
                }

                if (cbi.getDatabaseName() == null && !dbPart.isEmpty()) {
                    cbi.setDatabaseName(dbPart);
                }
            } else if (semicolonIndex > 0) {
                // Handle SQL Server style URLs
                String params = url.substring(semicolonIndex + 1);
                String[] paramPairs = params.split(";");

                for (String pair : paramPairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();

                        if (key.equalsIgnoreCase("databaseName") && cbi.getDatabaseName() == null) {
                            cbi.setDatabaseName(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors, just use the URL as is
        }
    }

    @Override
    public Integer sortOrder()
    {
        return Integer.MAX_VALUE - 500;
    }
}

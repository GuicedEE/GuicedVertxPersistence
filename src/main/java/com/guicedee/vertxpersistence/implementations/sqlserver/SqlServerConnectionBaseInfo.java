package com.guicedee.vertxpersistence.implementations.sqlserver;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.CleanVertxConnectionBaseInfo;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.java.Log;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

/**
 * A specialized ConnectionBaseInfo implementation for Microsoft SQL Server.
 * This class provides SQL Server-specific configuration options for Vertx SQL client.
 */
@Log
public class SqlServerConnectionBaseInfo extends CleanVertxConnectionBaseInfo {

    /**
     * Creates a new SqlServerConnectionBaseInfo instance.
     */
    public SqlServerConnectionBaseInfo() {
        super();
        setDriver("sqlserver");
    }

    /**
     * Creates a new SqlServerConnectionBaseInfo instance with the specified XA mode.
     *
     * @param xa Whether this is an XA connection
     */
    public SqlServerConnectionBaseInfo(boolean xa) {
        super(xa);
        setDriver("sqlserver");
    }

    /**
     * Returns a Vertx SqlClient configured for SQL Server.
     *
     * @return A SqlClient instance
     */
    @Override
    public SqlClient toPooledDatasource() {
        try {
            // Get the Vertx instance from the Guice context
            Vertx vertx = VertXPreStartup.getVertx();

            // Use reflection to create MSSQLConnectOptions and PoolOptions
            Class<?> mssqlConnectOptionsClass;
            Object connectOptions;
            try {
                mssqlConnectOptionsClass = Class.forName("io.vertx.mssqlclient.MSSQLConnectOptions");
                connectOptions = mssqlConnectOptionsClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error creating MSSQLConnectOptions", e);
                return null;
            }

            // Set basic connection properties
            if (getServerName() != null) {
                try {
                    Method setHostMethod = mssqlConnectOptionsClass.getMethod("setHost", String.class);
                    setHostMethod.invoke(connectOptions, getServerName());
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting host for SQL Server connection", e);
                }
            }

            if (getPort() != null) {
                try {
                    Method setPortMethod = mssqlConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, Integer.parseInt(getPort()));
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting port for SQL Server connection", e);
                }
            } else {
                // Default SQL Server port
                try {
                    Method setPortMethod = mssqlConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, 1433);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting default port for SQL Server connection", e);
                }
            }

            if (getDatabaseName() != null) {
                try {
                    Method setDatabaseMethod = mssqlConnectOptionsClass.getMethod("setDatabase", String.class);
                    setDatabaseMethod.invoke(connectOptions, getDatabaseName());
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting database for SQL Server connection", e);
                }
            }

            if (getUsername() != null) {
                try {
                    Method setUserMethod = mssqlConnectOptionsClass.getMethod("setUser", String.class);
                    setUserMethod.invoke(connectOptions, getUsername());
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting user for SQL Server connection", e);
                }
            }

            if (getPassword() != null) {
                try {
                    Method setPasswordMethod = mssqlConnectOptionsClass.getMethod("setPassword", String.class);
                    setPasswordMethod.invoke(connectOptions, getPassword());
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting password for SQL Server connection", e);
                }
            }

            // Set connection timeout
            if (getAcquisitionTimeout() != null) {
                try {
                    Method setConnectTimeoutMethod = mssqlConnectOptionsClass.getMethod("setConnectTimeout", int.class);
                    // Convert seconds to milliseconds
                    setConnectTimeoutMethod.invoke(connectOptions, getAcquisitionTimeout() * 1000);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting connect timeout for SQL Server connection", e);
                }
            }

            // Set idle timeout
            if (getMaxIdleTime() != null) {
                try {
                    Method setIdleTimeoutMethod = mssqlConnectOptionsClass.getMethod("setIdleTimeout", int.class);
                    // Convert seconds to milliseconds
                    setIdleTimeoutMethod.invoke(connectOptions, getMaxIdleTime() * 1000);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting idle timeout for SQL Server connection", e);
                }
            }

            // Set SQL Server-specific properties
            // Set instance name if specified
            if (getInstanceName() != null) {
                try {
                    Method setInstanceMethod = mssqlConnectOptionsClass.getMethod("setInstance", String.class);
                    setInstanceMethod.invoke(connectOptions, getInstanceName());
                } catch (NoSuchMethodException e) {
                    log.fine("setInstance method not found in MSSQLConnectOptions, skipping");
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting instance name for SQL Server connection", e);
                }
            }

            // Set packet size if specified in custom properties
            if (getCustomProperties().containsKey("packetSize")) {
                try {
                    Method setPacketSizeMethod = mssqlConnectOptionsClass.getMethod("setPacketSize", int.class);
                    setPacketSizeMethod.invoke(connectOptions, Integer.parseInt(getCustomProperties().get("packetSize")));
                } catch (NoSuchMethodException e) {
                    log.fine("setPacketSize method not found in MSSQLConnectOptions, skipping");
                } catch (NumberFormatException e) {
                    log.fine("Invalid packet size value: " + getCustomProperties().get("packetSize"));
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting packet size for SQL Server connection", e);
                }
            }

            // Set SSL if specified in custom properties
            if (getCustomProperties().containsKey("ssl")) {
                try {
                    Method setSslMethod = mssqlConnectOptionsClass.getMethod("setSsl", boolean.class);
                    setSslMethod.invoke(connectOptions, Boolean.parseBoolean(getCustomProperties().get("ssl")));
                } catch (NoSuchMethodException e) {
                    log.fine("setSsl method not found in MSSQLConnectOptions, skipping");
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting SSL for SQL Server connection", e);
                }
            }

            // Set any other custom properties
            for (Map.Entry<String, String> entry : getCustomProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip properties that are not related to SQL connection or already handled
                if (key.startsWith("hibernate.") || key.startsWith("jakarta.") || key.startsWith("javax.") || 
                    key.equals("packetSize") || key.equals("ssl")) {
                    continue;
                }

                try {
                    // Try to find a setter method for this property
                    String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                    Method method = mssqlConnectOptionsClass.getMethod(methodName, String.class);
                    method.invoke(connectOptions, value);
                } catch (NoSuchMethodException e) {
                    // Property might not have a direct setter, try to use a generic property setter if available
                    try {
                        Method setPropertyMethod = mssqlConnectOptionsClass.getMethod("setProperty", String.class, String.class);
                        setPropertyMethod.invoke(connectOptions, key, value);
                    } catch (NoSuchMethodException ex) {
                        // Ignore if no generic property setter is available
                        log.fine("No setter found for property: " + key);
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, "Error setting property " + key + " for SQL Server connection", ex);
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting property " + key + " for SQL Server connection", e);
                }
            }

            // Configure pool options
            Class<?> poolOptionsClass;
            Object poolOptions;
            try {
                poolOptionsClass = Class.forName("io.vertx.sqlclient.PoolOptions");
                poolOptions = poolOptionsClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error creating PoolOptions", e);
                return null;
            }

            // Set pool size limits
            if (getMaxPoolSize() != null) {
                try {
                    Method setMaxSizeMethod = poolOptionsClass.getMethod("setMaxSize", int.class);
                    setMaxSizeMethod.invoke(poolOptions, getMaxPoolSize());
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting max pool size", e);
                }
            }

            if (getMinPoolSize() != null) {
                try {
                    Method setMinSizeMethod = poolOptionsClass.getMethod("setMinSize", int.class);
                    setMinSizeMethod.invoke(poolOptions, getMinPoolSize());
                } catch (NoSuchMethodException e) {
                    // Min size might not be supported in all versions, ignore if method not found
                    log.fine("Min size method not found in PoolOptions, skipping");
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting min pool size", e);
                }
            }

            // Set max wait queue size if acquire increment is specified
            if (getAcquireIncrement() != null) {
                try {
                    Method setMaxWaitQueueSizeMethod = poolOptionsClass.getMethod("setMaxWaitQueueSize", int.class);
                    setMaxWaitQueueSizeMethod.invoke(poolOptions, getAcquireIncrement());
                } catch (NoSuchMethodException e) {
                    // Max wait queue size might not be supported in all versions, ignore if method not found
                    log.fine("Max wait queue size method not found in PoolOptions, skipping");
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting max wait queue size", e);
                }
            }

            // Set connection lifetime
            if (getMaxLifeTime() != null) {
                try {
                    Method setMaxLifetimeMethod = poolOptionsClass.getMethod("setMaxLifetime", int.class);
                    // Convert seconds to milliseconds
                    setMaxLifetimeMethod.invoke(poolOptions, getMaxLifeTime() * 1000);
                } catch (NoSuchMethodException e) {
                    // Max lifetime might not be supported in all versions, ignore if method not found
                    log.fine("Max lifetime method not found in PoolOptions, skipping");
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error setting max lifetime", e);
                }
            }

            // Create the pool using MSSQLPool
            try {
                Class<?> mssqlPoolClass = Class.forName("io.vertx.mssqlclient.MSSQLPool");
                Method poolMethod = mssqlPoolClass.getMethod("pool", Vertx.class, mssqlConnectOptionsClass, poolOptionsClass);
                Object client = poolMethod.invoke(null, vertx, connectOptions, poolOptions);
                return (SqlClient) client;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error creating SQL Server pool", e);
                return null;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error creating SQL Server SqlClient", e);
            return null;
        }
    }
}

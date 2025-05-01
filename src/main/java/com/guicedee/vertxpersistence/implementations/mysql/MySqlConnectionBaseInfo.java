package com.guicedee.vertxpersistence.implementations.mysql;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.CleanVertxConnectionBaseInfo;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

/**
 * A specialized ConnectionBaseInfo implementation for MySQL.
 * This class provides MySQL-specific configuration options for Vertx SQL client.
 */
@Slf4j
public class MySqlConnectionBaseInfo extends CleanVertxConnectionBaseInfo {

    /**
     * Creates a new MySqlConnectionBaseInfo instance.
     */
    public MySqlConnectionBaseInfo() {
        super();
        setDriver("mysql");
    }

    /**
     * Creates a new MySqlConnectionBaseInfo instance with the specified XA mode.
     *
     * @param xa Whether this is an XA connection
     */
    public MySqlConnectionBaseInfo(boolean xa) {
        super(xa);
        setDriver("mysql");
    }

    /**
     * Returns a Vertx SqlClient configured for MySQL.
     *
     * @return A SqlClient instance
     */
    @Override
    public SqlClient toPooledDatasource() {
        try {
            // Get the Vertx instance from the Guice context
            Vertx vertx = VertXPreStartup.getVertx();

            // Use reflection to create MySQLConnectOptions and PoolOptions
            Class<?> mySQLConnectOptionsClass = Class.forName("io.vertx.mysqlclient.MySQLConnectOptions");
            Object connectOptions = mySQLConnectOptionsClass.getDeclaredConstructor().newInstance();

            // Set basic connection properties
            if (getServerName() != null) {
                try {
                    Method setHostMethod = mySQLConnectOptionsClass.getMethod("setHost", String.class);
                    setHostMethod.invoke(connectOptions, getServerName());
                } catch (Exception e) {
                    log.error("Error setting host for MySQL connection", e);
                }
            }

            if (getPort() != null) {
                try {
                    Method setPortMethod = mySQLConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, Integer.parseInt(getPort()));
                } catch (Exception e) {
                    log.error("Error setting port for MySQL connection", e);
                }
            } else {
                // Default MySQL port
                try {
                    Method setPortMethod = mySQLConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, 3306);
                } catch (Exception e) {
                    log.error("Error setting default port for MySQL connection", e);
                }
            }

            if (getDatabaseName() != null) {
                try {
                    Method setDatabaseMethod = mySQLConnectOptionsClass.getMethod("setDatabase", String.class);
                    setDatabaseMethod.invoke(connectOptions, getDatabaseName());
                } catch (Exception e) {
                    log.error("Error setting database for MySQL connection", e);
                }
            }

            if (getUsername() != null) {
                try {
                    Method setUserMethod = mySQLConnectOptionsClass.getMethod("setUser", String.class);
                    setUserMethod.invoke(connectOptions, getUsername());
                } catch (Exception e) {
                    log.error("Error setting user for MySQL connection", e);
                }
            }

            if (getPassword() != null) {
                try {
                    Method setPasswordMethod = mySQLConnectOptionsClass.getMethod("setPassword", String.class);
                    setPasswordMethod.invoke(connectOptions, getPassword());
                } catch (Exception e) {
                    log.error("Error setting password for MySQL connection", e);
                }
            }

            // Set connection timeout
            if (getAcquisitionTimeout() != null) {
                try {
                    Method setConnectTimeoutMethod = mySQLConnectOptionsClass.getMethod("setConnectTimeout", int.class);
                    // Convert seconds to milliseconds
                    setConnectTimeoutMethod.invoke(connectOptions, getAcquisitionTimeout() * 1000);
                } catch (Exception e) {
                    log.error("Error setting connect timeout for MySQL connection", e);
                }
            }

            // Set idle timeout
            if (getMaxIdleTime() != null) {
                try {
                    Method setIdleTimeoutMethod = mySQLConnectOptionsClass.getMethod("setIdleTimeout", int.class);
                    // Convert seconds to milliseconds
                    setIdleTimeoutMethod.invoke(connectOptions, getMaxIdleTime() * 1000);
                } catch (Exception e) {
                    log.error("Error setting idle timeout for MySQL connection", e);
                }
            }

            // Set MySQL-specific properties
            // Set SSL mode if specified in custom properties
            if (getCustomProperties().containsKey("sslMode")) {
                try {
                    Method setSslModeMethod = mySQLConnectOptionsClass.getMethod("setSslMode", String.class);
                    setSslModeMethod.invoke(connectOptions, getCustomProperties().get("sslMode"));
                } catch (NoSuchMethodException e) {
                    log.debug("setSslMode method not found in MySQLConnectOptions, skipping");
                }
            }

            // Set character set if specified in custom properties
            if (getCustomProperties().containsKey("charset")) {
                try {
                    Method setCharsetMethod = mySQLConnectOptionsClass.getMethod("setCharset", String.class);
                    setCharsetMethod.invoke(connectOptions, getCustomProperties().get("charset"));
                } catch (NoSuchMethodException e) {
                    log.debug("setCharset method not found in MySQLConnectOptions, skipping");
                }
            }

            // Set collation if specified in custom properties
            if (getCustomProperties().containsKey("collation")) {
                try {
                    Method setCollationMethod = mySQLConnectOptionsClass.getMethod("setCollation", String.class);
                    setCollationMethod.invoke(connectOptions, getCustomProperties().get("collation"));
                } catch (NoSuchMethodException e) {
                    log.debug("setCollation method not found in MySQLConnectOptions, skipping");
                }
            }

            // Set any other custom properties
            for (Map.Entry<String, String> entry : getCustomProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip properties that are not related to SQL connection or already handled
                if (key.startsWith("hibernate.") || key.startsWith("jakarta.") || key.startsWith("javax.") || 
                    key.equals("sslMode") || key.equals("charset") || key.equals("collation")) {
                    continue;
                }

                try {
                    // Try to find a setter method for this property
                    String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                    Method method = mySQLConnectOptionsClass.getMethod(methodName, String.class);
                    method.invoke(connectOptions, value);
                } catch (NoSuchMethodException e) {
                    // Property might not have a direct setter, try to use a generic property setter if available
                    try {
                        Method setPropertyMethod = mySQLConnectOptionsClass.getMethod("setProperty", String.class, String.class);
                        setPropertyMethod.invoke(connectOptions, key, value);
                    } catch (NoSuchMethodException ex) {
                        // Ignore if no generic property setter is available
                        log.debug("No setter found for property: " + key);
                    }
                }
            }

            // Configure pool options
            Class<?> poolOptionsClass;
            Object poolOptions;
            try {
                poolOptionsClass = Class.forName("io.vertx.sqlclient.PoolOptions");
                poolOptions = poolOptionsClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("Error creating PoolOptions", e);
                return null;
            }

            // Set pool size limits
            if (getMaxPoolSize() != null) {
                try {
                    Method setMaxSizeMethod = poolOptionsClass.getMethod("setMaxSize", int.class);
                    setMaxSizeMethod.invoke(poolOptions, getMaxPoolSize());
                } catch (Exception e) {
                    log.error("Error setting max pool size", e);
                }
            }

            if (getMinPoolSize() != null) {
                try {
                    Method setMinSizeMethod = poolOptionsClass.getMethod("setMinSize", int.class);
                    setMinSizeMethod.invoke(poolOptions, getMinPoolSize());
                } catch (NoSuchMethodException e) {
                    // Min size might not be supported in all versions, ignore if method not found
                    log.debug("Min size method not found in PoolOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting min pool size", e);
                }
            }

            // Set max wait queue size if acquire increment is specified
            if (getAcquireIncrement() != null) {
                try {
                    Method setMaxWaitQueueSizeMethod = poolOptionsClass.getMethod("setMaxWaitQueueSize", int.class);
                    setMaxWaitQueueSizeMethod.invoke(poolOptions, getAcquireIncrement());
                } catch (NoSuchMethodException e) {
                    // Max wait queue size might not be supported in all versions, ignore if method not found
                    log.debug("Max wait queue size method not found in PoolOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting max wait queue size", e);
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
                    log.debug("Max lifetime method not found in PoolOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting max lifetime", e);
                }
            }

            // Create the pool using MySQLPool
            try {
                Class<?> mySQLPoolClass = Class.forName("io.vertx.mysqlclient.MySQLPool");
                Method poolMethod = mySQLPoolClass.getMethod("pool", Vertx.class, mySQLConnectOptionsClass, poolOptionsClass);
                Object client = poolMethod.invoke(null, vertx, connectOptions, poolOptions);
                return (SqlClient) client;
            } catch (Exception e) {
                log.error("Error creating MySQL pool", e);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating MySQL SqlClient", e);
            return null;
        }
    }
}

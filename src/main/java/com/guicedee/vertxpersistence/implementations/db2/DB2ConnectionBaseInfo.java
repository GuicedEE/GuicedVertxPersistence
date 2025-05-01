package com.guicedee.vertxpersistence.implementations.db2;

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
 * A specialized ConnectionBaseInfo implementation for IBM DB2.
 * This class provides DB2-specific configuration options for Vertx SQL client.
 */
@Slf4j
public class DB2ConnectionBaseInfo extends CleanVertxConnectionBaseInfo {

    /**
     * Creates a new DB2ConnectionBaseInfo instance.
     */
    public DB2ConnectionBaseInfo() {
        super();
        setDriver("db2");
    }

    /**
     * Creates a new DB2ConnectionBaseInfo instance with the specified XA mode.
     *
     * @param xa Whether this is an XA connection
     */
    public DB2ConnectionBaseInfo(boolean xa) {
        super(xa);
        setDriver("db2");
    }

    /**
     * Returns a Vertx SqlClient configured for DB2.
     *
     * @return A SqlClient instance
     */
    @Override
    public SqlClient toPooledDatasource() {
        try {
            // Get the Vertx instance from the Guice context
            Vertx vertx = VertXPreStartup.getVertx();

            // Use reflection to create DB2ConnectOptions and PoolOptions
            Class<?> db2ConnectOptionsClass = Class.forName("io.vertx.db2client.DB2ConnectOptions");
            Object connectOptions = db2ConnectOptionsClass.getDeclaredConstructor().newInstance();

            // Set basic connection properties
            if (getServerName() != null) {
                try {
                    Method setHostMethod = db2ConnectOptionsClass.getMethod("setHost", String.class);
                    setHostMethod.invoke(connectOptions, getServerName());
                } catch (Exception e) {
                    log.error("Error setting host for DB2 connection", e);
                }
            }

            if (getPort() != null) {
                try {
                    Method setPortMethod = db2ConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, Integer.parseInt(getPort()));
                } catch (Exception e) {
                    log.error("Error setting port for DB2 connection", e);
                }
            } else {
                // Default DB2 port
                try {
                    Method setPortMethod = db2ConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, 50000);
                } catch (Exception e) {
                    log.error("Error setting default port for DB2 connection", e);
                }
            }

            if (getDatabaseName() != null) {
                try {
                    Method setDatabaseMethod = db2ConnectOptionsClass.getMethod("setDatabase", String.class);
                    setDatabaseMethod.invoke(connectOptions, getDatabaseName());
                } catch (Exception e) {
                    log.error("Error setting database for DB2 connection", e);
                }
            }

            if (getUsername() != null) {
                try {
                    Method setUserMethod = db2ConnectOptionsClass.getMethod("setUser", String.class);
                    setUserMethod.invoke(connectOptions, getUsername());
                } catch (Exception e) {
                    log.error("Error setting user for DB2 connection", e);
                }
            }

            if (getPassword() != null) {
                try {
                    Method setPasswordMethod = db2ConnectOptionsClass.getMethod("setPassword", String.class);
                    setPasswordMethod.invoke(connectOptions, getPassword());
                } catch (Exception e) {
                    log.error("Error setting password for DB2 connection", e);
                }
            }

            // Set connection timeout
            if (getAcquisitionTimeout() != null) {
                try {
                    Method setConnectTimeoutMethod = db2ConnectOptionsClass.getMethod("setConnectTimeout", int.class);
                    // Convert seconds to milliseconds
                    setConnectTimeoutMethod.invoke(connectOptions, getAcquisitionTimeout() * 1000);
                } catch (Exception e) {
                    log.error("Error setting connect timeout for DB2 connection", e);
                }
            }

            // Set idle timeout
            if (getMaxIdleTime() != null) {
                try {
                    Method setIdleTimeoutMethod = db2ConnectOptionsClass.getMethod("setIdleTimeout", int.class);
                    // Convert seconds to milliseconds
                    setIdleTimeoutMethod.invoke(connectOptions, getMaxIdleTime() * 1000);
                } catch (Exception e) {
                    log.error("Error setting idle timeout for DB2 connection", e);
                }
            }

            // Set DB2-specific properties
            // Set SSL if specified in custom properties
            if (getCustomProperties().containsKey("ssl")) {
                try {
                    Method setSslMethod = db2ConnectOptionsClass.getMethod("setSsl", boolean.class);
                    setSslMethod.invoke(connectOptions, Boolean.parseBoolean(getCustomProperties().get("ssl")));
                } catch (NoSuchMethodException e) {
                    log.debug("setSsl method not found in DB2ConnectOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting SSL for DB2 connection", e);
                }
            }

            // Set any other custom properties
            for (Map.Entry<String, String> entry : getCustomProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip properties that are not related to SQL connection or already handled
                if (key.startsWith("hibernate.") || key.startsWith("jakarta.") || key.startsWith("javax.") || 
                    key.equals("ssl")) {
                    continue;
                }

                try {
                    // Try to find a setter method for this property
                    String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                    Method method = db2ConnectOptionsClass.getMethod(methodName, String.class);
                    method.invoke(connectOptions, value);
                } catch (NoSuchMethodException e) {
                    // Property might not have a direct setter, try to use a generic property setter if available
                    try {
                        Method setPropertyMethod = db2ConnectOptionsClass.getMethod("setProperty", String.class, String.class);
                        setPropertyMethod.invoke(connectOptions, key, value);
                    } catch (NoSuchMethodException ex) {
                        // Ignore if no generic property setter is available
                        log.debug("No setter found for property: " + key);
                    } catch (Exception ex) {
                        log.error("Error setting property " + key + " for DB2 connection", ex);
                    }
                } catch (Exception e) {
                    log.error("Error setting property " + key + " for DB2 connection", e);
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

            // Create the pool using DB2Pool
            try {
                Class<?> db2PoolClass = Class.forName("io.vertx.db2client.DB2Pool");
                Method poolMethod = db2PoolClass.getMethod("pool", Vertx.class, db2ConnectOptionsClass, poolOptionsClass);
                Object client = poolMethod.invoke(null, vertx, connectOptions, poolOptions);
                return (SqlClient) client;
            } catch (Exception e) {
                log.error("Error creating DB2 pool", e);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating DB2 SqlClient", e);
            return null;
        }
    }
}

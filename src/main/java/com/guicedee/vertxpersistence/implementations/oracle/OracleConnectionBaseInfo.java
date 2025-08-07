package com.guicedee.vertxpersistence.implementations.oracle;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.CleanVertxConnectionBaseInfo;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * A specialized ConnectionBaseInfo implementation for Oracle Database.
 * This class provides Oracle-specific configuration options for Vertx SQL client.
 */
@Log4j2
public class OracleConnectionBaseInfo extends CleanVertxConnectionBaseInfo {

    /**
     * Creates a new OracleConnectionBaseInfo instance.
     */
    public OracleConnectionBaseInfo() {
        super();
        setDriver("oracle");
    }

    /**
     * Creates a new OracleConnectionBaseInfo instance with the specified XA mode.
     *
     * @param xa Whether this is an XA connection
     */
    public OracleConnectionBaseInfo(boolean xa) {
        super(xa);
        setDriver("oracle");
    }

    /**
     * Returns a Vertx SqlClient configured for Oracle.
     *
     * @return A SqlClient instance
     */
    @Override
    public SqlClient toPooledDatasource() {
        try {
            // Get the Vertx instance from the Guice context
            Vertx vertx = VertXPreStartup.getVertx();

            // Use reflection to create OracleConnectOptions and PoolOptions
            Class<?> oracleConnectOptionsClass;
            Object connectOptions;
            try {
                oracleConnectOptionsClass = Class.forName("io.vertx.oracleclient.OracleConnectOptions");
                connectOptions = oracleConnectOptionsClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("Error creating OracleConnectOptions", e);
                return null;
            }

            // Set basic connection properties
            if (getServerName() != null) {
                try {
                    Method setHostMethod = oracleConnectOptionsClass.getMethod("setHost", String.class);
                    setHostMethod.invoke(connectOptions, getServerName());
                } catch (Exception e) {
                    log.error("Error setting host for Oracle connection", e);
                }
            }

            if (getPort() != null) {
                try {
                    Method setPortMethod = oracleConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, Integer.parseInt(getPort()));
                } catch (Exception e) {
                    log.error("Error setting port for Oracle connection", e);
                }
            } else {
                // Default Oracle port
                try {
                    Method setPortMethod = oracleConnectOptionsClass.getMethod("setPort", int.class);
                    setPortMethod.invoke(connectOptions, 1521);
                } catch (Exception e) {
                    log.error("Error setting default port for Oracle connection", e);
                }
            }

            // Oracle uses service name or SID instead of database name
            if (getDatabaseName() != null) {
                // Try to set service name first
                try {
                    Method setDatabaseMethod = oracleConnectOptionsClass.getMethod("setDatabase", String.class);
                    setDatabaseMethod.invoke(connectOptions, getDatabaseName());
                } catch (NoSuchMethodException e) {
                    // If setDatabase is not available, try to set service name
                    try {
                        Method setServiceNameMethod = oracleConnectOptionsClass.getMethod("setServiceName", String.class);
                        setServiceNameMethod.invoke(connectOptions, getDatabaseName());
                    } catch (NoSuchMethodException ex) {
                        // If setServiceName is not available, try to set SID
                        try {
                            Method setSidMethod = oracleConnectOptionsClass.getMethod("setSid", String.class);
                            setSidMethod.invoke(connectOptions, getDatabaseName());
                        } catch (NoSuchMethodException exc) {
                            log.debug("Neither setDatabase, setServiceName, nor setSid methods found in OracleConnectOptions, skipping");
                        } catch (Exception exc) {
                            log.error("Error setting SID for Oracle connection", exc);
                        }
                    } catch (Exception ex) {
                        log.error("Error setting service name for Oracle connection", ex);
                    }
                } catch (Exception e) {
                    log.error("Error setting database for Oracle connection", e);
                }
            }

            if (getUsername() != null) {
                try {
                    Method setUserMethod = oracleConnectOptionsClass.getMethod("setUser", String.class);
                    setUserMethod.invoke(connectOptions, getUsername());
                } catch (Exception e) {
                    log.error("Error setting user for Oracle connection", e);
                }
            }

            if (getPassword() != null) {
                try {
                    Method setPasswordMethod = oracleConnectOptionsClass.getMethod("setPassword", String.class);
                    setPasswordMethod.invoke(connectOptions, getPassword());
                } catch (Exception e) {
                    log.error("Error setting password for Oracle connection", e);
                }
            }

            // Set connection timeout
            if (getAcquisitionTimeout() != null) {
                try {
                    Method setConnectTimeoutMethod = oracleConnectOptionsClass.getMethod("setConnectTimeout", int.class);
                    // Convert seconds to milliseconds
                    setConnectTimeoutMethod.invoke(connectOptions, getAcquisitionTimeout() * 1000);
                } catch (Exception e) {
                    log.error("Error setting connect timeout for Oracle connection", e);
                }
            }

            // Set idle timeout
            if (getMaxIdleTime() != null) {
                try {
                    Method setIdleTimeoutMethod = oracleConnectOptionsClass.getMethod("setIdleTimeout", int.class);
                    // Convert seconds to milliseconds
                    setIdleTimeoutMethod.invoke(connectOptions, getMaxIdleTime() * 1000);
                } catch (Exception e) {
                    log.error("Error setting idle timeout for Oracle connection", e);
                }
            }

            // Set Oracle-specific properties
            // Set service name if specified in custom properties
            if (getCustomProperties().containsKey("serviceName")) {
                try {
                    Method setServiceNameMethod = oracleConnectOptionsClass.getMethod("setServiceName", String.class);
                    setServiceNameMethod.invoke(connectOptions, getCustomProperties().get("serviceName"));
                } catch (NoSuchMethodException e) {
                    log.debug("setServiceName method not found in OracleConnectOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting service name for Oracle connection", e);
                }
            }

            // Set SID if specified in custom properties
            if (getCustomProperties().containsKey("sid")) {
                try {
                    Method setSidMethod = oracleConnectOptionsClass.getMethod("setSid", String.class);
                    setSidMethod.invoke(connectOptions, getCustomProperties().get("sid"));
                } catch (NoSuchMethodException e) {
                    log.debug("setSid method not found in OracleConnectOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting SID for Oracle connection", e);
                }
            }

            // Set SSL if specified in custom properties
            if (getCustomProperties().containsKey("ssl")) {
                try {
                    Method setSslMethod = oracleConnectOptionsClass.getMethod("setSsl", boolean.class);
                    setSslMethod.invoke(connectOptions, Boolean.parseBoolean(getCustomProperties().get("ssl")));
                } catch (NoSuchMethodException e) {
                    log.debug("setSsl method not found in OracleConnectOptions, skipping");
                } catch (Exception e) {
                    log.error("Error setting SSL for Oracle connection", e);
                }
            }

            // Set any other custom properties
            for (Map.Entry<String, String> entry : getCustomProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip properties that are not related to SQL connection or already handled
                if (key.startsWith("hibernate.") || key.startsWith("jakarta.") || key.startsWith("javax.") || 
                    key.equals("serviceName") || key.equals("sid") || key.equals("ssl")) {
                    continue;
                }

                try {
                    // Try to find a setter method for this property
                    String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                    Method method = oracleConnectOptionsClass.getMethod(methodName, String.class);
                    method.invoke(connectOptions, value);
                } catch (NoSuchMethodException e) {
                    // Property might not have a direct setter, try to use a generic property setter if available
                    try {
                        Method setPropertyMethod = oracleConnectOptionsClass.getMethod("setProperty", String.class, String.class);
                        setPropertyMethod.invoke(connectOptions, key, value);
                    } catch (NoSuchMethodException ex) {
                        // Ignore if no generic property setter is available
                        log.debug("No setter found for property: " + key);
                    } catch (Exception ex) {
                        log.error("Error setting property " + key + " for Oracle connection", ex);
                    }
                } catch (Exception e) {
                    log.error("Error setting property " + key + " for Oracle connection", e);
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

            // Create the pool using OraclePool
            try {
                Class<?> oraclePoolClass = Class.forName("io.vertx.oracleclient.OraclePool");
                Method poolMethod = oraclePoolClass.getMethod("pool", Vertx.class, oracleConnectOptionsClass, poolOptionsClass);
                Object client = poolMethod.invoke(null, vertx, connectOptions, poolOptions);
                return (SqlClient) client;
            } catch (Exception e) {
                log.error("Error creating Oracle pool", e);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating Oracle SqlClient", e);
            return null;
        }
    }
}

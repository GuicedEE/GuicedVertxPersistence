package com.guicedee.vertxpersistence;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

import io.vertx.sqlclient.SqlClient;

/**
 * This class is a basic container (mirror) for the database jtm builder string.
 * Exists to specify the default properties for connections that a jtm should implement should btm be switched for a different
 * implementation
 */
@Slf4j
public class VertxConnectionBaseInfo
		extends ConnectionBaseInfo
{
	/**
	 * Constructor VertxConnectionBaseInfo creates a new VertxConnectionBaseInfo instance with XA enabled
	 */
	public VertxConnectionBaseInfo()
	{
		setServerInstanceNameProperty("Instance");
	}

	/**
	 * Returns a Vertx SqlClient configured for use with Hibernate Reactive
	 * 
	 * @return A SqlClient instance
	 */
	@Override
	public SqlClient toPooledDatasource()
	{
		try {
			// Get the Vertx instance from the Guice context
			Vertx vertx = VertXPreStartup.getVertx();

			// Use reflection to create SqlConnectOptions and PoolOptions
			Class<?> sqlConnectOptionsClass;
			Object connectOptions;
			try {
				sqlConnectOptionsClass = Class.forName("io.vertx.sqlclient.SqlConnectOptions");
				connectOptions = sqlConnectOptionsClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				log.error("Error creating SqlConnectOptions", e);
				return null;
			}

			// Set basic connection properties
			if (getServerName() != null) {
				try {
					Method setHostMethod = sqlConnectOptionsClass.getMethod("setHost", String.class);
					setHostMethod.invoke(connectOptions, getServerName());
				} catch (Exception e) {
					log.error("Error setting host", e);
				}
			}

			if (getPort() != null) {
				try {
					Method setPortMethod = sqlConnectOptionsClass.getMethod("setPort", int.class);
					setPortMethod.invoke(connectOptions, Integer.parseInt(getPort()));
				} catch (Exception e) {
					log.error("Error setting port", e);
				}
			}

			if (getDatabaseName() != null) {
				try {
					Method setDatabaseMethod = sqlConnectOptionsClass.getMethod("setDatabase", String.class);
					setDatabaseMethod.invoke(connectOptions, getDatabaseName());
				} catch (Exception e) {
					log.error("Error setting database", e);
				}
			}

			if (getUsername() != null) {
				try {
					Method setUserMethod = sqlConnectOptionsClass.getMethod("setUser", String.class);
					setUserMethod.invoke(connectOptions, getUsername());
				} catch (Exception e) {
					log.error("Error setting user", e);
				}
			}

			if (getPassword() != null) {
				try {
					Method setPasswordMethod = sqlConnectOptionsClass.getMethod("setPassword", String.class);
					setPasswordMethod.invoke(connectOptions, getPassword());
				} catch (Exception e) {
					log.error("Error setting password", e);
				}
			}

			// Set connection timeout
			if (getAcquisitionTimeout() != null) {
				try {
					Method setConnectTimeoutMethod = sqlConnectOptionsClass.getMethod("setConnectTimeout", int.class);
					// Convert seconds to milliseconds
					setConnectTimeoutMethod.invoke(connectOptions, getAcquisitionTimeout() * 1000);
				} catch (Exception e) {
					log.error("Error setting connect timeout", e);
				}
			}

			// Set idle timeout
			if (getMaxIdleTime() != null) {
				try {
					Method setIdleTimeoutMethod = sqlConnectOptionsClass.getMethod("setIdleTimeout", int.class);
					// Convert seconds to milliseconds
					setIdleTimeoutMethod.invoke(connectOptions, getMaxIdleTime() * 1000);
				} catch (Exception e) {
					log.error("Error setting idle timeout", e);
				}
			}

			// Set transaction isolation level if specified
			if (getTransactionIsolation() != null) {
				try {
					Method setTransactionIsolationMethod = sqlConnectOptionsClass.getMethod("setTransactionIsolation", String.class);
					setTransactionIsolationMethod.invoke(connectOptions, getTransactionIsolation());
				} catch (NoSuchMethodException e) {
					// Transaction isolation might be set differently in Vertx5, ignore if method not found
					log.debug("Transaction isolation method not found in SqlConnectOptions, skipping");
				} catch (Exception e) {
					log.error("Error setting transaction isolation", e);
				}
			}

			// Set any custom properties that might be specific to the SQL client implementation
			for (Map.Entry<String, String> entry : getCustomProperties().entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();

				// Skip properties that are not related to SQL connection
				if (key.startsWith("hibernate.") || key.startsWith("jakarta.") || key.startsWith("javax.")) {
					continue;
				}

				try {
					// Try to find a setter method for this property
					String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
					Method method = sqlConnectOptionsClass.getMethod(methodName, String.class);
					method.invoke(connectOptions, value);
				} catch (NoSuchMethodException e) {
					// Property might not have a direct setter, try to use a generic property setter if available
					try {
						Method setPropertyMethod = sqlConnectOptionsClass.getMethod("setProperty", String.class, String.class);
						setPropertyMethod.invoke(connectOptions, key, value);
					} catch (NoSuchMethodException ex) {
						// Ignore if no generic property setter is available
						log.debug("No setter found for property: " + key);
					} catch (Exception ex) {
						log.error("Error setting property " + key + " with generic setter", ex);
					}
				} catch (Exception e) {
					log.error("Error setting property " + key, e);
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

			// Set connection test query if specified
			if (getTestQuery() != null && !getTestQuery().isEmpty()) {
				try {
					Method setConnectionTestQueryMethod = poolOptionsClass.getMethod("setConnectionTestQuery", String.class);
					setConnectionTestQueryMethod.invoke(poolOptions, getTestQuery());
				} catch (NoSuchMethodException e) {
					// Connection test query might not be supported in all versions, ignore if method not found
					log.debug("Connection test query method not found in PoolOptions, skipping");
				} catch (Exception e) {
					log.error("Error setting connection test query", e);
				}
			}

			// Create the pool
			try {
				Class<?> poolClass = Class.forName("io.vertx.sqlclient.Pool");
				Method poolMethod = poolClass.getMethod("pool", Vertx.class, sqlConnectOptionsClass, poolOptionsClass);
				Object client = poolMethod.invoke(null, vertx, connectOptions, poolOptions);
				return (SqlClient) client;
			} catch (Exception e) {
				log.error("Error creating pool", e);
				return null;
			}
		} catch (Exception e) {
			log.error("Error creating SqlClient", e);
			return null;
		}
	}

	/**
	 * Configures this handler as either an XA or Non-XA Resource
	 *
	 * @param xa
	 * 		If the connection is XA
	 */
	public VertxConnectionBaseInfo(boolean xa)
	{
		setXa(xa);
	}

}

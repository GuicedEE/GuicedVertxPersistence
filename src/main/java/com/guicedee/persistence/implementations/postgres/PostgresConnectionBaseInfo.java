package com.guicedee.persistence.implementations.postgres;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.persistence.CleanVertxConnectionBaseInfo;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.pool.impl.SqlClientPool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A specialized ConnectionBaseInfo implementation for PostgreSQL.
 * This class provides PostgreSQL-specific configuration options for Vertx SQL client.
 */
@Log4j2
public class PostgresConnectionBaseInfo extends CleanVertxConnectionBaseInfo {

    /**
     * Creates a new PostgresConnectionBaseInfo instance.
     */
    public PostgresConnectionBaseInfo() { 
        super();
        setDriver("postgresql");
    }

    /**
     * Creates a new PostgresConnectionBaseInfo instance with the specified XA mode.
     *
     * @param xa Whether this is an XA connection
     */
    public PostgresConnectionBaseInfo(boolean xa) {
        super(xa);
        setDriver("postgresql");
    }

    /**
     * Returns a Vertx SqlClient configured for PostgreSQL.
     *
     * @return A SqlClient instance
     */
    @Override
    public SqlClient toPooledDatasource() {
        try {
            // Get the Vertx instance from the Guice context
            Vertx vertx = VertXPreStartup.getVertx();
            // Debug: record the context/thread where the pool is created
            try {
                var ctx = io.vertx.core.Context.isOnEventLoopThread() ? io.vertx.core.Vertx.currentContext() : null;
                log.info("[DB-POOL] Creating PostgreSQL Vert.x Pool for PU='{}' on thread='{}' context='{}'", 
                        getPersistenceUnitName(), Thread.currentThread().getName(), (ctx == null ? "<none>" : ctx));
            } catch (Throwable ignore) {
                // best-effort logging only
            }

            // Use reflection to create PgConnectOptions and PoolOptions
            PgConnectOptions connectOptions;
            try {
                connectOptions = new PgConnectOptions();
            } catch (Exception e) {
                log.error("Error creating PgConnectOptions", e);
                return null;
            }

            connectOptions.setReconnectAttempts(30);
            connectOptions.setReconnectInterval(1500);
            connectOptions.setCachePreparedStatements(true);
            connectOptions.setSslMode(SslMode.DISABLE);

            // Set basic connection properties
            try
            {
                if (getServerName() != null) {
                    try {
                        connectOptions.setHost(getServerName());
                    } catch (Exception e) {
                        log.error("Error setting host for PostgreSQL connection", e);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            try
            {
                if (getPort() != null) {
                    try {
                        connectOptions.setPort(Integer.parseInt(getPort()));
                    } catch (Exception e) {
                        log.error("Error setting port for PostgreSQL connection", e);
                    }
                } else {
                    // Default PostgreSQL port
                    try {
                        connectOptions.setPort(5432);
                    } catch (Exception e) {
                        log.error("Error setting default port for PostgreSQL connection", e);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            try
            {
                if (getDatabaseName() != null) {
                    try {
                        connectOptions.setDatabase(getDatabaseName());
                    } catch (Exception e) {
                        log.error("Error setting database for PostgreSQL connection", e);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            try
            {
                if (getUsername() != null) {
                    try {
                        connectOptions.setUser(getUsername());
                    } catch (Exception e) {
                        log.error("Error setting user for PostgreSQL connection", e);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            try
            {
                if (getPassword() != null) {
                    try {
                        connectOptions.setPassword(getPassword());
                    } catch (Exception e) {
                        log.error("Error setting password for PostgreSQL connection", e);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            // Configure pool options

            PoolOptions poolOptions;
            try {
                poolOptions = new PoolOptions();
            } catch (Exception e) {
                log.error("Error creating PoolOptions", e);
                return null;
            }

            // Set pool size limits
            if (getMaxPoolSize() != null) {
                try {
                    poolOptions.setMaxSize(getMaxPoolSize());
                } catch (Exception e) {
                    log.error("Error setting max pool size", e);
                }
            }

            // Min size not supported on all Vert.x versions used by this codebase

            // Idle timeout: ConnectionBaseInfo.getMaxLifeTime() is expressed in seconds
            if (getMaxLifeTime() != null && getMaxLifeTime() > 0) {
                try {
                    poolOptions.setIdleTimeout(getMaxLifeTime());
                    poolOptions.setIdleTimeoutUnit(TimeUnit.SECONDS);
                } catch (Throwable t) {
                    log.debug("Idle timeout options not supported on this Vert.x version, skipping");
                }
            }

            // Share pool across Vert.x contexts and give it a stable name so HR can reuse the same pool
            try {
                poolOptions.setShared(true);
            } catch (Throwable t) {
                log.debug("Vert.x PoolOptions.setShared not available, skipping");
            }

            try {
                String poolName = getPersistenceUnitName();
                if (poolName == null || poolName.isBlank()) {
                    poolName = "DefaultPersistenceUnit";
                }
                // Name the pool so it is reused across contexts when the same name is requested
                // This helps Hibernate Reactive pick up the same underlying pool when configured with the same name
                PoolOptions.class.getMethod("setName", String.class).invoke(poolOptions, poolName);
                log.info("[DB-POOL] Using shared named pool='{}' maxSize={} for PostgreSQL", poolName, getMaxPoolSize());
            } catch (NoSuchMethodException e) {
                log.debug("Vert.x PoolOptions.setName not available on this version, continuing without explicit name");
            } catch (Throwable t) {
                log.warn("[DB-POOL] Unable to set pool name due to: {}", t.toString());
            }

            // Create the shared Pool and return it (Pool implements SqlClient producer)
            try {
                return Pool.pool(vertx, connectOptions, poolOptions);
            } catch (Exception e) {
                log.error("Error creating PostgreSQL Pool", e);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating PostgreSQL SqlClient", e);
            return null;
        }
    }
}

package com.guicedee.vertxpersistence.implementations.postgres;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.CleanVertxConnectionBaseInfo;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.pool.impl.SqlClientPool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A specialized ConnectionBaseInfo implementation for PostgreSQL.
 * This class provides PostgreSQL-specific configuration options for Vertx SQL client.
 */
@Slf4j
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

            poolOptions.setIdleTimeout(getMaxLifeTime());
            poolOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
            poolOptions.setShared(true);
            // Create the pool using PgPool
            try {
                Future<PgConnection> future = PgConnection.connect(vertx, connectOptions);
                return (SqlClient) future.await(5,TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error creating PostgreSQL pool", e);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating PostgreSQL SqlClient", e);
            return null;
        }
    }
}

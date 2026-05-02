package com.guicedee.persistence.implementations.db2;

import com.guicedee.persistence.CleanVertxConnectionBaseInfo;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import io.vertx.db2client.DB2Builder;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;

/**
 * A specialized ConnectionBaseInfo implementation for IBM DB2.
 * This class provides DB2-specific configuration options for Vertx SQL client.
 */
@Log4j2
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
            Vertx vertx = VertXPreStartup.getVertx();
            if (vertx == null) {
                log.error("❌ Vert.x instance is not available. Cannot create DB2 pool.");
                return null;
            }

            DB2ConnectOptions connectOptions = new DB2ConnectOptions();

            if (getServerName() != null) {
                connectOptions.setHost(getServerName());
            }

            if (getPort() != null) {
                connectOptions.setPort(Integer.parseInt(getPort()));
            } else {
                connectOptions.setPort(50000);
            }

            if (getDatabaseName() != null) {
                connectOptions.setDatabase(getDatabaseName());
            }

            if (getUsername() != null) {
                connectOptions.setUser(getUsername());
            }

            if (getPassword() != null) {
                connectOptions.setPassword(getPassword());
            }

            // Set SSL if specified in custom properties
            if (getCustomProperties().containsKey("ssl")) {
                connectOptions.setSsl(Boolean.parseBoolean(getCustomProperties().get("ssl")));
            }

            // Configure pool options
            PoolOptions poolOptions = new PoolOptions();

            if (getMaxPoolSize() != null) {
                poolOptions.setMaxSize(getMaxPoolSize());
            }

            // Set shared pool name so Hibernate Reactive can reuse it
            String puName = getPersistenceUnitName() != null ? getPersistenceUnitName() : "db2-default";
            poolOptions.setShared(true);
            poolOptions.setName(puName);

            // Create the pool using DB2Builder
            return DB2Builder.pool()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating DB2 SqlClient", e);
            return null;
        }
    }
}

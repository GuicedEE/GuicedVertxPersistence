package com.guicedee.persistence.implementations.oracle;

import com.guicedee.persistence.CleanVertxConnectionBaseInfo;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import io.vertx.oracleclient.OracleBuilder;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;

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
            Vertx vertx = VertXPreStartup.getVertx();
            if (vertx == null) {
                log.error("❌ Vert.x instance is not available. Cannot create Oracle pool.");
                return null;
            }

            OracleConnectOptions connectOptions = new OracleConnectOptions();

            if (getServerName() != null) {
                connectOptions.setHost(getServerName());
            }

            if (getPort() != null) {
                connectOptions.setPort(Integer.parseInt(getPort()));
            } else {
                connectOptions.setPort(1521);
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
            String puName = getPersistenceUnitName() != null ? getPersistenceUnitName() : "oracle-default";
            poolOptions.setShared(true);
            poolOptions.setName(puName);

            // Create the pool using OracleBuilder
            return OracleBuilder.pool()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating Oracle SqlClient", e);
            return null;
        }
    }
}

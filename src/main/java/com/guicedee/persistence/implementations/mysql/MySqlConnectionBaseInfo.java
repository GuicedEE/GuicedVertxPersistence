package com.guicedee.persistence.implementations.mysql;

import com.guicedee.persistence.CleanVertxConnectionBaseInfo;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;

/**
 * A specialized ConnectionBaseInfo implementation for MySQL.
 * This class provides MySQL-specific configuration options for Vertx SQL client.
 */
@Log4j2
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
            Vertx vertx = VertXPreStartup.getVertx();
            if (vertx == null) {
                log.error("❌ Vert.x instance is not available. Cannot create MySQL pool.");
                return null;
            }

            MySQLConnectOptions connectOptions = new MySQLConnectOptions();

            if (getServerName() != null) {
                connectOptions.setHost(getServerName());
            }

            if (getPort() != null) {
                connectOptions.setPort(Integer.parseInt(getPort()));
            } else {
                connectOptions.setPort(3306);
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

            // Set MySQL-specific properties from custom properties
            if (getCustomProperties().containsKey("charset")) {
                connectOptions.setCharset(getCustomProperties().get("charset"));
            }

            if (getCustomProperties().containsKey("collation")) {
                connectOptions.setCollation(getCustomProperties().get("collation"));
            }

            // Configure pool options
            PoolOptions poolOptions = new PoolOptions();

            if (getMaxPoolSize() != null) {
                poolOptions.setMaxSize(getMaxPoolSize());
            }

            // Set shared pool name so Hibernate Reactive can reuse it
            String puName = getPersistenceUnitName() != null ? getPersistenceUnitName() : "mysql-default";
            poolOptions.setShared(true);
            poolOptions.setName(puName);

            // Create the pool using MySQLBuilder
            return MySQLBuilder.pool()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating MySQL SqlClient", e);
            return null;
        }
    }
}

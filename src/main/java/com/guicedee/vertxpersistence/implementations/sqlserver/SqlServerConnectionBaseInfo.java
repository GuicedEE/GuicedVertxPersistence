package com.guicedee.vertxpersistence.implementations.sqlserver;

import com.google.common.base.Strings;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.guicedee.vertxpersistence.CleanVertxConnectionBaseInfo;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.mssqlclient.MSSQLBuilder;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * A specialized ConnectionBaseInfo implementation for Microsoft SQL Server.
 * This class provides SQL Server-specific configuration options for Vertx SQL client.
 */
@Log4j2
public class SqlServerConnectionBaseInfo extends ConnectionBaseInfo {

    private boolean trustServerCertificate;

    /**
     * Gets whether to trust the server certificate.
     *
     * @return True if the server certificate should be trusted, false otherwise
     */
    public boolean isTrustServerCertificate() {
        return trustServerCertificate;
    }

    /**
     * Sets whether to trust the server certificate.
     *
     * @param trustServerCertificate True to trust the server certificate, false otherwise
     * @return This instance
     */
    public SqlServerConnectionBaseInfo setTrustServerCertificate(boolean trustServerCertificate) {
        this.trustServerCertificate = trustServerCertificate;
        return this;
    }

    /**
     * Creates a new SqlServerConnectionBaseInfo instance.
     */
    public SqlServerConnectionBaseInfo() {
        super();
        setDriver("sqlserver");
    }


    @Override
    public SqlClient toPooledDatasource() {
        try {
            // Get the Vertx instance from the Guice context
            Vertx vertx = VertXPreStartup.getVertx();

            io.vertx.mssqlclient.MSSQLConnectOptions connectOptions;

            // Set basic connection properties
            if (getUrl() != null && !getUrl().isEmpty()) {
                connectOptions = io.vertx.mssqlclient.MSSQLConnectOptions.fromUri(getUrl());
            } else {
                connectOptions = new io.vertx.mssqlclient.MSSQLConnectOptions();
            }

            if (getServerName() != null) {
                connectOptions.setHost(getServerName());
            }

            if (getPort() != null) {
                connectOptions.setPort(Integer.parseInt(getPort()));
            } else {
                // Default SQL Server port
                connectOptions.setPort(1433);
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
/*

            // Set connection timeout
            if (getAcquisitionTimeout() != null) {
                connectOptions.addProperty("connectTimeout", String.valueOf(getAcquisitionTimeout() * 1000));
            }

            // Set idle timeout
            if (getMaxIdleTime() != null) {
                connectOptions.addProperty("idleTimeout", String.valueOf(getMaxIdleTime() * 1000));
            }

            // Set SQL Server-specific properties
            // Set instance name if specified
            if (getInstanceName() != null) {
                connectOptions.addProperty("instanceName", getInstanceName());
            }

            // Handle integrated security and trust server certificate from custom properties or URL
            if (getCustomProperties().containsKey("integratedSecurity") || (getUrl() != null && getUrl().contains("integratedSecurity=true"))) {
                connectOptions.addProperty("integratedSecurity", "true");
            }

            // Authentication method
            String authentication = getCustomProperties().get("authentication");
            if (authentication == null && getUrl() != null && getUrl().contains("authentication=")) {
                int start = getUrl().indexOf("authentication=") + "authentication=".length();
                int end = getUrl().indexOf(";", start);
                if (end == -1) end = getUrl().indexOf("&", start);
                if (end == -1) end = getUrl().length();
                authentication = getUrl().substring(start, end);
            }
            if (authentication != null) {
                connectOptions.addProperty("authentication", authentication.toUpperCase());
            }

            // Handle encryption
            if (getCustomProperties().containsKey("encrypt") || (getUrl() != null && getUrl().contains("encrypt=true"))) {
                connectOptions.addProperty("encrypt", "true");
            }

            // Handle Trust Store properties
            String trustStorePath = getCustomProperties().get("trustStorePath");
            if (trustStorePath != null) {
                JksOptions jksOptions = new JksOptions();
                jksOptions.setPath(trustStorePath);

                String trustStorePassword = getCustomProperties().get("trustStorePassword");
                if (trustStorePassword != null) {
                    jksOptions.setPassword(trustStorePassword);
                }
                connectOptions.setSsl(true);
                if (connectOptions.getSslOptions() != null) {
                    connectOptions.getSslOptions().setTrustOptions(jksOptions);
                } else {
                    connectOptions.setSslOptions(new io.vertx.core.net.ClientSSLOptions().setTrustOptions(jksOptions));
                }
            }

            // Handle Workstation ID
            String workstationId = getCustomProperties().get("workstationId");
            if (workstationId == null && getUrl() != null && getUrl().contains("workstationId=")) {
                // Simple extraction from URL if present
                int start = getUrl().indexOf("workstationId=") + "workstationId=".length();
                int end = getUrl().indexOf(";", start);
                if (end == -1) end = getUrl().length();
                workstationId = getUrl().substring(start, end);
            }
            if (workstationId != null) {
                connectOptions.addProperty("workstationId", workstationId);
            }

            // Handle Application Name
            String applicationName = getCustomProperties().get("applicationName");
            if (applicationName == null && getUrl() != null && getUrl().contains("applicationName=")) {
                int start = getUrl().indexOf("applicationName=") + "applicationName=".length();
                int end = getUrl().indexOf(";", start);
                if (end == -1) end = getUrl().indexOf("&", start); // Fallback for some URL formats
                if (end == -1) end = getUrl().length();
                applicationName = getUrl().substring(start, end);
            }
            if (applicationName != null) {
                connectOptions.addProperty("applicationName", applicationName);
            }

            // Handle current schema
            String currentSchema = getCustomProperties().get("currentSchema");
            if (currentSchema == null && getUrl() != null && getUrl().contains("currentSchema=")) {
                int start = getUrl().indexOf("currentSchema=") + "currentSchema=".length();
                int end = getUrl().indexOf(";", start);
                if (end == -1) end = getUrl().indexOf("&", start);
                if (end == -1) end = getUrl().length();
                currentSchema = getUrl().substring(start, end);
            }
            if (currentSchema != null) {
                connectOptions.addProperty("currentSchema", currentSchema);
            }

            // Handle row fetch size
            String rowFetchSize = getCustomProperties().get("rowFetchSize");
            if (rowFetchSize == null && getUrl() != null && getUrl().contains("rowFetchSize=")) {
                int start = getUrl().indexOf("rowFetchSize=") + "rowFetchSize=".length();
                int end = getUrl().indexOf(";", start);
                if (end == -1) end = getUrl().indexOf("&", start);
                if (end == -1) end = getUrl().length();
                rowFetchSize = getUrl().substring(start, end);
            }
            if (rowFetchSize != null) {
                connectOptions.addProperty("rowFetchSize", rowFetchSize);
            }

            // Handle Key Store properties
            String keyStorePath = getCustomProperties().get("keyStorePath");
            if (keyStorePath != null) {
                JksOptions jksOptions = new JksOptions();
                jksOptions.setPath(keyStorePath);

                String keyStorePassword = getCustomProperties().get("keyStorePassword");
                if (keyStorePassword != null) {
                    jksOptions.setPassword(keyStorePassword);
                }
                connectOptions.setSsl(true);
                if (connectOptions.getSslOptions() != null) {
                    connectOptions.getSslOptions().setKeyCertOptions(jksOptions);
                } else {
                    connectOptions.setSslOptions(new io.vertx.core.net.ClientSSLOptions().setKeyCertOptions(jksOptions));
                }
            }

            // Set packet size if specified in custom properties
            if (getCustomProperties().containsKey("packetSize")) {
                try {
                    connectOptions.setPacketSize(Integer.parseInt(getCustomProperties().get("packetSize")));
                } catch (NumberFormatException e) {
                    log.debug("Invalid packet size value: " + getCustomProperties().get("packetSize"));
                }
            }

            // Set SSL if specified in custom properties
            if (getCustomProperties().containsKey("ssl")) {
                connectOptions.setSsl(Boolean.parseBoolean(getCustomProperties().get("ssl")));
            }

            // Set any other custom properties
            for (Map.Entry<String, String> entry : getCustomProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip properties that are not related to SQL connection or already handled
                if (key.startsWith("hibernate.") || key.startsWith("jakarta.") || key.startsWith("javax.") || 
                    key.equals("packetSize") || key.equals("ssl") || key.equals("integratedSecurity") || 
                    key.equals("trustServerCertificate") || key.equals("encrypt") || key.equals("workstationId") || 
                    key.equals("applicationName") || key.equals("currentSchema") || key.equals("authentication") ||
                    key.equals("trustStorePath") || key.equals("trustStorePassword") || key.equals("rowFetchSize") ||
                    key.equals("keyStorePath") || key.equals("keyStorePassword")) {
                    continue;
                }

                try {
                    // Try to use a generic property setter if available
                    connectOptions.getProperties().put(key, value);
                } catch (Exception e) {
                    log.error("Error setting property " + key + " for SQL Server connection", e);
                }
            }
*/

            // Configure pool options
            PoolOptions poolOptions = new PoolOptions();

            // Set pool size limits
            if (getMaxPoolSize() != null) {
                poolOptions.setMaxSize(getMaxPoolSize());
            }

      /*      if (getMinPoolSize() != null) {
                try {
                    java.lang.reflect.Method method = poolOptions.getClass().getMethod("setMinSize", int.class);
                    method.invoke(poolOptions, getMinPoolSize());
                } catch (Exception e) {
                    log.error("Error setting min pool size", e);
                }
            }*/

            // Set max wait queue size if acquire increment is specified
            if (getAcquireIncrement() != null) {
                poolOptions.setMaxWaitQueueSize(getAcquireIncrement());
            }

            // Set connection lifetime
            if (getMaxLifeTime() != null) {
                // Convert seconds to milliseconds
                poolOptions.setMaxLifetime(getMaxLifeTime() * 1000);
            }

            if (getCustomProperties().containsKey("trustServerCertificate") || (getUrl() != null && getUrl().contains("trustServerCertificate=true")) || isTrustServerCertificate()) {
                connectOptions.setSsl(true);
                if (connectOptions.getSslOptions() != null) {
                    connectOptions.getSslOptions().setTrustAll(true);
                } else {
                    connectOptions.setSslOptions(new io.vertx.core.net.ClientSSLOptions().setTrustAll(true));
                }
            }

            if (!Strings.isNullOrEmpty(getUrl())) {
                return MSSQLBuilder.pool()
                        .with(new NetClientOptions().setSsl(true).setTrustAll(true))
                        .with(poolOptions)
                        .connectingTo(getUrl())
                        .using(vertx)
                        .build();
            }


            // Create the pool using MSSQLBuilder
            return MSSQLBuilder.pool()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build();
        } catch (Exception e) {
            log.error("Error creating SQL Server SqlClient", e);
            return null;
        }
    }
}
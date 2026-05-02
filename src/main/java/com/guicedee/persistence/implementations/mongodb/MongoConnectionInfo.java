package com.guicedee.persistence.implementations.mongodb;

import com.fasterxml.jackson.annotation.*;
import io.vertx.core.json.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Configuration holder for a Vert.x MongoDB client connection.
 * <p>
 * Supports both connection-string mode ({@link #connectionString}) and
 * discrete property mode (host / port / username / password / db_name).
 * When a connection string is supplied it takes precedence.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(exclude = {"password"})
@EqualsAndHashCode(of = {"name"})
@Getter
@Setter
@Accessors(chain = true)
public class MongoConnectionInfo {

    /**
     * A logical name for this connection (used as the Guice @Named qualifier).
     */
    private String name = "default";

    /**
     * MongoDB connection string, e.g. {@code mongodb://localhost:27017}.
     * When set, host/port/username/password are ignored.
     */
    private String connectionString;

    /**
     * The host the MongoDB instance is running on. Defaults to {@code 127.0.0.1}.
     */
    private String host = "127.0.0.1";

    /**
     * The port the MongoDB instance is listening on. Defaults to {@code 27017}.
     */
    private int port = 27017;

    /**
     * The database name. Defaults to {@code default_db}.
     */
    private String databaseName = "default_db";

    /**
     * The username for authentication (optional).
     */
    private String username;

    /**
     * The password for authentication (optional).
     */
    @JsonIgnore
    private String password;

    /**
     * The authentication source database (optional).
     */
    private String authSource;

    /**
     * Whether to toggle ObjectId support. Defaults to {@code false}.
     */
    private boolean useObjectId = false;

    /**
     * Maximum number of connections in the pool. Defaults to {@code 100}.
     */
    private int maxPoolSize = 100;

    /**
     * Minimum number of connections in the pool. Defaults to {@code 0}.
     */
    private int minPoolSize = 0;

    /**
     * Maximum idle time of a pooled connection in milliseconds.
     */
    private long maxIdleTimeMS = 0;

    /**
     * Maximum time a pooled connection can live for in milliseconds.
     */
    private long maxLifeTimeMS = 0;

    /**
     * Connection timeout in milliseconds. Defaults to {@code 10000}.
     */
    private int connectTimeoutMS = 10000;

    /**
     * Socket timeout in milliseconds. Defaults to {@code 0} (no timeout).
     */
    private int socketTimeoutMS = 0;

    /**
     * Enable SSL. Defaults to {@code false}.
     */
    private boolean ssl = false;

    /**
     * Trust all certificates when using SSL. Defaults to {@code false}.
     */
    private boolean trustAll = false;

    /**
     * If this is the default MongoClient binding (no @Named required).
     */
    private boolean defaultConnection = true;

    /**
     * Any additional custom properties.
     */
    private final Map<String, Object> customProperties = new HashMap<>();

    /**
     * Converts this connection info to a Vert.x {@link JsonObject} suitable for
     * {@code MongoClient.createShared(vertx, config)}.
     *
     * @return the configuration as a JsonObject
     */
    public JsonObject toJsonConfig() {
        JsonObject config = new JsonObject();

        if (connectionString != null && !connectionString.isEmpty()) {
            config.put("connection_string", connectionString);
        } else {
            config.put("host", host);
            config.put("port", port);
            if (username != null && !username.isEmpty()) {
                config.put("username", username);
            }
            if (password != null && !password.isEmpty()) {
                config.put("password", password);
            }
            if (authSource != null && !authSource.isEmpty()) {
                config.put("authSource", authSource);
            }
        }

        config.put("db_name", databaseName);
        config.put("useObjectId", useObjectId);
        config.put("maxPoolSize", maxPoolSize);
        config.put("minPoolSize", minPoolSize);

        if (maxIdleTimeMS > 0) {
            config.put("maxIdleTimeMS", maxIdleTimeMS);
        }
        if (maxLifeTimeMS > 0) {
            config.put("maxLifeTimeMS", maxLifeTimeMS);
        }
        if (connectTimeoutMS > 0) {
            config.put("connectTimeoutMS", connectTimeoutMS);
        }
        if (socketTimeoutMS > 0) {
            config.put("socketTimeoutMS", socketTimeoutMS);
        }
        if (ssl) {
            config.put("ssl", true);
            config.put("trustAll", trustAll);
        }

        // Add any custom properties
        for (Map.Entry<String, Object> entry : customProperties.entrySet()) {
            config.put(entry.getKey(), entry.getValue());
        }

        return config;
    }
}


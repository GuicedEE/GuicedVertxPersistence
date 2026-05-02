package com.guicedee.persistence.implementations.cassandra;

import com.fasterxml.jackson.annotation.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Configuration holder for a Vert.x Cassandra client connection.
 * <p>
 * Supports multiple contact points and an optional keyspace.
 * Maps directly to {@code CassandraClientOptions}.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(exclude = {"password"})
@EqualsAndHashCode(of = {"name"})
@Getter
@Setter
@Accessors(chain = true)
public class CassandraConnectionInfo {

    /**
     * A logical name for this connection (used as the Guice @Named qualifier
     * and as the shared client name).
     */
    private String name = "default";

    /**
     * Contact points for the Cassandra cluster.
     * Each entry is a {@code host:port} pair.
     * If empty, defaults to {@code localhost:9042}.
     */
    private final List<ContactPoint> contactPoints = new ArrayList<>();

    /**
     * The keyspace to connect to (optional).
     */
    private String keyspace;

    /**
     * Username for authentication (optional).
     */
    private String username;

    /**
     * Password for authentication (optional).
     */
    @JsonIgnore
    private String password;

    /**
     * If this is the default CassandraClient binding (no @Named required).
     */
    private boolean defaultConnection = true;

    /**
     * Adds a contact point.
     *
     * @param host the host address
     * @param port the port (typically 9042)
     * @return this instance for chaining
     */
    public CassandraConnectionInfo addContactPoint(String host, int port) {
        contactPoints.add(new ContactPoint(host, port));
        return this;
    }

    /**
     * A single contact point in the Cassandra cluster.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
    public static class ContactPoint {
        private String host;
        private int port;

        public ContactPoint() {}

        public ContactPoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}


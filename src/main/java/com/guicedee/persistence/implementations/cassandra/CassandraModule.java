package com.guicedee.persistence.implementations.cassandra;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.cassandra.CassandraClient;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.Vertx;
import lombok.extern.log4j.Log4j2;

/**
 * A Guice module that creates and binds a Vert.x {@link CassandraClient} into the injector.
 * <p>
 * Cassandra is a wide-column NoSQL database and does <strong>not</strong> use JPA / Hibernate.
 * This module directly creates a shared {@link CassandraClient} and binds it with an optional
 * {@code @Named} qualifier.
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Subclass this class and implement {@link #getCassandraConnectionInfo()}.</li>
 *   <li>Register the subclass as an {@code IGuiceModule} SPI provider.</li>
 *   <li>Inject {@code CassandraClient} (optionally with {@code @Named("yourName")}).</li>
 * </ol>
 *
 * <pre>{@code
 * public class MyCassandraModule extends CassandraModule<MyCassandraModule> {
 *     @Override
 *     protected CassandraConnectionInfo getCassandraConnectionInfo() {
 *         return new CassandraConnectionInfo()
 *                 .addContactPoint("localhost", 9042)
 *                 .setKeyspace("my_keyspace");
 *     }
 * }
 * }</pre>
 */
@Log4j2
public abstract class CassandraModule<J extends CassandraModule<J>>
        extends AbstractModule
        implements IGuiceModule<J>, IGuicePreDestroy<J> {

    private CassandraClient cassandraClient;

    /**
     * Provides the Cassandra connection info for this module.
     *
     * @return the connection info describing how to connect to Cassandra
     */
    protected abstract CassandraConnectionInfo getCassandraConnectionInfo();

    @Override
    protected void configure() {
        CassandraConnectionInfo info = getCassandraConnectionInfo();
        if (info == null) {
            log.error("❌ CassandraConnectionInfo returned null from {}", getClass().getName());
            return;
        }

        log.info("🔷 Configuring Cassandra module '{}' — keyspace={}", info.getName(), info.getKeyspace());

        try {
            Vertx vertx = VertXPreStartup.getVertx();
            if (vertx == null) {
                log.error("❌ Vert.x instance is not available. Cannot create CassandraClient.");
                return;
            }

            CassandraClientOptions options = new CassandraClientOptions();

            // Add contact points
            if (info.getContactPoints().isEmpty()) {
                options.addContactPoint("localhost", 9042);
            } else {
                for (CassandraConnectionInfo.ContactPoint cp : info.getContactPoints()) {
                    options.addContactPoint(cp.getHost(), cp.getPort());
                }
            }

            // Set keyspace
            if (info.getKeyspace() != null && !info.getKeyspace().isEmpty()) {
                options.setKeyspace(info.getKeyspace());
            }

            cassandraClient = CassandraClient.createShared(vertx, info.getName(), options);

            // Bind with @Named qualifier
            bind(CassandraClient.class)
                    .annotatedWith(Names.named(info.getName()))
                    .toInstance(cassandraClient);

            // If this is the default connection, also bind without @Named
            if (info.isDefaultConnection()) {
                bind(CassandraClient.class).toInstance(cassandraClient);
            }

            log.info("✅ CassandraClient bound as @Named(\"{}\"){}",
                    info.getName(),
                    info.isDefaultConnection() ? " [default]" : "");

        } catch (Throwable t) {
            log.error("❌ Failed to create CassandraClient for '{}': {}", info.getName(), t.getMessage(), t);
        }
    }

    @Override
    public void onDestroy() {
        if (cassandraClient != null) {
            try {
                cassandraClient.close();
                log.info("🛑 CassandraClient '{}' closed", getCassandraConnectionInfo().getName());
            } catch (Throwable t) {
                log.debug("⚠️ CassandraClient close failed: {}", t.getMessage());
            }
        }
    }

    @Override
    public Integer sortOrder() {
        return 50;
    }
}


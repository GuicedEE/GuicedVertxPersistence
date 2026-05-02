package com.guicedee.persistence.implementations.mongodb;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import lombok.extern.log4j.Log4j2;

/**
 * A Guice module that creates and binds a Vert.x {@link MongoClient} into the injector.
 * <p>
 * MongoDB is a document database and does <strong>not</strong> use JPA / Hibernate.
 * This module therefore does <em>not</em> extend {@code DatabaseModule}; instead it
 * directly creates a shared {@link MongoClient} and binds it with an optional
 * {@code @Named} qualifier.
 * <p>
 * <h3>Usage</h3>
 * <ol>
 *   <li>Subclass this class and implement {@link #getMongoConnectionInfo()}.</li>
 *   <li>Register the subclass as an {@code IGuiceModule} SPI provider.</li>
 *   <li>Inject {@code MongoClient} (optionally with {@code @Named("yourName")}).</li>
 * </ol>
 *
 * <pre>{@code
 * public class MyMongoModule extends MongoModule<MyMongoModule> {
 *     @Override
 *     protected MongoConnectionInfo getMongoConnectionInfo() {
 *         return new MongoConnectionInfo()
 *                 .setConnectionString("mongodb://localhost:27017")
 *                 .setDatabaseName("mydb");
 *     }
 * }
 * }</pre>
 */
@Log4j2
public abstract class MongoModule<J extends MongoModule<J>>
        extends AbstractModule
        implements IGuiceModule<J>, IGuicePreDestroy<J> {

    private MongoClient mongoClient;

    /**
     * Provides the MongoDB connection info for this module.
     *
     * @return the connection info describing how to connect to MongoDB
     */
    protected abstract MongoConnectionInfo getMongoConnectionInfo();

    @Override
    protected void configure() {
        MongoConnectionInfo info = getMongoConnectionInfo();
        if (info == null) {
            log.error("❌ MongoConnectionInfo returned null from {}", getClass().getName());
            return;
        }

        log.info("🍃 Configuring MongoDB module '{}' — db_name={}", info.getName(), info.getDatabaseName());

        try {
            Vertx vertx = VertXPreStartup.getVertx();
            if (vertx == null) {
                log.error("❌ Vert.x instance is not available. Cannot create MongoClient.");
                return;
            }

            JsonObject config = info.toJsonConfig();
            mongoClient = MongoClient.createShared(vertx, config, info.getName());

            // Bind with @Named qualifier
            bind(MongoClient.class)
                    .annotatedWith(Names.named(info.getName()))
                    .toInstance(mongoClient);

            // If this is the default connection, also bind without @Named
            if (info.isDefaultConnection()) {
                bind(MongoClient.class).toInstance(mongoClient);
            }

            log.info("✅ MongoClient bound as @Named(\"{}\"){}",
                    info.getName(),
                    info.isDefaultConnection() ? " [default]" : "");

        } catch (Throwable t) {
            log.error("❌ Failed to create MongoClient for '{}': {}", info.getName(), t.getMessage(), t);
        }
    }

    @Override
    public void onDestroy() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                log.info("🛑 MongoClient '{}' closed", getMongoConnectionInfo().getName());
            } catch (Throwable t) {
                log.debug("⚠️ MongoClient close failed: {}", t.getMessage());
            }
        }
    }

    @Override
    public Integer sortOrder() {
        return 50;
    }
}


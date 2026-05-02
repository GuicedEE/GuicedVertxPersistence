package com.guicedee.persistence.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MongoDB support using Testcontainers.
 * <p>
 * Starts a MongoDB container, configures the {@link TestMongoModule} via system properties,
 * boots the GuicedEE context, and verifies that:
 * <ol>
 *   <li>A {@link MongoClient} is properly bound and injectable</li>
 *   <li>Documents can be saved and retrieved</li>
 *   <li>Collections can be listed</li>
 * </ol>
 */
@Testcontainers
@Log4j2
public class MongoDBTest {

    @Container
    private static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0")
            .withStartupTimeout(Duration.ofMinutes(2));

    @BeforeAll
    public static void setup() {
        IGuiceContext.contexts.clear();
        IGuiceContext.registerModuleForScanning.clear();
        IGuiceContext.modules.clear();
        IGuiceContext.allLoadedServices.clear();

        mongoContainer.start();

        // Set system properties for the TestMongoModule
        System.setProperty("mongo.connectionString", mongoContainer.getConnectionString());
        System.setProperty("mongo.database", "testdb");

        log.info("MongoDB container started at: {}", mongoContainer.getConnectionString());
    }

    @Test
    public void testMongoClientBinding() throws Exception {
        // Register our test module
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        // Verify the MongoClient is bound with @Named
        MongoClient namedClient = IGuiceContext.get(Key.get(MongoClient.class, Names.named("testMongo")));
        assertNotNull(namedClient, "MongoClient @Named(\"testMongo\") should not be null");

        // Verify the default MongoClient is also bound
        MongoClient defaultClient = IGuiceContext.get(MongoClient.class);
        assertNotNull(defaultClient, "Default MongoClient should not be null");

        log.info("✅ MongoClient successfully injected");
    }

    @Test
    public void testSaveAndFind() throws Exception {
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        MongoClient client = IGuiceContext.get(MongoClient.class);
        assertNotNull(client);

        CountDownLatch latch = new CountDownLatch(1);
        final String[] savedId = {null};

        // Save a document
        JsonObject document = new JsonObject()
                .put("title", "The Hobbit")
                .put("author", "J. R. R. Tolkien");

        client.save("books", document).onComplete(res -> {
            if (res.succeeded()) {
                savedId[0] = res.result();
                log.info("✅ Saved document with id: {}", savedId[0]);

                // Now find it
                client.find("books", new JsonObject().put("title", "The Hobbit")).onComplete(findRes -> {
                    if (findRes.succeeded()) {
                        assertFalse(findRes.result().isEmpty(), "Should find at least one book");
                        JsonObject found = findRes.result().get(0);
                        assertEquals("The Hobbit", found.getString("title"));
                        assertEquals("J. R. R. Tolkien", found.getString("author"));
                        log.info("✅ Found document: {}", found.encodePrettily());
                    } else {
                        fail("Find failed: " + findRes.cause().getMessage());
                    }
                    latch.countDown();
                });
            } else {
                fail("Save failed: " + res.cause().getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Operation should complete within 30 seconds");
    }

    @Test
    public void testListCollections() throws Exception {
        IGuiceContext.registerModule("com.guicedee.guicedpersistence.test");
        IGuiceContext.getContext().inject();

        MongoClient client = IGuiceContext.get(MongoClient.class);
        assertNotNull(client);

        CountDownLatch latch = new CountDownLatch(1);

        // Insert a document to ensure collection exists
        client.save("testCollection", new JsonObject().put("key", "value")).onComplete(saveRes -> {
            if (saveRes.succeeded()) {
                client.getCollections().onComplete(res -> {
                    if (res.succeeded()) {
                        log.info("✅ Collections: {}", res.result());
                        assertTrue(res.result().contains("testCollection"),
                                "Should contain 'testCollection'");
                    } else {
                        fail("getCollections failed: " + res.cause().getMessage());
                    }
                    latch.countDown();
                });
            } else {
                fail("Save failed: " + saveRes.cause().getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Operation should complete within 30 seconds");
    }
}


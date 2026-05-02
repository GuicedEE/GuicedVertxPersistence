package com.guicedee.persistence.test;

import com.guicedee.persistence.implementations.mongodb.MongoConnectionInfo;
import com.guicedee.persistence.implementations.mongodb.MongoModule;

/**
 * Test Guice module that configures a MongoClient for the MongoDB testcontainer.
 * Connection details are set from system properties populated by {@link MongoDBTest}.
 */
public class TestMongoModule extends MongoModule<TestMongoModule> {

    @Override
    protected MongoConnectionInfo getMongoConnectionInfo() {
        return new MongoConnectionInfo()
                .setName("testMongo")
                .setConnectionString(System.getProperty("mongo.connectionString", "mongodb://localhost:27017"))
                .setDatabaseName(System.getProperty("mongo.database", "testdb"))
                .setDefaultConnection(true);
    }
}


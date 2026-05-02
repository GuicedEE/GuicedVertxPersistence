package com.guicedee.persistence.test;

import com.guicedee.persistence.implementations.cassandra.CassandraConnectionInfo;
import com.guicedee.persistence.implementations.cassandra.CassandraModule;

/**
 * Test Guice module that configures a CassandraClient for the Cassandra testcontainer.
 * Connection details are set from system properties populated by {@link CassandraTest}.
 */
public class TestCassandraModule extends CassandraModule<TestCassandraModule> {

    @Override
    protected CassandraConnectionInfo getCassandraConnectionInfo() {
        String host = System.getProperty("cassandra.host", "localhost");
        int port = Integer.parseInt(System.getProperty("cassandra.port", "9042"));

        return new CassandraConnectionInfo()
                .setName("testCassandra")
                .addContactPoint(host, port)
                .setDefaultConnection(true);
    }
}


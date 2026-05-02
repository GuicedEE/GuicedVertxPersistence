import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.test.TestModulePostgresReactive;
import com.guicedee.persistence.test.TestMongoModule;

import com.guicedee.persistence.test.TestCassandraModule;
import com.guicedee.persistence.test.TestModuleMySQLReactive;
import com.guicedee.persistence.test.TestModuleDB2Reactive;
import com.guicedee.persistence.test.TestModuleOracleReactive;
import com.guicedee.persistence.test.TestModuleMSSQLReactive;

open module guiced.persistence.test {

    requires com.google.guice;
    requires transitive org.testcontainers;
    requires transitive com.guicedee.persistence;
    requires transitive com.sun.jna;

    provides IGuiceModule with TestModulePostgresReactive, TestMongoModule, TestCassandraModule, TestModuleMySQLReactive, TestModuleDB2Reactive, TestModuleOracleReactive, TestModuleMSSQLReactive;

    //reactive
    requires io.vertx.sql.client;
    requires io.vertx.sql.client.pg;
    requires io.vertx.sql.client.mysql;
    requires io.vertx.sql.client.db2;
    requires io.vertx.sql.client.oracle;
    requires io.vertx.sql.client.mssql;
    requires com.ongres.scram.client;
    requires com.guicedee.vertx;
    requires io.vertx.mongo.client;
    requires io.vertx.cassandra.client;
    //requires org.postgresql.jdbc;


    requires jakarta.persistence;

    requires org.junit.jupiter.api;
    requires org.slf4j;

    requires junit;
    requires static lombok;
    requires com.datastax.oss.driver.core;
    //requires io.smallrye.mutiny.vertx.core;
}

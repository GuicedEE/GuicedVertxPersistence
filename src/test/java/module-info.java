import com.guicedee.vertxpersistence.DatabaseModule;
import com.guicedee.vertxpersistence.test.TestModulePostgresReactive;

open module guiced.vertx.persistence.test {

    requires com.google.guice;
    requires transitive org.testcontainers;
    requires transitive com.guicedee.vertxpersistence;
    requires transitive com.sun.jna;

    provides DatabaseModule with TestModulePostgresReactive;


    //reactive
    requires io.vertx.sql.client;
    requires io.vertx.sql.client.pg;
    //jdbc
    //requires org.postgresql.jdbc;


    requires jakarta.persistence;

    requires org.junit.jupiter.api;
    requires org.slf4j;

    requires junit;
    requires static lombok;
    //requires io.smallrye.mutiny.vertx.core;
}

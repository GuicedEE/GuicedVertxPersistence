import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.test.TestModulePostgresReactive;

open module guiced.persistence.test {

    requires com.google.guice;
    requires transitive org.testcontainers;
    requires transitive com.guicedee.persistence;
    requires transitive com.sun.jna;

    provides IGuiceModule with TestModulePostgresReactive;

    //reactive
    requires io.vertx.sql.client;
    requires io.vertx.sql.client.pg;
    requires com.ongres.scram.client;
    //jdbc
    //requires org.postgresql.jdbc;


    requires jakarta.persistence;

    requires org.junit.jupiter.api;
    requires org.slf4j;

    requires junit;
    requires static lombok;
    //requires io.smallrye.mutiny.vertx.core;
}

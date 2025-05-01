open module guiced.vertx.persistence.test {

    exports com.guicedee.vertxpersistence.test;

    requires transitive com.guicedee.vertxpersistence;
    requires transitive org.postgresql.jdbc;
    requires transitive org.junit.jupiter.api;

    requires com.guicedee.tests;
    requires io.vertx.sql.client.pg;

    requires junit;

    requires com.google.guice;
    requires com.google.guice.extensions.persist;
    requires com.guicedee.client;

    requires org.hibernate.reactive;


    requires org.slf4j;

}
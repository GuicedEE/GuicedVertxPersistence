open module guiced.vertx.persistence.test {
    requires com.google.guice;
    requires com.google.guice.extensions.persist;
    requires com.guicedee.client;
    requires org.testcontainers;
    requires com.guicedee.vertxpersistence;
    requires io.vertx.sql.client;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.hibernate.reactive;
    requires org.junit.jupiter.api;

    requires transitive junit;
}
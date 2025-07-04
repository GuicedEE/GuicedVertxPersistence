open module guiced.vertx.persistence.test {

    requires com.google.guice;
    requires org.testcontainers;
    requires transitive com.guicedee.vertxpersistence;



    //reactive
    requires io.vertx.sql.client;
    requires io.vertx.sql.client.pg;
    //jdbc
    //requires org.postgresql.jdbc;


    requires jakarta.persistence;

    requires org.junit.jupiter.api;
    requires org.slf4j;

    requires transitive junit;
    requires static lombok;
    //requires io.smallrye.mutiny.vertx.core;
}
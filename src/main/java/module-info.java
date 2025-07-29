import com.guicedee.guicedinjection.interfaces.IGuiceConfigurator;
import com.guicedee.vertxpersistence.IPropertiesConnectionInfoReader;
import com.guicedee.vertxpersistence.IPropertiesEntityManagerReader;
import com.guicedee.vertxpersistence.implementations.GuicedConfigurator;
import com.guicedee.vertxpersistence.implementations.db2.DB2ConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.db2.DB2HibernateProperties;
import com.guicedee.vertxpersistence.implementations.hibernateproperties.HibernateEntityManagerProperties;
import com.guicedee.vertxpersistence.implementations.mysql.MySqlHibernateProperties;
import com.guicedee.vertxpersistence.implementations.oracle.OracleHibernateProperties;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresHibernateProperties;
import com.guicedee.vertxpersistence.implementations.sqlserver.SqlServerHibernateProperties;
import com.guicedee.vertxpersistence.implementations.systemproperties.SystemEnvironmentVariablesPropertiesReader;
import org.hibernate.service.spi.ServiceContributor;

module com.guicedee.vertxpersistence {

    exports com.guicedee.vertxpersistence;
    exports com.guicedee.vertxpersistence.annotations;
    //exports com.guicedee.vertxpersistence.bind;
    //exports com.guicedee.vertxpersistence.implementations;
    exports com.guicedee.vertxpersistence.implementations.postgres;
    exports com.guicedee.vertxpersistence.implementations.mysql;
    exports com.guicedee.vertxpersistence.implementations.db2;
    exports com.guicedee.vertxpersistence.implementations.sqlserver;
    exports com.guicedee.vertxpersistence.implementations.oracle;
    exports com.guicedee.vertxpersistence.implementations.vertxsql;

    requires transitive org.hibernate.reactive;
    requires transitive com.guicedee.vertx;
    requires transitive com.guicedee.guicedinjection;

    requires static com.guicedee.rest;

    requires org.slf4j;

    requires static lombok;
    requires transitive jakarta.transaction;

    requires transitive org.hibernate.orm.core;
    requires transitive io.vertx.sql.client;
    requires transitive io.vertx.mutiny;
    requires transitive com.guicedee.microprofile.config;

    requires transitive io.smallrye.mutiny;
    requires transitive com.ongres.scram.client;
    requires static io.vertx.sql.client.pg;

    uses com.guicedee.vertxpersistence.IPropertiesConnectionInfoReader;
    uses com.guicedee.vertxpersistence.IPropertiesEntityManagerReader;

    provides IGuiceConfigurator with GuicedConfigurator;
    provides IPropertiesEntityManagerReader with SystemEnvironmentVariablesPropertiesReader, HibernateEntityManagerProperties,
            DB2HibernateProperties,
            MySqlHibernateProperties,
            OracleHibernateProperties,
            PostgresHibernateProperties,
            SqlServerHibernateProperties;
    provides IPropertiesConnectionInfoReader with com.guicedee.vertxpersistence.implementations.hibernateproperties.HibernateDefaultConnectionBaseBuilder
            ;

    provides ServiceContributor with com.guicedee.vertxpersistence.implementations.VertxServiceContributor;

    opens com.guicedee.vertxpersistence to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.annotations to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.bind to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations.postgres to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations.mysql to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations.db2 to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations.sqlserver to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations.oracle to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.vertxpersistence.implementations.vertxsql to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;

    exports com.guicedee.vertxpersistence.implementations;
    exports com.guicedee.vertxpersistence.bind;
    exports com.guicedee.vertxpersistence.implementations.systemproperties;
}

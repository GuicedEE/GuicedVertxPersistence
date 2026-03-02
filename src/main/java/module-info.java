import com.guicedee.client.services.lifecycle.IGuiceConfigurator;
import com.guicedee.persistence.IPropertiesConnectionInfoReader;
import com.guicedee.persistence.IPropertiesEntityManagerReader;
import com.guicedee.persistence.implementations.GuicedConfigurator;
import com.guicedee.persistence.implementations.db2.DB2HibernateProperties;
import com.guicedee.persistence.implementations.hibernateproperties.HibernateEntityManagerProperties;
import com.guicedee.persistence.implementations.mysql.MySqlHibernateProperties;
import com.guicedee.persistence.implementations.oracle.OracleHibernateProperties;
import com.guicedee.persistence.implementations.postgres.PostgresHibernateProperties;
import com.guicedee.persistence.implementations.sqlserver.SqlServerHibernateProperties;
import com.guicedee.persistence.implementations.systemproperties.SystemEnvironmentVariablesPropertiesReader;
import org.hibernate.service.spi.ServiceContributor;

module com.guicedee.persistence {

    exports com.guicedee.persistence;
    exports com.guicedee.persistence.annotations;
    //exports com.guicedee.persistence.bind;
    //exports com.guicedee.persistence.implementations;
    exports com.guicedee.persistence.implementations.postgres;
    exports com.guicedee.persistence.implementations.mysql;
    exports com.guicedee.persistence.implementations.db2;
    exports com.guicedee.persistence.implementations.sqlserver;
    exports com.guicedee.persistence.implementations.oracle;
    exports com.guicedee.persistence.implementations.vertxsql;

    requires transitive org.hibernate.reactive;
    requires transitive com.guicedee.vertx;
    requires transitive com.guicedee.guicedinjection;

    requires static com.guicedee.rest;

    requires org.slf4j;

    requires static lombok;
    requires transitive jakarta.transaction;

    requires transitive org.hibernate.orm.core;
    requires static io.vertx.sql.client.pg;
    requires static io.vertx.sql.client.mssql;
    requires transitive io.vertx.sql.client;

    uses com.guicedee.persistence.IPropertiesConnectionInfoReader;
    uses com.guicedee.persistence.IPropertiesEntityManagerReader;

    provides IGuiceConfigurator with GuicedConfigurator;
    provides IPropertiesEntityManagerReader with SystemEnvironmentVariablesPropertiesReader, HibernateEntityManagerProperties,
            DB2HibernateProperties,
            MySqlHibernateProperties,
            OracleHibernateProperties,
            PostgresHibernateProperties,
            SqlServerHibernateProperties;
    provides IPropertiesConnectionInfoReader with com.guicedee.persistence.implementations.hibernateproperties.HibernateDefaultConnectionBaseBuilder
            ;

    provides ServiceContributor with com.guicedee.persistence.implementations.VertxServiceContributor;

    opens com.guicedee.persistence to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.annotations to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.bind to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations.postgres to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations.mysql to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations.db2 to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations.sqlserver to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations.oracle to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;
    opens com.guicedee.persistence.implementations.vertxsql to com.google.guice,com.guicedee.guicedinjection,io.vertx.core,io.vertx.codegen.api,org.hibernate.orm.core,net.bytebuddy,io.smallrye.mutiny;

    exports com.guicedee.persistence.implementations;
    exports com.guicedee.persistence.bind;
    exports com.guicedee.persistence.implementations.systemproperties;
}

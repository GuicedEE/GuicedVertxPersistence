package com.guicedee.persistence.test;

import com.guicedee.persistence.implementations.postgres.PostgresConnectionBaseInfo;
import com.guicedee.persistence.implementations.systemproperties.SystemEnvironmentVariablesPropertiesReader;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistencePropertiesTest {
    static class Connection extends PostgresConnectionBaseInfo {
        PgConnectOptions options() { return createConnectOptions(); }
    }
    private Connection populate(Properties properties) {
        new SystemEnvironmentVariablesPropertiesReader().processProperties(null, properties);
        var unit = mock(PersistenceUnitDescriptor.class);
        when(unit.getName()).thenReturn("fixture");
        var connection = new Connection();
        connection.populateFromProperties(unit, properties); // Real SPI discovery, including JPMS.
        return connection;
    }
    @Test void environmentResolvedJakartaSettingsReachBothHibernateAndPostgres() {
        var properties = new Properties();
        properties.setProperty("jakarta.persistence.jdbc.url", "jdbc:postgresql://${fixture.pg.host:db.example.test}:${fixture.pg.port:5544}/${fixture.pg.database:fixture}?sslmode=verify-full");
        properties.setProperty("jakarta.persistence.jdbc.user", "${fixture.pg.user:app}");
        properties.setProperty("jakarta.persistence.jdbc.password", "${fixture.pg.password:secret}");
        var connection = populate(properties);
        assertEquals("jdbc:postgresql://db.example.test:5544/fixture?sslmode=verify-full", connection.getJdbcUrl());
        assertEquals("app", connection.getUsername());assertEquals("secret", connection.getPassword());
        assertEquals("fixture", connection.getPersistenceUnitName());
        var options = connection.options();
        assertEquals("db.example.test", options.getHost());assertEquals(5544, options.getPort());
        assertEquals("fixture", options.getDatabase());assertEquals(SslMode.VERIFY_FULL, options.getSslMode());
        // Reprocessing a resolved URL must not consume its jdbc: prefix as a default expression.
        new SystemEnvironmentVariablesPropertiesReader().processProperties(null, properties);
        assertEquals(connection.getJdbcUrl(), properties.getProperty("jakarta.persistence.jdbc.url"));
    }
    @Test void literalHibernateSettingsAndApplicationOverridesRemainSupported() {
        var properties = new Properties();
        properties.setProperty("hibernate.connection.url", "jdbc:postgresql://db.example.test:5545/fixture");
        properties.setProperty("hibernate.connection.username", "legacy");
        properties.setProperty("hibernate.connection.password", "secret");
        var connection = populate(properties);
        assertEquals("jdbc:postgresql://db.example.test:5545/fixture", connection.getJdbcUrl());
        assertEquals("legacy", connection.getUsername());
        connection.setUrl("jdbc:postgresql://override:5546/other");
        connection.setUsername("override");connection.setPassword("override-secret");
        var unit = mock(PersistenceUnitDescriptor.class);
        connection.populateFromProperties(unit, properties);
        assertEquals("jdbc:postgresql://override:5546/other", connection.getJdbcUrl());
        assertEquals("override", properties.getProperty("jakarta.persistence.jdbc.user"));
        assertEquals("override-secret", properties.getProperty("jakarta.persistence.jdbc.password"));
    }
}

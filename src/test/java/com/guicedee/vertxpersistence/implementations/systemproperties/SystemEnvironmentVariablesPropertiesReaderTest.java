package com.guicedee.vertxpersistence.implementations.systemproperties.test;

import com.guicedee.vertxpersistence.implementations.systemproperties.SystemEnvironmentVariablesPropertiesReader;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link SystemEnvironmentVariablesPropertiesReader}.
 * Verifies that the reader correctly processes environment variables in properties,
 * including multiple placeholders in a single property value.
 */
public class SystemEnvironmentVariablesPropertiesReaderTest {

    private SystemEnvironmentVariablesPropertiesReader reader;
    private ParsedPersistenceXmlDescriptor nullDescriptor = null;

    @BeforeEach
    public void setUp() {
        reader = new SystemEnvironmentVariablesPropertiesReader();

        // Set system properties for testing
        System.setProperty("postgres.host", "db.example.com");
        System.setProperty("postgres.port", "5432");
        System.setProperty("postgres.database", "mydb");
    }

    @AfterEach
    public void tearDown() {
        // Clean up system properties after each test
        System.clearProperty("postgres.host");
        System.clearProperty("postgres.port");
        System.clearProperty("postgres.database");
        System.clearProperty("postgres.user");
        System.clearProperty("postgres.password");
    }

    @Test
    public void testProcessMultiplePlaceholders() {
        // Create properties with a JDBC URL containing multiple placeholders
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", 
                "jdbc:postgresql://${postgres.host:localhost}:${postgres.port:5432}/${postgres.database:testdb}");

        // Process the properties
        reader.processProperties(nullDescriptor, properties);

        // Verify that all placeholders were replaced correctly
        String expectedUrl = "jdbc:postgresql://db.example.com:5432/mydb";
        assertEquals(expectedUrl, properties.getProperty("hibernate.connection.url"),
                "Should replace all placeholders in the JDBC URL");
    }

    @Test
    public void testProcessMultiplePlaceholdersWithDefaults() {
        // Create properties with a JDBC URL containing multiple placeholders
        // Don't set system properties for these, so defaults should be used
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", 
                "jdbc:postgresql://${custom.host:localhost}:${custom.port:5433}/${custom.database:customdb}");

        // Process the properties
        reader.processProperties(nullDescriptor, properties);

        // Verify that all placeholders were replaced with default values
        String expectedUrl = "jdbc:postgresql://localhost:5433/customdb";
        assertEquals(expectedUrl, properties.getProperty("hibernate.connection.url"),
                "Should replace all placeholders with default values when system properties are not set");
    }

    @Test
    public void testProcessMixedPlaceholders() {
        // Create properties with a JDBC URL containing a mix of system properties and defaults
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", 
                "jdbc:postgresql://${postgres.host:localhost}:${custom.port:5433}/${postgres.database:testdb}");

        // Process the properties
        reader.processProperties(nullDescriptor, properties);

        // Verify that all placeholders were replaced correctly
        String expectedUrl = "jdbc:postgresql://db.example.com:5433/mydb";
        assertEquals(expectedUrl, properties.getProperty("hibernate.connection.url"),
                "Should replace placeholders with system properties when available and defaults when not");
    }

    @Test
    public void testProcessDirectFormat() {
        // Create properties with direct format
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.username", "postgres.user:defaultuser");

        // Set system property
        System.setProperty("postgres.user", "dbuser");

        // Process the properties
        reader.processProperties(nullDescriptor, properties);

        // Verify that the property was replaced correctly
        assertEquals("dbuser", properties.getProperty("hibernate.connection.username"),
                "Should replace direct format property with system property value");
    }

    @Test
    public void testProcessPlainProperty() {
        // Create properties with plain property
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.password", "postgres.password");

        // Set system property
        System.setProperty("postgres.password", "secret");

        // Process the properties
        reader.processProperties(nullDescriptor, properties);

        // Verify that the property was replaced correctly
        assertEquals("secret", properties.getProperty("hibernate.connection.password"),
                "Should replace plain property with system property value");
    }
}

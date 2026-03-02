package com.guicedee.persistence.implementations.systemproperties;

import com.google.common.base.Strings;
import com.guicedee.persistence.IPropertiesEntityManagerReader;
import lombok.extern.java.Log;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.guicedee.client.Environment.getProperty;
import static com.guicedee.client.Environment.getSystemPropertyOrEnvironment;

/**
 * A properties reader that resolves system environment variables.
 * Supports multiple formats:
 * 1. ${property:defaultValue} - Traditional format with default value
 * 2. com.property:defaultValue - Direct format with default value
 * 3. com.property - Dot notation (also tries COM_PROPERTY for Kubernetes environments)
 */
@Log
public class SystemEnvironmentVariablesPropertiesReader
        implements IPropertiesEntityManagerReader<SystemEnvironmentVariablesPropertiesReader>
{
    // Pattern for ${property:defaultValue} format
    private static final String TRADITIONAL_PATTERN = "\\$\\{([a-zA-Z0-9_\\-\\.]*)\\:?(.*)?\\}";
    private static final Pattern traditionalPattern = Pattern.compile(TRADITIONAL_PATTERN);

    // Pattern for property:defaultValue format
    private static final String DIRECT_PATTERN = "([a-zA-Z0-9_\\-\\.]+)\\:(.+)";
    private static final Pattern directPattern = Pattern.compile(DIRECT_PATTERN);

    /**
     * Resolves placeholders in incoming properties using system properties and environment variables.
     *
     * @param persistenceUnit the persistence unit descriptor
     * @param incomingProperties the properties to resolve in-place
     * @return an empty map (properties are updated in-place)
     */
    @Override
    public Map<String, String> processProperties(PersistenceUnitDescriptor persistenceUnit, Properties incomingProperties)
    {
        for (String prop : incomingProperties.stringPropertyNames())
        {
            String value = incomingProperties.getProperty(prop);
            if (value == null) {
                continue;
            }

            // Process traditional ${property:defaultValue} format
            if (value.contains("${"))
            {
                // Environment.resolvePlaceholders handles ${VAR:-default} and ${VAR}
                // It also handles recursion and multiple placeholders
                String processedValue = com.guicedee.client.Environment.resolvePlaceholders(value);
                if (!value.equals(processedValue)) {
                    incomingProperties.put(prop, processedValue);
                }
                continue;
            }

            // Process direct property:defaultValue format
            Matcher directMatcher = directPattern.matcher(value);
            if (directMatcher.matches())
            {
                processDirectFormat(incomingProperties, prop, directMatcher);
                continue;
            }

            // Process plain property (try both formats)
            processPlainProperty(incomingProperties, prop, value);
        }
        return new HashMap<>();
    }


    /**
     * Process the direct property:defaultValue format
     */
    private void processDirectFormat(Properties incomingProperties, String prop, Matcher matcher) {
        String propertyName = matcher.group(1);
        String defaultValue = matcher.group(2);

        // Try to get the property value
        String propertyValue = getSystemPropertyOrEnvironment(propertyName, defaultValue);

        if (!Strings.isNullOrEmpty(propertyValue)) {
            incomingProperties.put(prop, propertyValue);
        }
    }

    /**
     * Process a plain property name (no default value specified)
     */
    private void processPlainProperty(Properties incomingProperties, String prop, String value) {
        // If it's a plain property, try to resolve it
        if (value.matches("[a-zA-Z0-9_\\-\\.]+")) {
            String propertyValue = getSystemPropertyOrEnvironment(value, value);
            if (!Strings.isNullOrEmpty(propertyValue)) {
                incomingProperties.put(prop, propertyValue);
            }
        }
    }


    /**
     * Applies to all persistence units.
     *
     * @param persistenceUnit the persistence unit descriptor
     * @return true for all units
     */
    @Override
    public boolean applicable(PersistenceUnitDescriptor persistenceUnit)
    {
        return true;
    }

    /**
     * Runs early so environment substitutions are available to other readers.
     *
     * @return the sort order for this reader
     */
    @Override
    public Integer sortOrder()
    {
        return Integer.MIN_VALUE  + 100;
    }
}

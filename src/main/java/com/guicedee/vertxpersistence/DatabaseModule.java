package com.guicedee.vertxpersistence;

import com.google.inject.AbstractModule;
import com.google.inject.PrivateModule;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.vertxpersistence.annotations.EntityManager;
import com.guicedee.vertxpersistence.bind.JtaPersistModule;
import com.guicedee.vertxpersistence.implementations.VertxPersistenceModule;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;

/**
 * An abstract implementation for persistence.xml
 * <p>
 * Configuration conf = TransactionManagerServices.getConfiguration(); can be used to configure the transaction manager.
 */
@Slf4j
@EntityManager
public abstract class DatabaseModule<J extends DatabaseModule<J>>
        extends AbstractModule
        implements IGuiceModule<J>
{

    private static final List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors = PersistenceXmlParser.locatePersistenceUnits(Map.of());

    /**
     * Constructor DatabaseModule creates a new DatabaseModule instance.
     */
    public DatabaseModule()
    {
        //Config required
    }

    /**
     * Configures the module with the bindings
     */
    @Override
    protected void configure()
    {
        DatabaseModule.log.debug("Loading Database Module - " + getClass().getName() + " - " + getPersistenceUnitName());
        Properties jdbcProperties = getJDBCPropertiesMap();
        ParsedPersistenceXmlDescriptor pu = getPersistenceUnit();
        if (pu == null)
        {
            DatabaseModule.log
                    .error("Unable to register persistence unit with name " + getPersistenceUnitName() + " - No persistence unit containing this name was found.");
            return;
        }
        for (IPropertiesEntityManagerReader<?> entityManagerReader : IGuiceContext
                .instance()
                .getLoader(IPropertiesEntityManagerReader.class, true,
                        ServiceLoader.load(IPropertiesEntityManagerReader.class)))
        {
            if (!entityManagerReader.applicable(pu))
            {
                continue;
            }
            Map<String, String> output = entityManagerReader.processProperties(pu, jdbcProperties);
            if (output != null && !output.isEmpty())
            {
                jdbcProperties.putAll(output);
            }
        }
        try
        {
            ConnectionBaseInfo connectionBaseInfo = getConnectionBaseInfo(pu, jdbcProperties);
            connectionBaseInfo.populateFromProperties(pu, jdbcProperties);
            jdbcProperties.put("hibernate.connection.url", connectionBaseInfo.getJdbcUrl());

            if (connectionBaseInfo.getJndiName() == null)
            {
                connectionBaseInfo.setJndiName(getJndiMapping());
            }
            log.info(String.format("%s - Connection Base Info Final - %s",
                    getPersistenceUnitName(), connectionBaseInfo));
            connectionBaseInfo.setPersistenceUnitName(getPersistenceUnitName());
            var emAnnos = getClass().getAnnotationsByType(EntityManager.class);
            if (emAnnos.length > 0)
            {
                JtaPersistModule jpaModule = new JtaPersistModule(getPersistenceUnitName(), connectionBaseInfo, emAnnos[0]);
                jpaModule.properties(jdbcProperties);
                install(jpaModule);
                VertxPersistenceModule.getConnectionModules().put(connectionBaseInfo, jpaModule);
            }
            else
            {
                throw new Exception("No EntityManager annotation found on class " + getClass().getName());
            }
        }
        catch (Throwable T)
        {
            log.error("Unable to load DB Module [" + pu.getName() + "] - " + T.getMessage(), T);
        }
    }

    /**
     * The name found in persistence.xml
     *
     * @return The persistence unit name to sear h
     */
    @NotNull
    protected abstract String getPersistenceUnitName();

    /**
     * Builds up connection base data info from a persistence unit.
     * <p>
     * Use with the utility methods e.g.
     *
     * @param unit The physical persistence unit, changes have no effect the persistence ready
     * @return The new connetion base info
     */
    @NotNull
    protected abstract ConnectionBaseInfo getConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties);

    private ParsedPersistenceXmlDescriptor getPersistenceUnit()
    {
        for (ParsedPersistenceXmlDescriptor parsedPersistenceXmlDescriptor : parsedPersistenceXmlDescriptors)
        {
            if (parsedPersistenceXmlDescriptor.getName()
                    .equals(getPersistenceUnitName()))
            {
                return parsedPersistenceXmlDescriptor;
            }
        }
        return null;
    }

    /**
     * A properties map of the properties from the file
     *
     * @return A properties map of the given persistence units properties
     */
    @NotNull
    private Properties getJDBCPropertiesMap()
    {
        Properties jdbcProperties = new Properties();
        configurePersistenceUnitProperties(getPersistenceUnit(), jdbcProperties);
        return jdbcProperties;
    }

    /**
     * The name found in jta-data-source from the persistence.xml
     *
     * @return The JNDI mapping name to use
     */
    protected String getJndiMapping()
    {
        return null;
    }

    /**
     * Builds a property map from a persistence unit properties file
     * <p>
     * Overwrites ${} items with system properties
     *
     * @param pu             The persistence unit
     * @param jdbcProperties The final properties map
     */
    protected void configurePersistenceUnitProperties(ParsedPersistenceXmlDescriptor pu, Properties jdbcProperties)
    {
        if (pu != null)
        {
            try
            {
                for (Object o : pu.getProperties().keySet())
                {
                    String key = o.toString();
                    String value = pu.getProperties().get(o).toString();
                    jdbcProperties.put(key, value);
                }
            }
            catch (Throwable t)
            {
                log.error("Unable to load persistence unit properties for [" + pu.getName() + "]", t);
            }
        }
    }

    @Override
    public Integer sortOrder()
    {
        return 50;
    }
}

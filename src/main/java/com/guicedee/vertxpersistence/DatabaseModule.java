package com.guicedee.vertxpersistence;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.vertxpersistence.annotations.EntityManager;
import com.guicedee.vertxpersistence.bind.JtaPersistModule;
import com.guicedee.vertxpersistence.bind.JtaPersistService;
import com.guicedee.vertxpersistence.implementations.VertxPersistenceModule;
import io.vertx.core.Future;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

import java.util.*;

/**
 * An abstract implementation for persistence.xml
 * <p>
 * Configuration conf = TransactionManagerServices.getConfiguration(); can be used to configure the transaction manager.
 */
@Log4j2
@EntityManager
public abstract class DatabaseModule<J extends DatabaseModule<J>>
        extends AbstractModule
        implements IGuiceModule<J>,
        IGuicePostStartup<J>, IGuicePreDestroy<J> {

    private static final List<PersistenceUnitDescriptor> PersistenceUnitDescriptors = new ArrayList<>();

    /**
     * Constructor DatabaseModule creates a new DatabaseModule instance.
     */
    public DatabaseModule() {
        var parser = PersistenceXmlParser.create(Map.of(), null, null);
        var urls = parser.getClassLoaderService().locateResources("META-INF/persistence.xml");
        if (urls.isEmpty()) {
            return;
        }
        PersistenceUnitDescriptors.addAll(parser.parse(urls).values());
        for (var desc : PersistenceUnitDescriptors) {
            log.debug("📋 PU Found: {}", desc.getName());
        }
        GuiceContext.instance().loadPostStartupServices().add(this);
        GuiceContext.instance().loadPreDestroyServices().add(this);
    }

    @Override
    public List<Future<Boolean>> postLoad() {
        return List.of(getVertx().executeBlocking(() -> {
            log.info("🚀 PersistService starting");
            JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named("ActivityMaster-Test")));
            ps.start();
            log.info("✅ PersistService started successfully");
            return true;
        }));
    }

    @Override
    public void onDestroy() {
        IGuiceContext.get(Key.get(PersistService.class, Names.named("ActivityMaster-Test"))).stop();
        log.info("🛑 PersistService stopped");
    }

    /**
     * Configures the module with the bindings
     */
    @Override
    protected void configure() {
        log.debug("📋 Loading Database Module - {} - {}", getClass().getName(), getPersistenceUnitName());
        Properties jdbcProperties = getJDBCPropertiesMap();
        PersistenceUnitDescriptor pu = getPersistenceUnit();
        if (pu == null) {
            log.error("❌ Unable to register persistence unit with name {} - No persistence unit containing this name was found.", getPersistenceUnitName());
            return;
        }
        for (IPropertiesEntityManagerReader<?> entityManagerReader : IGuiceContext
                .instance()
                .getLoader(IPropertiesEntityManagerReader.class, true,
                        ServiceLoader.load(IPropertiesEntityManagerReader.class))) {
            if (!entityManagerReader.applicable(pu)) {
                continue;
            }
            Map<String, String> output = entityManagerReader.processProperties(pu, jdbcProperties);
            if (output != null && !output.isEmpty()) {
                jdbcProperties.putAll(output);
            }
        }
        try {
            ConnectionBaseInfo connectionBaseInfo = getConnectionBaseInfo(pu, jdbcProperties);
            connectionBaseInfo.populateFromProperties(pu, jdbcProperties);
            String jdbcUrl = connectionBaseInfo.getJdbcUrl();
            jdbcProperties.put("hibernate.connection.url", jdbcUrl);
            connectionBaseInfo.setUrl(jdbcUrl);

            if (connectionBaseInfo.getJndiName() == null) {
                connectionBaseInfo.setJndiName(getJndiMapping());
            }
            log.info("💾 {} - Connection Base Info Final - {}", getPersistenceUnitName(), connectionBaseInfo);
            connectionBaseInfo.setPersistenceUnitName(getPersistenceUnitName());
            var emAnnos = getClass().getAnnotationsByType(EntityManager.class);
            if (emAnnos.length > 0) {
                JtaPersistModule jpaModule = new JtaPersistModule(getPersistenceUnitName(), connectionBaseInfo, emAnnos[0]);
                jpaModule.properties(jdbcProperties);
                install(jpaModule);
                VertxPersistenceModule.getConnectionModules().put(connectionBaseInfo, jpaModule);
            } else {
                throw new Exception(String.format("No EntityManager annotation found on class %s", getClass().getName()));
            }
        } catch (Throwable T) {
            log.error("❌ Unable to load DB Module [{}] - {}", pu.getName(), T.getMessage(), T);
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
    protected abstract ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties);

    private PersistenceUnitDescriptor getPersistenceUnit() {
        for (PersistenceUnitDescriptor PersistenceUnitDescriptor : PersistenceUnitDescriptors) {
            if (PersistenceUnitDescriptor.getName()
                    .equals(getPersistenceUnitName())) {
                return PersistenceUnitDescriptor;
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
    private Properties getJDBCPropertiesMap() {
        Properties jdbcProperties = new Properties();
        configurePersistenceUnitProperties(getPersistenceUnit(), jdbcProperties);
        return jdbcProperties;
    }

    /**
     * The name found in jta-data-source from the persistence.xml
     *
     * @return The JNDI mapping name to use
     */
    protected String getJndiMapping() {
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
    protected void configurePersistenceUnitProperties(PersistenceUnitDescriptor pu, Properties jdbcProperties) {
        if (pu != null) {
            try {
                for (Object o : pu.getProperties().keySet()) {
                    String key = o.toString();
                    String value = pu.getProperties().get(o).toString();
                    jdbcProperties.put(key, value);
                }
            } catch (Throwable t) {
                log.error("❌ Unable to load persistence unit properties for [{}]", pu.getName(), t);
            }
        }
    }

    @Override
    public Integer sortOrder() {
        return 50;
    }
}

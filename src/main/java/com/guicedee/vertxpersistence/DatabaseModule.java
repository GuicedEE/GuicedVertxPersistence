package com.guicedee.vertxpersistence;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScopeSource;
import com.guicedee.client.scopes.CallScoper;
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
 * Base Guice module that wires a persistence unit defined in persistence.xml.
 * Implementations supply the persistence unit name and build a {@link ConnectionBaseInfo}
 * from its properties.
 */
@Log4j2
@EntityManager
public abstract class DatabaseModule<J extends DatabaseModule<J>>
        extends AbstractModule
        implements IGuiceModule<J>,
        IGuicePostStartup<J>, IGuicePreDestroy<J> {

    private static final List<PersistenceUnitDescriptor> PersistenceUnitDescriptors = new ArrayList<>();

    /**
     * Parses persistence.xml resources and registers lifecycle hooks.
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

    /**
     * Starts the persistence service after Guice startup.
     *
     * @return a list of futures indicating startup completion
     */
    @Override
    public List<Future<Boolean>> postLoad() {
        return List.of(getVertx().executeBlocking(() -> {
            CallScoper callScoper = IGuiceContext.get(CallScoper.class);
            boolean startedScope = callScoper.isStartedScope();
            if (!startedScope) {
                callScoper.enter();
            }
            try {
                CallScopeProperties props = IGuiceContext.get(CallScopeProperties.class);
                if (props.getSource() == null || props.getSource() == CallScopeSource.Unknown) {
                    props.setSource(CallScopeSource.Persistence);
                }
                log.info("🚀 PersistService starting");
                JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named("ActivityMaster-Test")));
                ps.start();
                log.info("✅ PersistService started successfully");
                return true;
            } finally {
                if (!startedScope) {
                    callScoper.exit();
                }
            }
        }));
    }

    /**
     * Stops the persistence service during shutdown.
     */
    @Override
    public void onDestroy() {
        IGuiceContext.get(Key.get(PersistService.class, Names.named("ActivityMaster-Test"))).stop();
        log.info("🛑 PersistService stopped");
    }

    /**
     * Configures module bindings for the persistence unit.
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
     * Returns the persistence unit name as defined in persistence.xml.
     *
     * @return the persistence unit name to load
     */
    @NotNull
    protected abstract String getPersistenceUnitName();

    /**
     * Builds connection base info from a persistence unit and properties.
     *
     * @param unit The persistence unit descriptor
     * @param filteredProperties properties derived from persistence.xml and overrides
     * @return the connection base info for this unit
     */
    @NotNull
    protected abstract ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties);

    /**
     * Finds the persistence unit descriptor matching {@link #getPersistenceUnitName()}.
     *
     * @return the descriptor or null when not found
     */
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
     * Builds a properties map for the active persistence unit.
     *
     * @return the persistence unit properties
     */
    @NotNull
    private Properties getJDBCPropertiesMap() {
        Properties jdbcProperties = new Properties();
        configurePersistenceUnitProperties(getPersistenceUnit(), jdbcProperties);
        return jdbcProperties;
    }

    /**
     * Returns the JNDI mapping name to use when not specified by the connection info.
     *
     * @return the JNDI mapping name, or null to skip
     */
    protected String getJndiMapping() {
        return null;
    }

    /**
     * Builds a property map from persistence unit properties, resolving system substitutions.
     *
     * @param pu The persistence unit
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

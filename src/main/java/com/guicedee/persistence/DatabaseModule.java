package com.guicedee.persistence;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.persistence.annotations.EntityManager;
import com.guicedee.persistence.bind.JtaPersistModule;
import com.guicedee.persistence.bind.JtaPersistService;
import com.guicedee.persistence.implementations.VertxPersistenceModule;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
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
        if (PersistenceUnitDescriptors.isEmpty()) {
            var parser = PersistenceXmlParser.create(Map.of(), null, null);
            var urls = parser.getClassLoaderService().locateResources("META-INF/persistence.xml");
            if (urls.isEmpty()) {
                return;
            }
            PersistenceUnitDescriptors.addAll(parser.parse(urls).values());
            for (var desc : PersistenceUnitDescriptors) {
                log.debug("📋 PU Found: {}", desc.getName());
            }
        }
        GuiceContext.instance().loadPostStartupServices().add(this);
        GuiceContext.instance().loadPreDestroyServices().add(this);
    }

    /**
     * Starts the persistence service after Guice startup.
     *
     * <p>Hibernate Reactive requires a valid Vert.x {@link io.vertx.core.Context} to be
     * active when the {@code EntityManagerFactory} is created, because its internal
     * reactive connection pool binds to the current context. Calling
     * {@code vertx.executeBlocking()} on the bare {@code Vertx} instance runs the
     * task on an internal worker thread with <b>no</b> associated context, which can
     * cause "No Vert.x context" errors or silent mis-binding of the pool.</p>
     *
     * <p>To avoid this we first hop onto a proper event-loop context via
     * {@code runOnContext}, then execute the blocking EMF creation from there.
     * The worker thread spawned by {@code context.executeBlocking()} inherits the
     * event-loop context, satisfying Hibernate Reactive's requirements.</p>
     *
     * @return a list of futures indicating startup completion
     */
    @Override
    public List<Uni<Boolean>> postLoad() {
        // Ensure we are on a Vert.x context before creating the EntityManagerFactory.
        // runOnContext places us on an event-loop; executeBlocking from that context
        // then runs the blocking work on a worker thread that still has a valid Context.
        JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named(getPersistenceUnitName())));
        return List.of(ps.start().replaceWith(true)
                .onFailure().invoke(t -> log.error("❌ Failed to start PersistService for PU '{}': {}", getPersistenceUnitName(), t.getMessage(), t))
        );
    }

    /**
     * Stops the persistence service during shutdown.
     */
    @Override
    public void onDestroy() {
        IGuiceContext.get(Key.get(PersistService.class, Names.named(getPersistenceUnitName()))).stop();
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
            // Pre-initialize a shared, named Vert.x SQL pool as early as possible so HR can reuse it
            // We try hard to execute this on a Vert.x event-loop, but also provide safe fallbacks
            try {
                String puName = String.valueOf(connectionBaseInfo.getPersistenceUnitName());
                var vertx = com.guicedee.vertx.spi.VertXPreStartup.getVertx();
                if (vertx != null) {
                    log.info("[DB-POOL-INIT] Scheduling pool init on Vert.x context for PU='{}'", puName);

                    // Primary: schedule on event-loop via runOnContext
                    vertx.runOnContext(v -> {
                        try {
                            log.info("[DB-POOL-INIT] runOnContext executing for PU='{}' on thread='{}'", puName, Thread.currentThread().getName());
                            io.vertx.sqlclient.SqlClient client = connectionBaseInfo.toPooledDatasource();
                            if (client != null) {
                                log.info("[DB-POOL-INIT] runOnContext pool init complete for PU='{}'", puName);
                            } else {
                                log.warn("[DB-POOL-INIT] runOnContext pool init returned null for PU='{}'", puName);
                            }
                        } catch (Throwable t) {
                            log.warn("[DB-POOL-INIT] runOnContext pool init failed for PU='{}': {}", puName, t.toString());
                        }
                    });

                    // Secondary: a zero-delay timer as a backup in case runOnContext doesn't fire under debugger
                    try {
                        vertx.setTimer(0, id -> {
                            try {
                                log.info("[DB-POOL-INIT] setTimer(0) executing for PU='{}' on thread='{}'", puName, Thread.currentThread().getName());
                                io.vertx.sqlclient.SqlClient client = connectionBaseInfo.toPooledDatasource();
                                if (client != null) {
                                    log.info("[DB-POOL-INIT] setTimer(0) pool init complete for PU='{}'", puName);
                                } else {
                                    log.warn("[DB-POOL-INIT] setTimer(0) pool init returned null for PU='{}'", puName);
                                }
                            } catch (Throwable t) {
                                log.warn("[DB-POOL-INIT] setTimer(0) pool init failed for PU='{}': {}", puName, t.toString());
                            }
                        });
                    } catch (Throwable tt) {
                        log.debug("[DB-POOL-INIT] setTimer not available, skipping");
                    }
                } else {
                    // Last resort: initialize immediately (off-context). Pool creation is safe off the event-loop,
                    // and our implementation sets shared+name so HR can still reuse it.
                    try {
                        log.info("[DB-POOL-INIT] Vert.x not ready; performing immediate fallback pool init for PU='{}' on thread='{}'", puName, Thread.currentThread().getName());
                        io.vertx.sqlclient.SqlClient client = connectionBaseInfo.toPooledDatasource();
                        if (client != null) {
                            log.info("[DB-POOL-INIT] Immediate fallback pool init complete for PU='{}'", puName);
                        } else {
                            log.warn("[DB-POOL-INIT] Immediate fallback pool init returned null for PU='{}'", puName);
                        }
                    } catch (Throwable t) {
                        log.warn("[DB-POOL-INIT] Immediate fallback pool init failed for PU='{}': {}", puName, t.toString());
                    }
                }
            } catch (Throwable t) {
                log.debug("[DB-POOL-INIT] Skipping pre-initialization due to unexpected error: {}", t.toString());
            }
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
     * @param unit               The persistence unit descriptor
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

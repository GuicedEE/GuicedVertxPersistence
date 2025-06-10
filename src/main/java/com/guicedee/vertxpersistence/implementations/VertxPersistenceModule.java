package com.guicedee.vertxpersistence.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.annotations.EntityManager;
import com.guicedee.vertxpersistence.bind.JtaPersistModule;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.PackageInfo;
import io.github.classgraph.ScanResult;
import io.vertx.sqlclient.SqlClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class VertxPersistenceModule extends AbstractModule implements IGuiceModule<VertxPersistenceModule>
{
    @Getter
    private static final Map<ConnectionBaseInfo, JtaPersistModule> connectionModules = new HashMap<>();

    /**
     * Map to store SqlClient instances by entity manager name
     */
    @Getter
    private static final Map<String, SqlClient> sqlClientMap = new HashMap<>();

    /**
     * Map to store EntityManager annotations by their value (entity manager name)
     */
    @Getter
    private static final Map<String, EntityManager> entityManagerAnnotations = new HashMap<>();

    /**
     * Map to store package names by entity manager name
     */
    @Getter
    private static final Map<String, String> packageNamesByEntityManager = new HashMap<>();

    private static final String DEFAULT_PACKAGE = "";

    @Override
    protected void configure()
    {
        ScanResult scanResult = IGuiceContext.instance().getScanResult();

        // Find all concrete classes that extend DatabaseModule and package-level EntityManager annotations
        Set<String> processedEntityManagers = new HashSet<>();
        Map<String, ConnectionBaseInfo> localConnectionInfoMap = new HashMap<>();
        ConnectionBaseInfo defaultConnectionInfo = null;
        JtaPersistModule defaultModule = null;

        // Now process all the ConnectionBaseInfo objects from the connectionModules map
        log.debug("Processing ConnectionBaseInfo objects from connectionModules map");
        for (Map.Entry<ConnectionBaseInfo, JtaPersistModule> entry : connectionModules.entrySet())
        {
            ConnectionBaseInfo connectionInfo = entry.getKey();
            JtaPersistModule module = entry.getValue();
            String emName = connectionInfo.getPersistenceUnitName();

            // Check if this entity manager name is already in use by a different module
            if (packageNamesByEntityManager.containsKey(emName))
            {
                String existingPackage = packageNamesByEntityManager.get(emName);
                String currentPackage = module.getClass().getPackage().getName();

                // If the package is different, it's a duplicate
                if (!existingPackage.equals(currentPackage))
                {
                    throw new IllegalStateException("Duplicate EntityManager annotation value '" + emName +
                            "' found in package '" + currentPackage + "'. Already defined in package '" + existingPackage + "'.");
                }
            }
            else
            {
                // Store the package name for this entity manager
                packageNamesByEntityManager.put(emName, module.getClass().getPackage().getName());
            }

            if (!processedEntityManagers.contains(emName))
            {
                processedEntityManagers.add(emName);
                localConnectionInfoMap.put(emName, connectionInfo);

                // Install the module
                install(module);

                // If this is the first module or it's marked as default, use it as the default
                if (defaultConnectionInfo == null || connectionInfo.isDefaultConnection())
                {
                    defaultConnectionInfo = connectionInfo;
                    defaultModule = module;
                }
            }
        }

        // Process package-level annotations for EntityManager
        log.debug("Processing package-level EntityManager annotations");
        for (PackageInfo packageInfo : scanResult.getPackageInfo())
        {
            AnnotationInfo annotationInfo = packageInfo.getAnnotationInfo(EntityManager.class.getName());
            if (annotationInfo != null)
            {
                String emName = getAnnotationValue(annotationInfo);
                boolean isDefault = getAnnotationDefaultEm(annotationInfo);
                log.info("Found package-level EntityManager annotation: " + packageInfo.getName() + " with value: " + emName);

                // Check if this entity manager name is already in use
                if (!entityManagerAnnotations.containsKey(emName))
                {
                    throw new IllegalStateException("No Database Module EntityManager annotation value '" + emName +
                            "' found in package info'" + packageInfo.getName() + "'. Please define a database module with the EntityManager annotation.");
                }

                // Store the EntityManager annotation and package name
                EntityManager emAnno = new EntityManager()
                {
                    @Override
                    public Class<? extends Annotation> annotationType()
                    {
                        return EntityManager.class;
                    }

                    @Override
                    public String value()
                    {
                        return emName;
                    }

                    @Override
                    public boolean allClasses()
                    {
                        Object allClasses = annotationInfo.getParameterValues().getValue("allClasses");
                        return allClasses != null ? Boolean.parseBoolean(allClasses.toString()) : true;
                    }

                    @Override
                    public boolean defaultEm()
                    {
                        return isDefault;
                    }
                };

                entityManagerAnnotations.put(emName, emAnno);
                packageNamesByEntityManager.put(emName, packageInfo.getName());
            }
        }

        // If no EntityManager annotations or DatabaseModules were found, use the default package
        if (processedEntityManagers.isEmpty())
        {
            throw new IllegalStateException("No EntityManager annotations or DatabaseModules found, using default package");
        }

        // Ensure we have a default entity manager
        if (defaultConnectionInfo == null && !processedEntityManagers.isEmpty())
        {
            // Use the first one as default if none was marked as default
            String firstEmName = processedEntityManagers.iterator().next();

            // Check if we already have a ConnectionBaseInfo for this entity manager
            ConnectionBaseInfo connectionInfo = localConnectionInfoMap.get(firstEmName);
            if (connectionInfo == null)
            {
                // If not, check if it exists in the static map
                connectionInfo = getConnectionInfoByEntityManager(firstEmName);
                if (connectionInfo == null)
                {
                    // If still not found, create a new one
                    connectionInfo = createConnectionInfo(firstEmName);
                }
                localConnectionInfoMap.put(firstEmName, connectionInfo);
            }

            // Get the EntityManager annotation for this entity manager
            EntityManager emAnno = entityManagerAnnotations.get(firstEmName);
            if (emAnno == null)
            {
                // If no EntityManager annotation exists, create a default one
                emAnno = new EntityManager()
                {
                    @Override
                    public Class<? extends Annotation> annotationType()
                    {
                        return EntityManager.class;
                    }

                    @Override
                    public String value()
                    {
                        return firstEmName;
                    }

                    @Override
                    public boolean allClasses()
                    {
                        return true;
                    }

                    @Override
                    public boolean defaultEm()
                    {
                        return true;
                    }
                };

                entityManagerAnnotations.put(firstEmName, emAnno);
                packageNamesByEntityManager.put(firstEmName, "default");
            }

            defaultModule = createAndBindModule(connectionInfo, emAnno);
            defaultConnectionInfo = connectionInfo;
        }

        // Bind the default EntityManager for @Inject @EntityManager EntityManager
        if (defaultModule != null)
        {
            defaultConnectionInfo.setDefaultConnection(true);
            bind(jakarta.persistence.EntityManager.class)
                    .to(Key.get(jakarta.persistence.EntityManager.class, Names.named(defaultConnectionInfo.getPersistenceUnitName())));

            // Bind the default UnitOfWork for @Inject @EntityManager UnitOfWork
            bind(UnitOfWork.class)
                    .to(Key.get(UnitOfWork.class, Names.named(defaultConnectionInfo.getPersistenceUnitName())));
        }
        bind(jakarta.persistence.EntityManager.class)
                .annotatedWith(EntityManager.class)
                .to(Key.get(jakarta.persistence.EntityManager.class, Names.named(defaultConnectionInfo.getPersistenceUnitName())));

        // Bind the default UnitOfWork for @Inject @EntityManager UnitOfWork
        bind(UnitOfWork.class)
                .annotatedWith(EntityManager.class)
                .to(Key.get(UnitOfWork.class, Names.named(defaultConnectionInfo.getPersistenceUnitName())));
    }

    private String getAnnotationValue(AnnotationInfo annotationInfo)
    {
        Object value = annotationInfo.getParameterValues().getValue("value");
        return value != null ? value.toString() : DEFAULT_PACKAGE;
    }

    private boolean getAnnotationDefaultEm(AnnotationInfo annotationInfo)
    {
        Object defaultEm = annotationInfo.getParameterValues().getValue("defaultEm");
        return defaultEm != null ? Boolean.parseBoolean(defaultEm.toString()) : true;
    }

    /**
     * Retrieves a ConnectionBaseInfo for the specified persistence unit name.
     * This method checks if a ConnectionBaseInfo with the given name already exists,
     * and if so, returns it. If not found, it throws an exception as the ConnectionBaseInfo
     * should have been created during the module configuration.
     *
     * @param persistenceUnitName The name of the persistence unit
     * @return The ConnectionBaseInfo for the specified persistence unit
     * @throws IllegalStateException if no ConnectionBaseInfo exists for the given persistence unit name
     */
    private ConnectionBaseInfo createConnectionInfo(String persistenceUnitName)
    {
        // Check if we already have a ConnectionBaseInfo for this persistence unit
        ConnectionBaseInfo connectionInfo = getConnectionInfoByEntityManager(persistenceUnitName);
        if (connectionInfo == null)
        {
            throw new IllegalStateException("No ConnectionBaseInfo found for persistence unit: " + persistenceUnitName +
                    ". ConnectionBaseInfo should be created during module configuration.");
        }

        // Ensure SqlClient is populated in the map if not already
        if (!sqlClientMap.containsKey(persistenceUnitName))
        {
            SqlClient sqlClient = connectionInfo.toPooledDatasource();
            if (sqlClient != null)
            {
                sqlClientMap.put(persistenceUnitName, sqlClient);
            }
        }

        return connectionInfo;
    }

    /**
     * Creates and binds a JtaPersistModule for the given ConnectionBaseInfo and EntityManager annotation.
     *
     * @param connectionInfo The ConnectionBaseInfo to use
     * @param emAnno         The EntityManager annotation to use
     * @return The created JtaPersistModule
     */
    private JtaPersistModule createAndBindModule(ConnectionBaseInfo connectionInfo, EntityManager emAnno)
    {
        JtaPersistModule module = new JtaPersistModule(
                connectionInfo.getPersistenceUnitName(),
                connectionInfo,
                emAnno);

        install(module);
        connectionModules.put(connectionInfo, module);

        return module;
    }

    /**
     * Creates and binds a JtaPersistModule for the given ConnectionBaseInfo.
     * This method looks up the EntityManager annotation from the entityManagerAnnotations map.
     * If no EntityManager annotation is found, it creates a default one.
     *
     * @param connectionInfo The ConnectionBaseInfo to use
     * @return The created JtaPersistModule
     */
    private JtaPersistModule createAndBindModule(ConnectionBaseInfo connectionInfo)
    {
        String persistenceUnitName = connectionInfo.getPersistenceUnitName();
        EntityManager emAnno = entityManagerAnnotations.get(persistenceUnitName);

        if (emAnno == null)
        {
            // Create a default EntityManager annotation
            emAnno = new EntityManager()
            {
                @Override
                public Class<? extends Annotation> annotationType()
                {
                    return EntityManager.class;
                }

                @Override
                public String value()
                {
                    return persistenceUnitName;
                }

                @Override
                public boolean allClasses()
                {
                    return true;
                }

                @Override
                public boolean defaultEm()
                {
                    return persistenceUnitName.equals(DEFAULT_PACKAGE);
                }
            };

            // Store the annotation for future use
            entityManagerAnnotations.put(persistenceUnitName, emAnno);
        }

        return createAndBindModule(connectionInfo, emAnno);
    }

    /**
     * Looks up a ConnectionBaseInfo by the entity manager name.
     * This method allows easy retrieval of the ConnectionBaseInfo associated with a specific @EntityManager annotation.
     *
     * @param entityManagerName The name of the entity manager to look up (value from the @EntityManager annotation)
     * @return The ConnectionBaseInfo for the specified entity manager, or null if not found
     */
    public static ConnectionBaseInfo getConnectionInfoByEntityManager(String entityManagerName)
    {
        if (entityManagerName == null)
        {
            entityManagerName = DEFAULT_PACKAGE;
        }

        for (ConnectionBaseInfo connectionInfo : connectionModules.keySet())
        {
            if (entityManagerName.equals(connectionInfo.getPersistenceUnitName()))
            {
                return connectionInfo;
            }
        }

        return null;
    }

    /**
     * Gets the default ConnectionBaseInfo.
     * This is a convenience method to retrieve the ConnectionBaseInfo for the default entity manager.
     *
     * @return The default ConnectionBaseInfo, or null if not found
     */
    public static ConnectionBaseInfo getDefaultConnectionInfo()
    {
        return getConnectionInfoByEntityManager(DEFAULT_PACKAGE);
    }

    /**
     * Gets the JtaPersistModule associated with the specified entity manager name.
     * This method allows easy retrieval of the JtaPersistModule associated with a specific @EntityManager annotation.
     *
     * @param entityManagerName The name of the entity manager to look up (value from the @EntityManager annotation)
     * @return The JtaPersistModule for the specified entity manager, or null if not found
     */
    public static JtaPersistModule getPersistModuleByEntityManager(String entityManagerName)
    {
        ConnectionBaseInfo connectionInfo = getConnectionInfoByEntityManager(entityManagerName);
        if (connectionInfo != null)
        {
            return connectionModules.get(connectionInfo);
        }
        return null;
    }

    /**
     * Gets the default JtaPersistModule.
     * This is a convenience method to retrieve the JtaPersistModule for the default entity manager.
     *
     * @return The default JtaPersistModule, or null if not found
     */
    public static JtaPersistModule getDefaultPersistModule()
    {
        return getPersistModuleByEntityManager(DEFAULT_PACKAGE);
    }

    /**
     * Gets the SqlClient associated with the specified entity manager name.
     * This method allows easy retrieval of the SqlClient associated with a specific @EntityManager annotation.
     *
     * @param entityManagerName The name of the entity manager to look up (value from the @EntityManager annotation)
     * @return The SqlClient for the specified entity manager, or null if not found
     */
    public static SqlClient getSqlClientByEntityManager(String entityManagerName)
    {
        if (entityManagerName == null)
        {
            entityManagerName = DEFAULT_PACKAGE;
        }

        return sqlClientMap.get(entityManagerName);
    }

    /**
     * Gets the default SqlClient.
     * This is a convenience method to retrieve the SqlClient for the default entity manager.
     *
     * @return The default SqlClient, or null if not found
     */
    public static SqlClient getDefaultSqlClient()
    {
        return getSqlClientByEntityManager(DEFAULT_PACKAGE);
    }

    @Override
    public Integer sortOrder()
    {
        return Integer.MAX_VALUE - 250;
    }
}

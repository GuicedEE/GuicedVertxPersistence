# C4 Level 3 — Configuration & Bootstrap Components

This bounded context captures how persistence units defined in `persistence.xml` are processed, wired into GuicedEE, and turned into reactive session factories.

```mermaid
C4Component
    Container(persistence, "GuicedEE Persistence Library", "Java 25")
    Component(databaseModule, "DatabaseModule<...>", "GuicedEE module", "Reads `persistence.xml`, merges JDBC/Hibernate properties via readers, and installs `JtaPersistModule` for each `@EntityManager`-annotated subclass.")
    Component(connectionInfoReader, "IPropertiesConnectionInfoReader implementations", "ServiceLoader", "Populates `ConnectionBaseInfo` from filtered persistence properties and environment overrides.")
    Component(entityManagerReader, "IPropertiesEntityManagerReader implementations", "ServiceLoader", "Transforms `PersistenceUnitDescriptor` details into the final property map before persistence initialization.")
    Component(connectionInfo, "ConnectionBaseInfo / CleanConnectionBaseInfo", "Data holder", "Encapsulates JDBC URL, credentials, pool sizing, XA/reactive flags, and exposes `toPooledDatasource()` for Vert.x.")
    Component(jtaPersistModule, "JtaPersistModule", "Guice module", "Ties `ConnectionBaseInfo` to a persistence unit name and installs transaction-aware bindings inside `DatabaseModule`." )
    Component(jtaPersistService, "JtaPersistService", "Lifecycle service", "Creates and stops `EntityManagerFactory` instances; unwraps Hibernate Reactive `Mutiny.SessionFactory` for reactive callers.")
    Component(mutinyProvider, "MutinySessionFactoryProvider", "Provider", "Supplies `Mutiny.SessionFactory` from `JtaPersistService` for Guiced components.")
    Component(vertxPersistenceModule, "VertxPersistenceModule", "Registry", "Tracks configured `ConnectionBaseInfo` → `JtaPersistModule` pairs and contributes Vert.x services (via `VertxServiceContributor`).")
    Component(guicedConfigurator, "GuicedConfigurator", "IGuiceConfigurator", "Enables GuicedEE scanning features like annotation/field/method visibility required by persistence modules.")
    Component(serviceContributor, "VertxServiceContributor", "ServiceContributor", "Registers Hibernate services accessible to GuicedEE and Vert.x when the module is loaded.")

    Rel(databaseModule, entityManagerReader, "enriches properties")
    Rel(databaseModule, connectionInfoReader, "populates `ConnectionBaseInfo`")
    Rel(databaseModule, connectionInfo, "configures persistence unit metadata")
    Rel(databaseModule, jtaPersistModule, "installs with resolved connection info")
    Rel(databaseModule, vertxPersistenceModule, "registers module + connection info")
    Rel(jtaPersistModule, jtaPersistService, "creates `EntityManagerFactory` on startup")
    Rel(mutinyProvider, jtaPersistService, "unwraps `Mutiny.SessionFactory`")
    Rel(jtaPersistModule, vertxPersistenceModule, "hooks into Vert.x service discovery")
    Rel(vertxPersistenceModule, serviceContributor, "exposes Hibernate service contributions")
    Rel(guicedConfigurator, databaseModule, "applies GuicedEE configuration rules")
```

This component diagram ensures that the configuration, property enrichment, and lifecycle transitions are documented and accessible for follow-on guides and implementation plans.

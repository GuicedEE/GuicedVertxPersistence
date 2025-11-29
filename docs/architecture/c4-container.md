# C4 Level 2 — Container View

The persistence library breaks down into configuration helpers, runtime bridges, and reactive service providers. The following diagram summarizes the containers inside `com.guicedee.vertxpersistence` and how they interact with external runtimes.

```mermaid
C4Container
    Container(persistence, "GuicedEE Persistence Library", "Java 25, Lombok, Log4j2", "Bootstraps persistence units, configures transactions, and exposes reactive session factories for GuicedEE modules.")

    ContainerDb(configLayer, "Configuration & Connection Info", "Java", "`ConnectionBaseInfo`, `CleanConnectionBaseInfo`, and property readers (`IPropertiesConnectionInfoReader`, `IPropertiesEntityManagerReader`) that shape JDBC/Hibernate properties before bootstrapping persistence units.")
    ContainerDb(runtimeLayer, "Persistence Runtime", "Guice modules + Hibernate", "`DatabaseModule`, `JtaPersistModule`, and `JtaPersistService` that read `persistence.xml`, apply binding hooks, and start/stop Hibernate entity manager factories.")
    ContainerDb(reactiveLayer, "Reactive Session Provider", "Mutiny + Vertx", "`MutinySessionFactoryProvider`, `VertxPersistenceModule`, and service contributors that unwrap `Mutiny.SessionFactory` and integrate with Vert.x services (e.g., `VertxServiceContributor`).")

    System_Ext(guicedee, "GuicedEE Runtime", "Handles dependency injection and lifecycle hooks.")
    System_Ext(hibernate, "Hibernate Reactive 7", "Provides reactive `Mutiny.SessionFactory` backed by JDBC drivers.")
    System_Ext(vertx, "Vert.x 5 / Mutiny", "Event-loop + SQL client used by reactive connection builders.")
    System_Ext(config, "MicroProfile / Jakarta Config", "Property sources referenced by persistence descriptors.")
    System_Ext(databases, "External RDBMS", "Postgres, MySQL, Oracle, DB2, SQL Server, etc.")

    Rel(persistence, configLayer, "builds `ConnectionBaseInfo` and merges property readers from service discovery")
    Rel(persistence, runtimeLayer, "loads `DatabaseModule`, registers `JtaPersistService`, and wires `EntityManager` metadata")
    Rel(persistence, reactiveLayer, "exposes providers for `Mutiny.SessionFactory` and integrates with Vert.x services")
    Rel(configLayer, config, "reads property sources, supplements `persistence.xml`")
    Rel(runtimeLayer, guicedee, "registers Guice modules, IGuiceModule hooks, and lifecycle callbacks")
    Rel(reactiveLayer, vertx, "returns Vert.x-ready session factories and contributors")
    Rel(reactiveLayer, hibernate, "unwraps `Mutiny.SessionFactory` from Hibernate Reactive")
    Rel(runtimeLayer, databases, "passes JDBC URLs and credentials")
```

Any implementation following this container map must keep these explicit responsibilities: configuration shaping, runtime bootstrapping, and reactive exposure.

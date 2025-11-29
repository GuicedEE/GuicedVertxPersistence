# C4 Level 1 — System Context

The GuicedEE Persistence library (`com.guicedee.vertxpersistence`) is a lightweight, Guice-friendly bridge that binds persistence units defined in `persistence.xml` to Hibernate Reactive 7, Mutiny, and Vert.x 5. The diagram below captures the system boundary and the primary actors/tech stacks we depend on.

```mermaid
C4Context
    Person(dev, "GuicedEE Library Author", "Defines persistence modules, configures persistence units, and consumes reactive session factories.")
    System_Boundary(persistence, "GuicedEE Persistence Library (com.guicedee.vertxpersistence)") {
        System_Ext(guicedee, "GuicedEE Injection + Lifecycle", "Boots modules, runs post-startup/pre-destroy hooks, and wires IGuiceModule implementations.")
        System_Ext(hibernate, "Hibernate Reactive 7", "Creates `Mutiny.SessionFactory` instances backed by asynchronous drivers.")
        System_Ext(vertx, "Vert.x 5 + Mutiny", "Provides the event-loop, reactive SQL client, and Vert.x persistence modules for reactive execution.")
        System_Ext(config, "MicroProfile/Jakarta Config & Environment", "Supplies property sources referenced from `persistence.xml`, environment variables, and custom readers.")
        System_Ext(rdbms, "External RDBMS (PostgreSQL, MySQL, Oracle, DB2, SQL Server)", "Hosts the physical databases the persistence units target.")
    }
    Rel(dev, persistence, "configures modules, property readers, and consumes Mutiny.SessionFactory")
    Rel(persistence, guicedee, "registers `IGuiceModule`, `ILifecycle` callbacks, and `IGuiceConfigurator`")
    Rel(persistence, hibernate, "builds `Mutiny.SessionFactory` via `JtaPersistService`")
    Rel(persistence, vertx, "exposes Vert.x-friendly connection info and session factories")
    Rel(persistence, config, "applies JDBC and Hibernate properties via readers")
    Rel(persistence, rdbms, "drives reactive connections and transactions")
```

## Trust Boundaries and Threat Summary
- **Config inputs:** Treat MicroProfile/Jakarta Config and env vars as untrusted; sanitize values before merging into `ConnectionBaseInfo` and avoid logging secrets (Log4j2 markers only).  
- **Reactive surface:** `Mutiny.SessionFactory` crosses into Vert.x callers; ensure providers are read-only and enforce CRTP builders to avoid leaking mutable connection state.  
- **Database edge:** TLS enforcement and driver provenance (use `com.guicedee.services` artifacts) protect the RDBMS boundary; reject non-approved drivers.  
- **ServiceLoader plugins:** `IProperties*Reader` implementations must validate prefixes and fail closed when encountering unknown properties.

This context diagram anchors every downstream artifact (rules, guides, implementation) so that the modular docs consistently reference the same stack and responsibilities.

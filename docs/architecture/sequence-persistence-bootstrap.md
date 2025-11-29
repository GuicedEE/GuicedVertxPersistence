# Sequence — Persistence Module Bootstrap

This flow tracks how GuicedEE loads `DatabaseModule`, enriches persistence settings, and starts the underlying `Mutiny.SessionFactory` via `JtaPersistService`.

```mermaid
sequenceDiagram
    participant Dev as GuicedEE Developer
    participant Guice as GuicedEE Injector
    participant DbModule as DatabaseModule
    participant EntityReader as IPropertiesEntityManagerReader
    participant ConnReader as IPropertiesConnectionInfoReader
    participant ConnInfo as ConnectionBaseInfo
    participant JtaModule as JtaPersistModule
    participant VertxRegistry as VertxPersistenceModule
    participant PersistSvc as JtaPersistService
    participant Mutiny as Mutiny.SessionFactory

    Dev->>Guice: install new DatabaseModule subclass
    Guice->>DbModule: configure()
    DbModule->>EntityReader: load persistence overrides
    EntityReader-->>DbModule: enriched properties
    DbModule->>ConnReader: populate ConnectionBaseInfo
    ConnReader-->>ConnInfo: fills URL, credentials, pool sizing
    DbModule->>JtaModule: build with @EntityManager info
    DbModule->>VertxRegistry: register ConnectionBaseInfo + module
    DbModule->>PersistSvc: instantiate via JtaPersistModule
    PersistSvc->>JtaModule: start() -> create EntityManagerFactory
    PersistSvc->>Mutiny: unwrap SessionFactory
    VertxRegistry->>Mutiny: expose provider for application modules
```

The sequence ensures each reader and module participates in the bootstrapping handshake before the reactive session factory becomes available to applications.

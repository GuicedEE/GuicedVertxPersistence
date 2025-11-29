# Integration & Trust Boundaries — GuicedEE Persistence

This artifact captures the dependency map, trust boundaries, and threat considerations for `com.guicedee.vertxpersistence` as it bridges GuicedEE, Hibernate Reactive 7, Vert.x 5, and external databases.

## Dependency / Integration Map
```mermaid
flowchart TB
    subgraph trusted["Trusted runtime (GuicedEE JVM)"]
        DBModule[DatabaseModule subclasses]
        ConnReaders[IPropertiesConnectionInfoReader\n(ServiceLoader)]
        EntityReaders[IPropertiesEntityManagerReader\n(ServiceLoader)]
        JtaModule[JtaPersistModule]
        JtaService[JtaPersistService]
        MutinyProvider[MutinySessionFactoryProvider]
        VertxRegistry[VertxPersistenceModule\n+ VertxServiceContributor]
    end

    subgraph config["Config boundary"]
        ConfigSources[MicroProfile/Jakarta Config\nEnv Vars, props, secrets managers]
    end

    subgraph reactive["Reactive boundary"]
        Vertx[Vert.x 5 + Mutiny]
        Hibernate[Hibernate Reactive 7]
    end

    subgraph data["Data stores (untrusted)"]
        Rdbms[External RDBMS\n(Postgres/MySQL/Oracle/DB2/SQL Server)]
    end

    DBModule --> ConnReaders
    DBModule --> EntityReaders
    DBModule --> JtaModule
    JtaModule --> JtaService
    JtaService --> Hibernate
    JtaService --> MutinyProvider
    MutinyProvider --> Vertx
    VertxRegistry --> Vertx
    ConnReaders --> ConfigSources
    EntityReaders --> ConfigSources
    Hibernate --> Rdbms
    Vertx --> Rdbms
```

## Trust Boundaries & Threat Notes
- **Config boundary:** Property readers pull from environment and MicroProfile/Jakarta Config. Validate prefixes and strip sensitive values from logs (Log4j2 with structured markers) to avoid leaking credentials across trust lines.
- **Reactive boundary:** `Mutiny.SessionFactory` is exposed to Vert.x callers; enforce CRTP-based builders to prevent accidental mutable sharing. Constrain `MutinySessionFactoryProvider` to return immutable references.
- **Data boundary:** External RDBMS is untrusted; ensure connection info enforces TLS flags and JDBC drivers from `com.guicedee.services` artifacts per JPMS notes.
- **ServiceLoader inputs:** `IProperties*Reader` implementations are pluggable; reject unknown prefixes and sanitize property maps before applying them to `JtaPersistModule`.
- **Lifecycle:** `JtaPersistService` start/stop hooks must be idempotent and guard against double-start to avoid leaking connections across module reloads.

## How to use this map
- Reference this file from RULES/GUIDES when describing onboarding steps, secrets handling, or database support.
- Cross-link to `docs/architecture/c4-container.md` for container responsibilities and to `docs/architecture/sequence-persistence-bootstrap.md` for the temporal order of these boundaries.

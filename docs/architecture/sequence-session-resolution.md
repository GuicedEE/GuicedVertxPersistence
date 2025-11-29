# Sequence — Reactive Session Factory Resolution

Once the persistence module is bootstrapped, application services request a `Mutiny.SessionFactory`. This sequence shows how Guice, `MutinySessionFactoryProvider`, and `JtaPersistService` coordinate the reactive factory lookup.

```mermaid
sequenceDiagram
    participant App as Application Service
    participant Guice as GuicedEE Injector
    participant MutinyProvider as MutinySessionFactoryProvider
    participant PersistSvc as JtaPersistService
    participant EMFactory as EntityManagerFactory
    participant Mutiny as Mutiny.SessionFactory

    App->>Guice: request `Mutiny.SessionFactory`
    Guice->>MutinyProvider: obtain provider
    MutinyProvider->>PersistSvc: get()
    PersistSvc->>PersistSvc: ensure EntityManagerFactory started
    PersistSvc->>EMFactory: create if missing
    EMFactory->>Mutiny: unwrap Mutiny.SessionFactory
    MutinyProvider-->>Guice: return Mutiny.SessionFactory
    Guice-->>App: deliver reactive session factory
```

This sequence highlights the lazy-start semantics in `JtaPersistService` and the Guiced-enabled provider path for downstream services.

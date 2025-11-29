# Implementation — GuicedEE Persistence Library

This document maps the current code layout to the rules and guides (closing loops) and links to the architecture diagrams that describe each flow.

## 1. Code layout
- `src/main/java/com/guicedee/vertxpersistence/` — core boundary, including `DatabaseModule`, `ConnectionBaseInfo` family, and the `annotations`/`bind` packages. Reference `docs/architecture/c4-container.md` for the container responsibilities and `docs/architecture/c4-component-configuration.md` for the detailed ties between modules, readers, and lifecycle services.  
- `src/main/java/com/guicedee/vertxpersistence/bind/` — `JtaPersistModule`, `JtaPersistService`, `MutinySessionFactoryProvider`, and `PersistModule` deliver the GuicedEE/Hibernate bootstrap runtime (`rules/generative/backend/guicedee/functions/guiced-vertx-persistence-rules.md`).  
- `src/main/java/com/guicedee/vertxpersistence/implementations/` — concrete `IProperties*Reader` implementations per database (Postgres, MySQL, SQL Server, Oracle, DB2) along with property builders (`ConnectionBaseInfoFactory`, `ConnectionBaseInfoBuilder`). They must honor CRTP chaining explained in `rules/generative/backend/fluent-api/crtp.rules.md`.
- `src/main/resources/META-INF/` — service loader descriptors (`com.guicedee.vertxpersistence.IPropertiesEntityManagerReader`, etc.) and test persistence units (`persistence.xml` under `src/test/resources`). Keep these descriptors aligned with `GLOSSARY.md` terms (`ConnectionBaseInfo`, `PersistenceUnit`).

## 2. Flow references
- Startup: `DatabaseModule` uses `IPropertiesEntityManagerReader` + `IPropertiesConnectionInfoReader` before installing `JtaPersistModule`; see `docs/architecture/sequence-persistence-bootstrap.md` and the `GUIDES.md` section on bootstrapping.  
- Runtime: `MutinySessionFactoryProvider` obtains `Mutiny.SessionFactory` via `JtaPersistService`; the runtime sequence lives in `docs/architecture/sequence-session-resolution.md`.  
- Configuration: `ConnectionBaseInfo` readers and `CleanConnectionBaseInfo` forms the configuration data model described in `docs/architecture/erd-connection-domain.md` and the project glossary entries.

## 3. Linking to RULES and GUIDES
- RULES: follow logging, stack, and lifecycle requirements from `RULES.md` and the modular rules index at `rules/generative/backend/guicedee/persistence/README.md`.  
- GUIDES: refer to `GUIDES.md` when onboarding new persistence units, defining reactive flows, or documenting the CRTP/logging standards; examples should cite the architecture diagrams.  
- Diagrams: keep referencing `docs/architecture/README.md` for each new artifact to ensure traceability.

## 4. Forward Work
- Stage 3 plan is documented in `docs/implementation-plan.md`; execute Stage 4 tasks (README, release notes, tests) in line with that plan.

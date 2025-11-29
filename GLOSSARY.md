# Glossary — GuicedEE Persistence Library

This glossary follows the topic-first precedence policy: any term defined in a topic-scoped glossary (e.g., GuicedEE, Hibernate, Lombok, Fluent API) takes priority over the host glossary. The host file acts as an index and clarifier for project-specific terminology while pointing to the authoritative topic glossaries.

## Glossary Precedence Policy
1. **Topic glossaries win.** When a term lives under `rules/generative/*/GLOSSARY.md`, use that definition verbatim and reference the topic file.  
2. **Root glossary references the topic.** The host glossary points to the topic file and only duplicates terms when project-specific context adds nuance.  
3. **Forward-only updates.** When a term changes, update only the current host glossary entry and/or the referenced topic file; do not resurrect removed definitions.

## Topic Glossary Index
- **GuicedEE Persistence (this library):** `rules/generative/backend/guicedee/persistence/GLOSSARY.md` (authoritative, topic-first glossary with LLM interpretation guidance for persistence flows).  
- **GuicedEE:** `rules/generative/backend/guicedee/GLOSSARY.md` (includes lifecycle, modules, and connectors).  
- **Hibernate Reactive 7:** `rules/generative/backend/hibernate/GLOSSARY.md` (describes session factories, transactions, and bootstrapping).  
- **Lombok & Logging:** `rules/generative/backend/lombok/GLOSSARY.md` plus `rules/generative/backend/logging/README.md`/`LOGGING_RULES.md` for `@Log4j2` guidance.  
- **CRTP Fluent APIs:** `rules/generative/backend/fluent-api/crtp.rules.md` defines the required fluent setters and explicit `(J)this` chaining we adopt across persistence helpers.
- **Java 25 Tooling:** `rules/generative/language/java/java-25.rules.md` (language requirements) and `rules/generative/language/java/build-tooling.md` (Maven integration).

## Project-Specific Terms
### `ConnectionBaseInfo` / `CleanConnectionBaseInfo` (`src/main/java/com/guicedee/vertxpersistence/ConnectionBaseInfo.java`, `CleanConnectionBaseInfo.java`)  
Contains JDBC URL, credentials, pool sizing, XA/reactive flags, and the `toPooledDatasource()` contract. Project code treats `CleanConnectionBaseInfo` as a lighter-weight specialization of `ConnectionBaseInfo` while still relying on `ConnectionBaseInfo` readers defined under `rules/generative/backend/guicedee/functions/guiced-vertx-persistence-rules.md`.

### `DatabaseModule` (`src/main/java/com/guicedee/vertxpersistence/DatabaseModule.java`)  
Abstract GuicedEE module that loads `persistence.xml`, applies `IPropertiesEntityManagerReader` and `IPropertiesConnectionInfoReader` extensions, and installs a typed `JtaPersistModule` per `@EntityManager` annotation. Follow the GuicedEE lifecycle guidance in `rules/generative/backend/guicedee/README.md` and ensure Log4j2 logging tags (`@Log4j2`) remain consistent.

### `JtaPersistService` / `MutinySessionFactoryProvider` (`bind/JtaPersistService.java`, `bind/MutinySessionFactoryProvider.java`)  
`JtaPersistService` lazily creates an `EntityManagerFactory`, unwraps it to `Mutiny.SessionFactory` (per `rules/generative/backend/hibernate/GLOSSARY.md`), and exposes thread-safe start/stop hooks. `MutinySessionFactoryProvider` is the Guice `Provider` that feeds reactive callers via GuicedEE injection, so treat `Mutiny.SessionFactory` as the canonical reactive session contract.

### `IPropertiesEntityManagerReader` / `IPropertiesConnectionInfoReader`  
ServiceLoader-driven hooks that post-process `PersistenceUnitDescriptor` metadata (`rules/generative/backend/guicedee/functions/guiced-vertx-persistence-rules.md`). Project-level implementations live under `src/main/java/com/guicedee/vertxpersistence/implementations/` and must respect the CRTP fluent setters defined in `rules/generative/backend/fluent-api/crtp.rules.md` when exposing configuration builders.

### `GuicedConfigurator` (`implementations/GuicedConfigurator.java`)  
`IGuiceConfigurator` that enables the annotation, field, and method scanning heuristics required by the persistence stack; follow the default Log4j2 logging policy from `rules/generative/backend/logging/README.md`.

### CRTP Fluent API  
We favor the CRTP pattern described in `rules/generative/backend/fluent-api/crtp.rules.md`. Subclasses expose fluent setters returning `(J)this` and use `@SuppressWarnings("unchecked")` where needed. Do **not** mix Lombok `@Builder` in these persistence helpers.

### Java 25 + Maven  
All code and documentation target Java 25 (`rules/generative/language/java/java-25.rules.md`). Build references should cite Maven artifact coordinates only, deferring plugin wiring to `rules/generative/language/java/build-tooling.md`.

# Guides — Applying the Persistence Rules

Use these steps with the modular rules in `rules/generative/backend/guicedee/persistence/`.

## 1) Bootstrapping Persistence Modules
1. Follow `docs/architecture/sequence-persistence-bootstrap.md` and `rules/generative/backend/guicedee/persistence/bootstrapping.rules.md` for ordering: property readers → connection enrichment → `JtaPersistModule` install → `JtaPersistService.start()`.
2. Extend `DatabaseModule` subclasses with `@EntityManager`; override `getPersistenceUnitName()` and `getConnectionBaseInfo(...)`. Keep CRTP setters (`rules/generative/backend/fluent-api/crtp.rules.md`) and avoid `@Builder`.
3. Register `IPropertiesEntityManagerReader` and `IPropertiesConnectionInfoReader` implementations under `src/main/java/com/guicedee/vertxpersistence/implementations/`; validate prefixes and redact secrets per `rules/generative/backend/guicedee/persistence/configuration.rules.md`.
4. Use `VertxPersistenceModule` to map `ConnectionBaseInfo → JtaPersistModule` and expose Hibernate services (`rules/generative/backend/guicedee/vertx/README.md`). Align trust boundaries with `docs/architecture/integration-trust-boundaries.md`.

## 2) Reactive Session Resolution
1. Applications request `Mutiny.SessionFactory` through Guice; consult `docs/architecture/sequence-session-resolution.md` and `rules/generative/backend/guicedee/persistence/reactive-session.rules.md`.
2. Ensure `JtaPersistService` logging follows Log4j2 guidance (`rules/generative/backend/logging/README.md`) and retains emoji markers without leaking secrets.
3. Apply Hibernate Reactive 7 transaction boundaries (`rules/generative/backend/hibernate/README.md`) and keep Vert.x calls non-blocking; offload blocking work to worker pools.

## 3) Logging, Glossary, and Naming
- Annotate runtime modules/providers with Lombok `@Log4j2`; use structured markers for boundary crossings noted in `docs/architecture/integration-trust-boundaries.md`.
- Resolve terminology via `rules/generative/backend/guicedee/persistence/GLOSSARY.md` (topic-first) and the host `GLOSSARY.md` for project-specific nuances.
- Keep persistence unit and provider names consistent with `persistence.xml` descriptors to avoid mismatched bindings.

## 4) CI, Secrets, and Examples
1. Align GitHub Actions with `rules/generative/backend/guicedee/persistence/ci-cd-and-secrets.rules.md` and `rules/generative/platform/ci-cd/providers/github-actions.md`; pin Java 25 toolchains.
2. Document env vars in `.env.example` per `rules/generative/platform/secrets-config/env-variables.md`; never log credentials.
3. Add example snippets showing `Mutiny.SessionFactory` injection and a simple transaction; gate merges on ServiceLoader registration tests and bootstrap smoke tests.

## 5) Traceability
- Keep PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION linked. Update `docs/PROMPT_REFERENCE.md` when stacks or diagrams change.

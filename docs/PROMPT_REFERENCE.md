# Prompt Reference — GuicedEE Persistence

This document records the decisions, stacks, and architecture links we must carry forward through every stage of the Rules Repository workflow. Stage approvals are **blanket-approved** per the current prompt; note this in summaries when moving between stages.

## Selected stacks & tooling
- **Java LTS:** Java 25 (per prompt). Build metadata resides in `pom.xml`, and all new artifacts must align with the Java 25 rules (`rules/generative/language/java/java-25.rules.md`) plus the shared build-tooling guide (`rules/generative/language/java/build-tooling.md`).
- **Frameworks:** GuicedEE Core + Persistence (GuicedEE injection, Vert.x 5, and Hibernate Reactive 7). Reference `rules/generative/backend/guicedee/README.md`, `rules/generative/backend/guicedee/vertx/README.md`, `rules/generative/backend/guicedee/functions/guiced-vertx-persistence-rules.md`, and `rules/generative/backend/hibernate/README.md` when drafting rules/guides.
- **Logging:** Log4j2 + Lombok (`@Log4j2`). Follow `rules/generative/backend/logging/README.md` and `rules/generative/backend/logging/LOGGING_RULES.md` for concrete guidance.
- **Dependency management:** Maven/GuicedEE library. Use artifact-coordinates only when requested; rely on the shared build-tooling guide for plugin wiring.
- **Architecture & documentation:** All diagrams live under `docs/architecture/` and are linked from `docs/architecture/README.md`.
- **Library metadata:** Guiced Vert.x Persistence `2.0.0-SNAPSHOT`, repo `https://github.com/GuicedEE/GuicedVertxPersistence.git`, Service/Framework type, CRTP fluent API, CI via GitHub Actions.

## Glossary plan
- Compose `GLOSSARY.md` topic-first, referencing topic glossaries like `rules/generative/backend/guicedee/GLOSSARY.md`, `rules/generative/backend/hibernate/GLOSSARY.md`, `rules/generative/backend/lombok/GLOSSARY.md`, and `rules/generative/backend/fluent-api/GLOSSARY.md` (CRTP). 
- Document the Glossary Precedence Policy inside the host `GLOSSARY.md`: topic-level glossaries override root definitions and the root file serves as an index.
- Only duplicate terms where project-specific context demands it (e.g., `ConnectionBaseInfo`, `Mutiny.SessionFactory`). For prompt language alignment (CRTP vs Builder) rely on `rules/generative/backend/fluent-api/crtp.rules.md`.

## Architecture diagrams (referenced by all artifacts)
- `docs/architecture/c4-context.md` — system context showing GuicedEE, Hibernate Reactive, Vert.x, MicroProfile Config, and external databases.
- `docs/architecture/c4-container.md` — container view (configuration, runtime, and reactive provider layers + interactions with GuicedEE/Hibernate/Vert.x).
- `docs/architecture/c4-component-configuration.md` — component view inside the configuration/binding bounded context.
- `docs/architecture/integration-trust-boundaries.md` — integration map across GuicedEE, Hibernate Reactive, Vert.x, config providers, and RDBMS plus threat/trust boundaries.
- `docs/architecture/sequence-persistence-bootstrap.md` — bootstrapping flow for `DatabaseModule`, property readers, and `JtaPersistService`.
- `docs/architecture/sequence-session-resolution.md` — runtime path when application services request `Mutiny.SessionFactory`.
- `docs/architecture/erd-connection-domain.md` — ERD describing connection metadata, persistence units, and property readers.

## Stage gate traceability
- Stage 1 artifacts: `PACT.md`, `docs/architecture/*`, `docs/PROMPT_REFERENCE.md`. Stage 1 is auto-approved because the prompt grants blanket approval; note this in future stage summaries so reviewers know the gate was skipped by policy.
- Future stages must reference these docs when drafting RULES/GUIDES/IMPLEMENTATION, linking back to the diagrams and glossary plan to satisfy traceability requirements.

## Migration notes
- Treat all `rules/` documentation as the source of truth for prompts. Derived host docs (PACT, architecture) must reference or link to the rules submodule content rather than copying it.
- Document any risky changes in `MIGRATION.md` once we stabilize the architecture-first artifacts.

# Rules — GuicedEE Persistence Adoption

## 1. Scope & Purpose
- This module (`com.guicedee.vertxpersistence`) is a GuicedEE-friendly persistence helper that bridges `persistence.xml` descriptors with Hibernate Reactive 7 and Vert.x 5 services.  
- Document-first work must reference the diagrams under `docs/architecture/` (C4, sequences, ERD) and keep the forward-only traceability chain: `PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION`.
- Topic rules live under `rules/generative/backend/guicedee/persistence/` (index + modular rules for configuration, bootstrapping, reactive sessions, CI/secrets, glossary).

## 2. Selected Stacks & References
- **Java 25 LTS:** `rules/generative/language/java/java-25.rules.md` (language expectations) and `rules/generative/language/java/build-tooling.md` (Maven wiring).  
- **GuicedEE Core/Persistence:** `rules/generative/backend/guicedee/README.md`, `rules/generative/backend/guicedee/vertx/README.md`, `rules/generative/backend/guicedee/functions/guiced-vertx-persistence-rules.md`, and `rules/generative/backend/guicedee/inject/README.md`.  
- **Hibernate Reactive 7:** `rules/generative/backend/hibernate/README.md` plus topic guides for transaction handling, session factories, and the reactive overview.  
- **Logging:** Default to Log4j2 (`rules/generative/backend/logging/README.md`, `rules/generative/backend/logging/LOGGING_RULES.md`), annotate classes with Lombok `@Log4j2`, and align log messages with existing emoji-rich style.  
- **Fluent API strategy:** CRTP per `rules/generative/backend/fluent-api/crtp.rules.md`; do not mix Lombok `@Builder`.  
- **CRTP & builders:** `ConnectionBaseInfo` and related builders must keep `(J)this` chaining semantics described in the CRTP guide.  
- **GuicedEE lifecycle & modules:** follow `rules/generative/backend/guicedee/vertx/publishers.rules.md`, `rules/generative/backend/guicedee/web/lifecycle.rules.md`, and `rules/generative/backend/guicedee/services/services.md` for hooking persistence modules into the injector and service lifecycle.

## 3. Naming, Structure, and Terminology
- Reference the host `GLOSSARY.md` when naming classes, configuration properties, or logs. Where a topic glossary already defines terms (e.g., `Mutiny.SessionFactory`, `ConnectionBaseInfo`), defer to that definition and link back to it.
- Keep `persistence.xml` descriptors and related property readers under `src/main/resources/META-INF/`. Names should match the persistence unit names used by tests (`ActivityMaster-Test`, etc.).
- Logging statements should remain consistent with the iconography seen in `DatabaseModule`/`JtaPersistService` (emojis for state transitions) while still aligning with Log4j2 best practices.

## 4. Documentation & Diagrams
- Always cite the relevant architecture artifact when describing flow or module boundaries: e.g., `docs/architecture/sequence-persistence-bootstrap.md` for startup sequences, `docs/architecture/c4-container.md` for container boundaries, `docs/architecture/erd-connection-domain.md` for domain relationships.  
- Trust boundaries and dependency map are documented in `docs/architecture/integration-trust-boundaries.md`; use these when noting secrets handling or external database edges.  
- Update `docs/PROMPT_REFERENCE.md` whenever stack or diagram responsibilities change.  
- Close loops by referencing RULES in GUIDES, GUIDES in IMPLEMENTATION, and linking back to PACT + architecture diagrams.

## 5. CI & Tooling Plans
- Prepare for GitHub Actions referencing `rules/generative/platform/ci-cd/README.md` and `rules/generative/platform/ci-cd/providers/github-actions.md`.  
- Keep secrets documentation aligned with `rules/generative/platform/secrets-config/env-variables.md` and `rules/generative/platform/secrets-config/README.md`.  
- Document `.env.example` variables as per the secrets-config guide before Stage 4 scaffolding.

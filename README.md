# GuicedEE Vert.x Persistence — Rules & Docs

Guice-friendly persistence helper for Vert.x 5 and Hibernate Reactive 7 (Java 25). Version: `2.0.0-SNAPSHOT`. Architecture and prompt references are maintained in `docs/`.

## How to Use These Rules
- Start with the topic rules index: `rules/generative/backend/guicedee/persistence/README.md` (configuration, bootstrapping, reactive session, CI/secrets, glossary).
- Follow `GUIDES.md` for step-by-step bootstrapping and reactive session usage; cross-check architecture diagrams in `docs/architecture/README.md`.
- Keep the traceability loop intact: `PACT.md` ↔ `GLOSSARY.md` ↔ `RULES.md` ↔ `GUIDES.md` ↔ `IMPLEMENTATION.md` (and `docs/implementation-plan.md` for Stage 3).
- Apply CI guidance from `rules/generative/backend/guicedee/persistence/ci-cd-and-secrets.rules.md` and platform rules under `rules/generative/platform/`.

## Prompt Language Alignment & Glossary
- Authoritative glossary for this topic: `rules/generative/backend/guicedee/persistence/GLOSSARY.md` (topic-first). Host projects should link to it and copy only enforced aligned names; avoid duplicating other terms.
- The host `GLOSSARY.md` indexes topic glossaries and captures project-specific nuances; use topic terms verbatim when prompting (e.g., `DatabaseModule`, `MutinySessionFactoryProvider`, `ConnectionBaseInfo`).

## Stage Gates & Approvals
- Documentation-first workflow (Stages 1–4). This run has blanket approval; stages are still recorded in summaries and `docs/implementation-plan.md`.

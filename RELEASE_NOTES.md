# Release Notes — GuicedEE Vert.x Persistence (Rules Update)

Version: 2.0.0-SNAPSHOT  
Scope: Forward-only documentation and rules reorganization (no source changes). Stage approvals: blanket-approved per prompt.

## Changes
- Added integration/trust-boundary map and refreshed architecture references in `docs/architecture/` plus updated `docs/PROMPT_REFERENCE.md`.
- Created modular ruleset under `rules/generative/backend/guicedee/persistence/` (configuration, bootstrapping, reactive session, CI/secrets, topic glossary).
- Updated GUIDES, RULES, IMPLEMENTATION, and GLOSSARY to close loops and reference modular rules/diagrams; added Stage 3 implementation plan.
- Added root `README.md`, AI assistant/copilot workspace rules, and documented CI/testing expectations.

## Next Steps
- Run markdown link checks and Maven tests on Java 25 once integration tests are added.
- Align any GitHub Actions workflow with the CI guidance and ensure `.env.example` documents required vars.

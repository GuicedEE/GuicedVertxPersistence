# Implementation Plan — GuicedEE Vert.x Persistence (Stage 3)

This plan precedes implementation (Stage 4) and maps the remaining work for rules/guides and validation.

## File & Module Plan
- Rules: finalize modular files under `rules/generative/backend/guicedee/persistence/` (configuration, bootstrapping, reactive-session, ci-cd-and-secrets, glossary) and keep `README.md` as the parent index.
- Guides: align `GUIDES.md` with the new modular structure and reference architecture diagrams plus RULES anchors.
- README: add a root `README.md` with usage, prompt language alignment, and glossary links; keep architecture and rules references prominent.
- Release docs: add `RELEASE_NOTES.md` summarizing forward-only changes and stage approvals; ensure `docs/PROMPT_REFERENCE.md` and `PACT.md` remain consistent.

## CI/Publishing Plan
- Add/validate GitHub Actions guidance in RULES and ensure workflows pin Java 25, run link checks for `docs/` + `rules/`, and run integration tests for persistence bootstrapping.
- Publish SNAPSHOT artifacts only after Stage 4 deliverables pass validation; include steps in `RELEASE_NOTES.md`.

## Rollout & Risks
- Rollout: update RULES/GUIDES first, then README and release notes; avoid touching source code until documentation is settled.
- Risks: ServiceLoader registration drift (mitigate with tests), outdated diagram links (mitigate via link check), and secrets leakage (mitigate via Log4j2 redaction guidance).

## Validation Approach
- Manual review: verify all RULES/GUIDES reference `docs/architecture/*` and `docs/PROMPT_REFERENCE.md`.
- Automated: run markdown link checks and Maven tests (including integration tests once added) on Java 25.
- Traceability: confirm each artifact closes the loop PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION.

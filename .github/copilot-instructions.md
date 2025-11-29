# Copilot Instructions — GuicedEE Persistence

- Obey `RULES.md` sections 4/5 plus Document Modularity and Forward-Only policies; keep RULES/GUIDES modular and linked to architecture diagrams under `docs/architecture/`.
- Logging default is Log4j2 with Lombok `@Log4j2`; do not emit secrets. Use CRTP fluent setters (no `@Builder`) for configuration helpers.
- Stage gates are documentation-first (Stages 1–4). This run has blanket approval; note auto-approvals in responses but maintain stage sequencing.
- Keep references to selected stacks: Java 25, Maven, GuicedEE Core/Persistence, Hibernate Reactive 7, Vert.x 5, GitHub Actions CI.
- Close loops: PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION; cite diagrams like `docs/architecture/sequence-persistence-bootstrap.md` when describing flows.

# AI Assistant Rules (GuicedEE Persistence)

Pinned summary for AI Assistant. Honor these constraints on every action:
- Follow `RULES.md` sections 4 (Documentation & Diagrams) and 5 (CI & Tooling Plans). Always cite architecture artifacts from `docs/architecture/` when describing flows.
- Document Modularity Policy: prefer small, topic-scoped RULES/GUIDES (kebab-case files) and keep cross-links between PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION.
- Forward-Only Change Policy: no backfills or resurrecting removed docs; replace monoliths with modular files and record breaking changes in `RELEASE_NOTES.md`.
- Logging default: Log4j2 with Lombok `@Log4j2`; redact secrets and keep existing emoji-style markers.
- Stage gates: documentation-first workflow with stages 1–4; approvals are blanket-approved for this run but note the auto-approval in summaries.

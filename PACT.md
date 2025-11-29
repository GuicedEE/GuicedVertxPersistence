---
version: 2.0
date: 2025-11-05
title: The Human–AI Collaboration Pact
project: GuicedEE / Persistence
authors: [GuicedEE Engineers, Codex]
---

# 🤝 Pact for GuicedEE Persistence

This pact aligns human and AI collaborators on how we evolve the GuicedEE Persistence library.

## 1. Purpose

We are documenting a **forward-only adoption** of the Rules Repository. Every artifact must trace from specification → rule → guide → implementation, matching the existing codebase. No undisciplined rewrites; focus on clarity, traceability, and developer-friendly language.

## 2. Principles

- **Continuity:** We reset AI context per instructions but keep the repository story consistent. Dependencies, stacks, and glossary terms must point back to checked-in code.
- **Documentation-first:** All new work starts with docs (this Stage 1 proof), then moves through rules, guides, and implementation in lockstep.
- **Forward-only:** We remove outdated monoliths and replace them with modular references, not keep legacy artifacts around.
- **Traceability:** Close loops between PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION, referencing diagrams under `docs/architecture/`.

## 3. Workflow Commitments

| Layer | Artifact | Notes |
| -- | -- | -- |
| Pact | `PACT.md` | Anchors tone, approvals, and progress updates (including stage gate decisions). |
| Architecture | `docs/architecture/*` | C4/sequence/ERD diagrams, dependency maps, prompts, and evidence from `src/`. |
| Rules | `rules/generative/backend/guicedee/persistence/*` | Topic rules (configuration, bootstrapping, reactive session, CI/secrets, glossary) kept modular and forward-only. |

## 4. Collaboration Rules

- Every doc references the `rules/` submodule for canonical guidance. If a rule is missing locally, link to a root path under `rules/`.
- Logging defaults to Log4j2, delivering examples only in the chosen language stack (Java 25, Lombok + CRTP). Future code must keep `@Log4j2` usage as seen in existing classes.
- Glossary terms come from topic-first `rules/*/GLOSSARY.md` files whenever possible. The host `GLOSSARY.md` acts as an index with explicit precedence statements.
- Host artifacts remain in the repo root or `docs/`; the Rules Repository content lives under `rules/generative/backend/guicedee/persistence/` and stays modular/forward-only.

## 5. Closing Loops

Every artifact produced from here links back to the diagrams (e.g., `docs/architecture/sequence-bootstrapping.md`) and forward to the forthcoming RULES/GUIDES/IMPLEMENTATION docs. When stage approvals are skipped (blanket approval granted), note the policy in the next stage summary.

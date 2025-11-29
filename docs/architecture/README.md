# Architecture Index

This directory centralizes the architecture-first artifacts required by the Rules Repository staging workflow. Each file is a text-based diagram or artifact that explains the current `com.guicedee.vertxpersistence` implementation.

| Artifact | Purpose |
| --- | --- |
| [c4-context](c4-context.md) | C4 Level 1 context showing the library boundary, external systems (GuicedEE, Hibernate Reactive, Vert.x, configuration sources, data stores), and the developer persona. |
| [c4-container](c4-container.md) | Container diagram that highlights configuration helpers, persistence runtime, and reactive session providers plus external integrations. |
| [c4-component-configuration](c4-component-configuration.md) | C4 Level 3 breakdown of configuration, property reader, and lifecycle components such as `DatabaseModule`, `ConnectionBaseInfo`, `JtaPersistService`, and `MutinySessionFactoryProvider`. |
| [integration-trust-boundaries](integration-trust-boundaries.md) | Dependency and trust-boundary map covering GuicedEE, Hibernate Reactive, Vert.x, configuration providers, and databases with threat considerations. |
| [sequence-persistence-bootstrap](sequence-persistence-bootstrap.md) | Sequence for bootstrapping persistence units, running property readers, and starting `Mutiny.SessionFactory`. |
| [sequence-session-resolution](sequence-session-resolution.md) | Sequence describing how application services request `Mutiny.SessionFactory` through the GuicedEE provider layer. |
| [erd-connection-domain](erd-connection-domain.md) | ERD depicting `ConnectionBaseInfo`, `CleanConnectionBaseInfo`, persistence descriptors, and property readers. |

All diagrams are linked by `docs/PROMPT_REFERENCE.md` so the rest of the prompt-driven workflow can find and reuse them in RULES/GUIDES/IMPLEMENTATION artifacts.

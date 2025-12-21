# 🗄️ GuicedEE Vert.x Persistence

[![JDK](https://img.shields.io/badge/JDK-25%2B-0A7?logo=java)](https://openjdk.org/projects/jdk/25/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Vert.x](https://img.shields.io/badge/Vert.x-5-4B9)](https://vertx.io/)
[![Hibernate Reactive](https://img.shields.io/badge/Hibernate-Reactive-59666C)](https://hibernate.org/reactive/)

Guice-friendly persistence helpers for Vert.x 5 and Hibernate Reactive 7 (Java 25). Version: `2.0.0-SNAPSHOT`. Provides DI modules, session factory provisioning, and testing guidance for reactive JPA workloads.

## ✨ Features
- Mutiny-based Hibernate Reactive integration with Vert.x 5
- Guice modules for `Mutiny.SessionFactory` provisioning
- Testcontainers-friendly patterns and CI guidance
- JPMS/SPI-ready composition

## 📦 Install (Maven)
```
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>guiced-persistence</artifactId>
</dependency>
```

## 🚀 Quick Start (Reactive Session)
```
// Obtain a SessionFactory via Guice (see rules/generative/backend/guicedee/persistence)
// Key pattern: compose chains, avoid blocking, pass Session through transactions

sessionFactory.withSession(session ->
  session.withTransaction(tx ->
    new MyEntity().builder(session)
      .persist(new MyEntity().setName("Example"))
      .chain(() -> new MyEntity().builder(session)
                                 .where("name", com.entityassist.enumerations.Operand.Equals, "Example")
                                 .setMaxResults(1)
                                 .getAll())
  )
).invoke(list -> log.info("Inserted and fetched {} entity(ies)", list.size()));
```

## ⚙️ Configuration
- Supply DB URL/credentials via environment or `.env` aligned to `rules/generative/platform/secrets-config/env-variables.md`.
- Customize connection info using `ConnectionBaseInfo` in your Guice `DatabaseModule`.
- Keep secrets in CI secret stores; never commit `.env`.

## 🧪 Testing & CI
- Use Testcontainers for local integration tests (see rules and guides).
- CI runs `mvn -B verify` with required secrets injected as environment variables.

## 📚 How to Use These Rules
- Start with `rules/generative/backend/guicedee/persistence/README.md` (configuration, bootstrapping, reactive session, CI/secrets, glossary).
- Follow `GUIDES.md` for step-by-step setup; cross-check diagrams in `docs/architecture/README.md`.
- Keep the traceability loop intact: `PACT.md` ↔ `GLOSSARY.md` ↔ `RULES.md` ↔ `GUIDES.md` ↔ `IMPLEMENTATION.md`.

## 🧩 JPMS & SPI
- JPMS-ready; services and providers registered via ServiceLoader.

## 📝 License & Contributions
- License: Apache 2.0
- PRs welcome; keep docs updated when changing provisioning or configuration.

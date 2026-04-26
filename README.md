# GuicedEE Persistence

[![Build](https://github.com/GuicedEE/GuicedVertxPersistence/actions/workflows/build.yml/badge.svg)](https://github.com/GuicedEE/GuicedVertxPersistence/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.guicedee/persistence)](https://central.sonatype.com/artifact/com.guicedee/persistence)
[![Maven Snapshot](https://img.shields.io/nexus/s/com.guicedee/persistence?server=https%3A%2F%2Foss.sonatype.org&label=Maven%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/com/guicedee/persistence/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)

![Java 25+](https://img.shields.io/badge/Java-25%2B-green)
![Guice 7](https://img.shields.io/badge/Guice-7%2B-green)
![Vert.X 5](https://img.shields.io/badge/Vert.x-5%2B-green)
![Maven 4](https://img.shields.io/badge/Maven-4%2B-green)

Reactive **JPA persistence** for [GuicedEE](https://github.com/GuicedEE) applications using **Hibernate Reactive 7** and **Vert.x 5**.
Extend `DatabaseModule`, point it at a `persistence.xml` unit, and the module wires a `Mutiny.SessionFactory` into Guice — fully reactive, annotation-driven, with built-in support for PostgreSQL, MySQL, SQL Server, Oracle, and DB2.

Built on [Hibernate Reactive](https://hibernate.org/reactive/) · [Vert.x SQL Client](https://vertx.io/docs/vertx-sql-client/java/) · [Google Guice](https://github.com/google/guice) · [Mutiny](https://smallrye.io/smallrye-mutiny/) · JPMS module `com.guicedee.persistence` · Java 25+

## 📦 Installation

```xml
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>persistence</artifactId>
</dependency>
```

<details>
<summary>Gradle (Kotlin DSL)</summary>

```kotlin
implementation("com.guicedee:persistence:2.0.0-RC10")
```
</details>

## ✨ Features

- **Annotation-driven persistence units** — extend `DatabaseModule`, annotate with `@EntityManager`, and Guice wires everything from `persistence.xml`
- **Hibernate Reactive + Mutiny** — `Mutiny.SessionFactory` is bound in Guice with full reactive session/transaction support
- **Multi-database support** — built-in `ConnectionBaseInfo` implementations for PostgreSQL, MySQL, SQL Server, Oracle, and DB2
- **Environment variable resolution** — `${VAR_NAME}` placeholders in `persistence.xml` properties are resolved from system properties or environment variables
- **Vert.x SQL Client pooling** — pre-initialized shared connection pools on the Vert.x event loop for optimal Hibernate Reactive integration
- **Multiple persistence units** — bind multiple `DatabaseModule` subclasses with distinct `@Named` qualifiers; one is marked as the default
- **`@EntityManager` scoping** — annotate packages or classes to associate entities with specific persistence units
- **SPI-driven property processing** — `IPropertiesEntityManagerReader` and `IPropertiesConnectionInfoReader` contribute database-specific Hibernate settings
- **Vert.x context-aware startup** — `EntityManagerFactory` creation runs on a proper Vert.x context to satisfy Hibernate Reactive's internal requirements
- **Lifecycle management** — `PersistService.start()` / `stop()` integrated with `IGuicePostStartup` / `IGuicePreDestroy`

## 🚀 Quick Start

**Step 1** — Add a `persistence.xml`:

```xml
<!-- src/main/resources/META-INF/persistence.xml -->
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
  <persistence-unit name="mydb">
    <provider>org.hibernate.reactive.provider.ReactivePersistenceProvider</provider>
    <class>com.example.entities.User</class>
    <properties>
      <property name="jakarta.persistence.jdbc.url"
                value="${DB_URL:jdbc:postgresql://localhost:5432/mydb}"/>
      <property name="jakarta.persistence.jdbc.user" value="${DB_USER:postgres}"/>
      <property name="jakarta.persistence.jdbc.password" value="${DB_PASSWORD:secret}"/>
      <property name="hibernate.hbm2ddl.auto" value="update"/>
    </properties>
  </persistence-unit>
</persistence>
```

**Step 2** — Create a `DatabaseModule` subclass:

```java
public class MyDatabaseModule extends DatabaseModule<MyDatabaseModule> {

    @Override
    protected String getPersistenceUnitName() {
        return "mydb";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(
            PersistenceUnitDescriptor unit, Properties properties) {
        return ConnectionBaseInfoFactory.createConnectionBaseInfo("postgresql");
    }
}
```

**Step 3** — Register via JPMS:

```java
module my.app {
    requires com.guicedee.persistence;

    provides com.guicedee.client.services.lifecycle.IGuiceModule
        with my.app.MyDatabaseModule;
}
```

**Step 4** — Use the reactive session factory:

```java
public class UserService {

    @Inject
    private Mutiny.SessionFactory sessionFactory;

    public Uni<User> createUser(String name) {
        User user = new User();
        user.setName(name);
        return sessionFactory.withTransaction(session ->
            session.persist(user).replaceWith(user)
        );
    }

    public Uni<User> findUser(Long id) {
        return sessionFactory.withSession(session ->
            session.find(User.class, id)
        );
    }
}
```

## 📐 Architecture

```
Startup
  IGuiceContext.instance()
   └─ IGuiceConfigurator hooks
       └─ GuicedConfigurator             (enables annotation, field, method scanning)
   └─ IGuiceModule hooks
       └─ MyDatabaseModule (extends DatabaseModule)
           ├─ Parse persistence.xml       (PersistenceXmlParser)
           ├─ IPropertiesEntityManagerReader SPIs
           │   ├─ SystemEnvironmentVariablesPropertiesReader (${VAR} resolution)
           │   ├─ HibernateEntityManagerProperties
           │   └─ PostgresHibernateProperties / MySql... / Oracle... / SqlServer... / DB2...
           ├─ IPropertiesConnectionInfoReader SPIs
           │   └─ HibernateDefaultConnectionBaseBuilder (maps jakarta.persistence.jdbc.*)
           ├─ ConnectionBaseInfo.populateFromProperties()
           ├─ ConnectionBaseInfo.toPooledDatasource()  (Vert.x SQL pool init)
           └─ JtaPersistModule
               ├─ bind PersistService @Named("mydb")
               └─ bind Mutiny.SessionFactory @Named("mydb") + default
       └─ VertxPersistenceModule
           ├─ Validate @EntityManager annotations
           ├─ Process package-level @EntityManager
           └─ Bind default EntityManager / SessionFactory
   └─ IGuicePostStartup hooks
       └─ DatabaseModule.postLoad()
           └─ PersistService.start()       (creates EntityManagerFactory on Vert.x context)
   └─ IGuicePreDestroy hooks
       └─ DatabaseModule.onDestroy()
           └─ PersistService.stop()        (closes SessionFactory + EntityManagerFactory)
```

### Hibernate Reactive integration

```
Hibernate Reactive
 └─ ServiceContributor SPI
     └─ VertxServiceContributor
         └─ Registers VertxInstance backed by VertXPreStartup.getVertx()
             → Hibernate Reactive uses the shared Vert.x instance for all reactive IO
```

## 🗃️ Database Module

### Extending `DatabaseModule`

Every persistence unit is represented by a `DatabaseModule` subclass annotated with `@EntityManager`:

```java
public class OrdersDatabaseModule extends DatabaseModule<OrdersDatabaseModule> {

    @Override
    protected String getPersistenceUnitName() {
        return "orders";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(
            PersistenceUnitDescriptor unit, Properties properties) {
        return ConnectionBaseInfoFactory.createConnectionBaseInfo("postgresql");
    }

    @Override
    public Integer sortOrder() {
        return 50; // controls startup ordering
    }
}
```

### `@EntityManager` annotation

| Attribute | Default | Purpose |
|---|---|---|
| `value` | `""` | Persistence unit name (maps to `persistence.xml`) |
| `allClasses` | `true` | Include all entity classes or only the annotated package |
| `defaultEm` | `true` | Mark as the default `SessionFactory` binding |

Apply at **class level** (on `DatabaseModule` subclasses) or **package level** (`package-info.java`) to scope entities to specific persistence units.

## 🔌 Connection Configuration

### `ConnectionBaseInfo`

Abstract base class carrying all JDBC connection properties. Database-specific subclasses provide the `toPooledDatasource()` method that creates a Vert.x `SqlClient` pool.

### `ConnectionBaseInfoFactory`

Creates the correct `ConnectionBaseInfo` for a given database type:

```java
// By database name
ConnectionBaseInfo cbi = ConnectionBaseInfoFactory.createConnectionBaseInfo("postgresql");

// By JDBC URL (auto-detects database type)
ConnectionBaseInfo cbi = ConnectionBaseInfoFactory.createConnectionBaseInfoFromJdbcUrl(
    "jdbc:postgresql://localhost:5432/mydb");
```

### Supported databases

| Database | Type string | `ConnectionBaseInfo` class | Hibernate properties |
|---|---|---|---|
| PostgreSQL | `postgresql`, `postgres` | `PostgresConnectionBaseInfo` | `PostgresHibernateProperties` |
| MySQL / MariaDB | `mysql`, `mariadb` | `MySqlConnectionBaseInfo` | `MySqlHibernateProperties` |
| SQL Server | `sqlserver`, `mssql` | `SqlServerConnectionBaseInfo` | `SqlServerHibernateProperties` |
| Oracle | `oracle` | `OracleConnectionBaseInfo` | `OracleHibernateProperties` |
| DB2 | `db2` | `DB2ConnectionBaseInfo` | `DB2HibernateProperties` |

### Connection properties

| Property | Default | Purpose |
|---|---|---|
| `serverName` | — | Database server hostname |
| `port` | varies | Database server port |
| `databaseName` | — | Database / schema name |
| `username` | — | Authentication username |
| `password` | — | Authentication password |
| `minPoolSize` | `1` | Minimum connection pool size |
| `maxPoolSize` | `5` | Maximum connection pool size |
| `maxIdleTime` | — | Idle connection timeout (seconds) |
| `maxLifeTime` | — | Maximum connection lifetime (seconds) |
| `reactive` | `true` | Use Hibernate Reactive (vs. blocking) |
| `defaultConnection` | `true` | Register as the default binding |

## ⚙️ Configuration

### `persistence.xml` properties

Standard JPA/Jakarta persistence properties are supported:

| Property | Purpose |
|---|---|
| `jakarta.persistence.jdbc.url` | JDBC connection URL |
| `jakarta.persistence.jdbc.user` | Database username |
| `jakarta.persistence.jdbc.password` | Database password |
| `jakarta.persistence.jdbc.driver` | JDBC driver class |
| `hibernate.hbm2ddl.auto` | Schema management (`update`, `validate`, `create`, `create-drop`) |
| `hibernate.dialect` | Hibernate dialect (auto-set by database-specific readers) |

### Environment variable resolution

All `persistence.xml` property values support `${VAR_NAME}` and `${VAR_NAME:default}` syntax:

```xml
<property name="jakarta.persistence.jdbc.url"
          value="${DB_URL:jdbc:postgresql://localhost:5432/mydb}"/>
<property name="jakarta.persistence.jdbc.user"
          value="${DB_USER:postgres}"/>
<property name="jakarta.persistence.jdbc.password"
          value="${DB_PASSWORD}"/>
```

The `SystemEnvironmentVariablesPropertiesReader` resolves placeholders in this order:
1. System property (`-DDB_URL=...`)
2. Environment variable (`export DB_URL=...`)
3. Default value (after the `:` separator)

Kubernetes-friendly: dot-notation properties (e.g., `db.url`) are also tried as uppercase underscored (`DB_URL`).

## 🔌 SPI Extension Points

All SPIs are discovered via `ServiceLoader`. Register implementations with JPMS `provides...with` or `META-INF/services`.

### `IPropertiesEntityManagerReader`

Contributes database-specific Hibernate properties for a persistence unit:

```java
public class MyCustomProperties
        implements IPropertiesEntityManagerReader<MyCustomProperties> {

    @Override
    public boolean applicable(PersistenceUnitDescriptor pu) {
        return "mydb".equals(pu.getName());
    }

    @Override
    public Map<String, String> processProperties(
            PersistenceUnitDescriptor pu, Properties properties) {
        return Map.of("hibernate.jdbc.batch_size", "100");
    }
}
```

### `IPropertiesConnectionInfoReader`

Populates `ConnectionBaseInfo` from persistence unit properties:

```java
public class MyConnectionReader
        implements IPropertiesConnectionInfoReader<MyConnectionReader> {

    @Override
    public ConnectionBaseInfo populateConnectionBaseInfo(
            PersistenceUnitDescriptor unit, Properties props,
            ConnectionBaseInfo cbi) {
        cbi.setMaxPoolSize(20);
        return cbi;
    }
}
```

### SPI summary

| SPI | Purpose |
|---|---|
| `IPropertiesEntityManagerReader` | Contribute database-specific Hibernate properties |
| `IPropertiesConnectionInfoReader` | Populate `ConnectionBaseInfo` from persistence properties |
| `IGuiceConfigurator` | Configure classpath scanning (enabled by `GuicedConfigurator`) |
| `ServiceContributor` (Hibernate) | Bridge the Vert.x instance into Hibernate Reactive |

## 💉 Dependency Injection

### Available bindings

| Type | Qualifier | Scope | Purpose |
|---|---|---|---|
| `Mutiny.SessionFactory` | `@Named("puName")` | Singleton | Named session factory for a specific persistence unit |
| `Mutiny.SessionFactory` | *(none)* | Singleton | Default session factory (from `defaultEm = true`) |
| `PersistService` | `@Named("puName")` | Singleton | Lifecycle service (`start()` / `stop()`) |
| `PersistService` | *(none)* | Singleton | Default persistence service |

### Multiple persistence units

```java
public class MultiDbService {

    @Inject
    private Mutiny.SessionFactory defaultFactory;  // from defaultEm = true

    @Inject
    @Named("orders")
    private Mutiny.SessionFactory ordersFactory;   // specific PU

    @Inject
    @Named("analytics")
    private Mutiny.SessionFactory analyticsFactory; // another PU
}
```

### Using sessions

```java
// Read with a session
sessionFactory.withSession(session ->
    session.find(User.class, userId)
).subscribe().with(
    user -> log.info("Found: {}", user),
    err  -> log.error("Failed", err)
);

// Write inside a transaction
sessionFactory.withTransaction(session ->
    session.persist(newUser)
           .chain(() -> session.persist(newOrder))
).subscribe().with(
    v    -> log.info("Committed"),
    err  -> log.error("Rolled back", err)
);
```

## 🔀 Multi-Database Setup

### Step 1 — Define persistence units

```xml
<persistence>
  <persistence-unit name="users">
    <class>com.example.entities.User</class>
    <properties>
      <property name="jakarta.persistence.jdbc.url" value="${USERS_DB_URL}"/>
      ...
    </properties>
  </persistence-unit>
  <persistence-unit name="orders">
    <class>com.example.entities.Order</class>
    <properties>
      <property name="jakarta.persistence.jdbc.url" value="${ORDERS_DB_URL}"/>
      ...
    </properties>
  </persistence-unit>
</persistence>
```

### Step 2 — Create modules

```java
@EntityManager(value = "users", defaultEm = true)
public class UsersDatabaseModule extends DatabaseModule<UsersDatabaseModule> {
    @Override protected String getPersistenceUnitName() { return "users"; }
    @Override protected ConnectionBaseInfo getConnectionBaseInfo(
            PersistenceUnitDescriptor unit, Properties props) {
        return ConnectionBaseInfoFactory.createConnectionBaseInfo("postgresql");
    }
}

@EntityManager(value = "orders", defaultEm = false)
public class OrdersDatabaseModule extends DatabaseModule<OrdersDatabaseModule> {
    @Override protected String getPersistenceUnitName() { return "orders"; }
    @Override protected ConnectionBaseInfo getConnectionBaseInfo(
            PersistenceUnitDescriptor unit, Properties props) {
        return ConnectionBaseInfoFactory.createConnectionBaseInfo("mysql");
    }
}
```

### Step 3 — Scope entities via package annotation

```java
@EntityManager(value = "orders")
package com.example.entities.orders;

import com.guicedee.persistence.annotations.EntityManager;
```

## 🔄 Startup Flow

```
IGuiceContext.instance()
 └─ IGuiceConfigurator hooks
     └─ GuicedConfigurator                   (enables rich classpath scanning)
 └─ IGuiceModule hooks
     └─ DatabaseModule subclasses            (parse persistence.xml, build ConnectionBaseInfo)
         └─ JtaPersistModule                 (bind PersistService + Mutiny.SessionFactory)
     └─ VertxPersistenceModule               (validate, bind defaults, process @EntityManager)
 └─ Hibernate ServiceContributor SPI
     └─ VertxServiceContributor              (registers shared Vertx instance)
 └─ IGuicePostStartup hooks
     └─ DatabaseModule.postLoad()            (PersistService.start() → EntityManagerFactory)
 └─ IGuicePreDestroy hooks
     └─ DatabaseModule.onDestroy()           (PersistService.stop() → close factory)
```

## 🗺️ Module Graph

```
com.guicedee.persistence
 ├── org.hibernate.reactive           (Hibernate Reactive — Mutiny.SessionFactory)
 ├── org.hibernate.orm.core           (Hibernate ORM — persistence.xml parsing, entity management)
 ├── com.guicedee.vertx               (Vert.x lifecycle, VertXPreStartup)
 ├── com.guicedee.guicedinjection     (GuicedEE — scanning, DI, lifecycle)
 ├── com.guicedee.microprofile.config (MicroProfile Config — @ConfigProperty injection, SmallRye Config)
 ├── io.vertx.sql.client              (Vert.x SQL Client — pooled connections)
 ├── io.vertx.sql.client.pg           (Vert.x PostgreSQL client — optional)
 ├── io.vertx.sql.client.mssql        (Vert.x MSSQL client — optional)
 ├── jakarta.transaction              (JTA transactions)
 └── com.guicedee.rest                (REST integration — optional)
```

## 🧩 JPMS

Module name: **`com.guicedee.persistence`**

The module:
- **exports** `com.guicedee.persistence`, `com.guicedee.persistence.annotations`, `com.guicedee.persistence.bind`, `com.guicedee.persistence.implementations`, and database-specific packages
- **provides** `IGuiceConfigurator` with `GuicedConfigurator`
- **provides** `IPropertiesEntityManagerReader` with `SystemEnvironmentVariablesPropertiesReader`, `HibernateEntityManagerProperties`, `PostgresHibernateProperties`, `MySqlHibernateProperties`, `OracleHibernateProperties`, `SqlServerHibernateProperties`, `DB2HibernateProperties`
- **provides** `IPropertiesConnectionInfoReader` with `HibernateDefaultConnectionBaseBuilder`
- **provides** `ServiceContributor` with `VertxServiceContributor`
- **uses** `IPropertiesConnectionInfoReader`, `IPropertiesEntityManagerReader`

In non-JPMS environments, `META-INF/services` discovery still works.

## 🏗️ Key Classes

| Class | Package | Role |
|---|---|---|
| `DatabaseModule` | `vertxpersistence` | Abstract Guice module — extend per persistence unit; lifecycle, config, and binding |
| `ConnectionBaseInfo` | `vertxpersistence` | Abstract connection configuration — host, port, credentials, pool settings |
| `ConnectionBaseInfoFactory` | `vertxpersistence` | Factory — creates database-specific `ConnectionBaseInfo` by type or JDBC URL |
| `ConnectionBaseInfoBuilder` | `vertxpersistence` | Maps `jakarta.persistence.jdbc.*` properties into `ConnectionBaseInfo` |
| `PersistService` | `vertxpersistence` | Interface — `start()` / `stop()` lifecycle for `EntityManagerFactory` |
| `@EntityManager` | `annotations` | Binding annotation — names persistence units, controls default binding |
| `JtaPersistModule` | `bind` | Internal Guice module — binds `PersistService`, `Mutiny.SessionFactory`, properties |
| `JtaPersistService` | `bind` | Manages `EntityManagerFactory` creation and shutdown via Mutiny `Uni` |
| `VertxPersistenceModule` | `implementations` | Guice module — validates `@EntityManager` annotations, binds defaults |
| `VertxServiceContributor` | `implementations` | Hibernate `ServiceContributor` — bridges shared Vert.x instance |
| `GuicedConfigurator` | `implementations` | `IGuiceConfigurator` — enables classpath scanning for persistence |
| `SystemEnvironmentVariablesPropertiesReader` | `implementations` | Resolves `${VAR:default}` placeholders in persistence properties |
| `PostgresConnectionBaseInfo` | `implementations.postgres` | PostgreSQL-specific Vert.x SQL pool creation |
| `MySqlConnectionBaseInfo` | `implementations.mysql` | MySQL-specific Vert.x SQL pool creation |
| `SqlServerConnectionBaseInfo` | `implementations.sqlserver` | SQL Server-specific Vert.x SQL pool creation |
| `OracleConnectionBaseInfo` | `implementations.oracle` | Oracle-specific Vert.x SQL pool creation |
| `DB2ConnectionBaseInfo` | `implementations.db2` | DB2-specific Vert.x SQL pool creation |

## 🧪 Testing

Use Testcontainers for integration tests with real databases:

```java
@Testcontainers
public class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @BeforeAll
    static void setup() {
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());

        IGuiceContext.registerModuleForScanning.add("com.example");
        IGuiceContext.instance();
    }
}
```

## 🤝 Contributing

Issues and pull requests are welcome — please add tests for new database adapters, connection options, or SPI implementations.

## 📄 License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

# ERD — Connection Configuration Domain

The core domain revolves around connection metadata readers, persistence descriptors, and the resultant Hibernate/Mutiny wiring.

```mermaid
erDiagram
    ConnectionBaseInfo {
        string persistenceUnitName PK
        string url
        string username
        string driver
        string jdbcIdentifier
        bool reactive
    }
    CleanConnectionBaseInfo {
        string url
        string username
        string driver
        string driverClass
    }
    DatabaseModule {
        string persistenceUnitName
        string moduleName
    }
    PersistenceUnitDescriptor {
        string name PK
        map properties
    }
    IPropertiesConnectionInfoReader {
        string readerName
    }
    IPropertiesEntityManagerReader {
        string readerName
    }
    JtaPersistModule {
        string persistenceUnitName
        string entityManagerAnnotation
    }

    ConnectionBaseInfo ||--|| CleanConnectionBaseInfo : "specializes"
    ConnectionBaseInfo }|..|| PersistenceUnitDescriptor : "populated from"
    ConnectionBaseInfo |o--|| JtaPersistModule : "serves"
    DatabaseModule ||--|| JtaPersistModule : "installs"
    DatabaseModule ||--|| PersistenceUnitDescriptor : "loads"
    DatabaseModule ||--o{ IPropertiesEntityManagerReader : "enriches properties"
    ConnectionBaseInfo ||--o{ IPropertiesConnectionInfoReader : "populated by"
```

The ERD identifies the persistence module database module as the entry point (`DatabaseModule` reads `PersistenceUnitDescriptor`), how property readers feed `ConnectionBaseInfo`, and how `JtaPersistModule` consumes the enriched metadata.

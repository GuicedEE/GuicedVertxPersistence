# Persistence discovery through SPI

Persistence and its Hibernate wrapper support `GUICEDEE_DISCOVERY_MODE=spi`. Register each `DatabaseModule` with `IGuiceModule`, using JPMS and `META-INF/services` for the supported launch paths. The persistence library starts and stops the services installed by Guice; applications do not repeat post-startup or pre-destroy registrations. Keep `META-INF/persistence.xml` and the reactive provider configuration. Environment placeholders are resolved by `SystemEnvironmentVariablesPropertiesReader` through the shared client `Environment` resolver.

Hibernate's `ScannerFactory` already selects GuicedEE's `ClassGraphScanner`. In SPI
mode it uses explicit registrations and named resources directly; it does not start
ClassGraph. Scan mode reuses the GuicedEE context's existing ClassGraph result.
Persistence-unit roots and exclusion rules are checked before reflecting on a
registered class, so unrelated units do not load its annotation dependencies.
Module read edges are added only for matching unit roots that Hibernate cannot
already read. No scanner-result cache is shared across persistence units.
`GUICEDEE_DIAGNOSTICS_DIR` adds a `discovery-scan` phase to distinguish this work
from Hibernate's metadata and session-factory construction.

Register model metadata through `IGuiceDiscovery` in client:

```java
public class PersistenceIndex implements IGuiceDiscovery<PersistenceIndex> {
    public Collection<Class<?>> classes() {
        return List.of(Order.class, Address.class, AuditedEntity.class, CurrencyConverter.class);
    }
    public Collection<Package> packages() {
        return List.of(PackageMarker.class.getPackage());
    }
}
```

The four example classes represent an `@Entity`, `@Embeddable`, `@MappedSuperclass` and `@Converter`. Register the index with `provides com.guicedee.client.services.IGuiceDiscovery with my.app.PersistenceIndex;` and the corresponding classpath service file. Discovery reads class metadata without initializing entities. Each registered class also contributes its package; use `packages()` for additional `package-info` metadata. Open model packages to `com.google.guice`, `org.hibernate.orm.core`, `org.hibernate.reactive` and `net.bytebuddy` for entity construction and proxy access. The wrapper adds Hibernate read edges for modules belonging to the persistence unit; it does not replace these application `opens` declarations.

The Hibernate adapter reads registered class bytes and named resources directly. It does not enumerate dependency archives or request a ClassGraph scan in SPI mode. Package-level Hibernate annotations and GuicedEE `@EntityManager` metadata are supported. Persistence-unit ownership still comes from `persistence.xml` and its root/companion archive URLs; global registrations do not make a class part of every unrelated unit.

The unit's `exclude-unlisted-classes` and root/non-root discovery settings still apply. An SPI registration does not override a unit's exclusion of unlisted classes. When exclusions are enabled, list the managed classes in that unit's XML. Explicit `<class>` entries work without repeating those classes in the discovery SPI. XML-only models work through `<mapping-file>` entries.

`META-INF/orm.xml` is looked up directly. Additional mappings, including legacy `*.hbm.xml` files, must be listed using `<mapping-file>` in SPI mode; their filenames are not discovered by archive enumeration. Scan mode retains automatic mapping-file discovery and adds explicit SPI registrations to its existing ClassGraph results.

Persistence no longer pulls in the unused MicroProfile Config integration or Vert.x Mutiny wrapper. Applications using `@ConfigProperty` should explicitly add `com.guicedee.microprofile:config`; the BOM selects the current 2.5.1 integration with its SPI discovery adapter. Consumers using `io.vertx.mutiny` APIs must add that wrapper themselves. Persistence still depends on Mutiny for its reactive API, and its existing environment/property readers remain available independently.

The inject benchmark's `verify-integration-spi.ps1` runs a packaged REST/persistence application on both launch paths with SPI and scan modes. It uses an isolated PostgreSQL container, waits for actual framework readiness, commits and reads entities through REST, verifies converters/embedded/inherited fields, and checks XML-only and explicitly listed models. Timed runs assert identical managed entities in both modes. A separate `-OmitDiscoveryModel` run verifies that an unregistered entity is absent only in SPI mode. Every run asserts that SPI mode creates no ClassGraph scanner. The PostgreSQL JPMS fixture explicitly resolves `io.vertx.sql.client.pg` and `com.ongres.scram.client` for SCRAM authentication.

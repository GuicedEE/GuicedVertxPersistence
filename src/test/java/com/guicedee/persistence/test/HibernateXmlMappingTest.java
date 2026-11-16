package com.guicedee.persistence.test;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** Runs against the installed GuicedEE Hibernate wrapper, including its shaded replacements. */
class HibernateXmlMappingTest {
    public static class LegacyEntity {
        private Long id;
        private String name;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    private void verifyMapping(String xml) {
        try (var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build()) {
            var metadata = new MetadataSources(registry)
                    .addInputStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                    .buildMetadata();
            var binding = metadata.getEntityBinding(LegacyEntity.class.getName());
            assertNotNull(binding);
            assertEquals(1, metadata.getEntityBindings().size());
            ((org.hibernate.boot.spi.MetadataImplementor) metadata).validate();
        }
    }

    @Test void legacyHbmMappingStillBuildsItsEntityModel() {
        verifyMapping("""
                <hibernate-mapping>
                  <class name="%s" table="legacy_entities">
                    <id name="id" type="long"/>
                    <property name="name" type="string"/>
                  </class>
                </hibernate-mapping>
                """.formatted(LegacyEntity.class.getName()));
    }

    @Test void standardOrmXmlStillBuildsItsEntityModel() {
        verifyMapping("""
                <entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2">
                  <entity class="%s" access="PROPERTY">
                    <table name="orm_entities"/>
                    <attributes><id name="id"/><basic name="name"/></attributes>
                  </entity>
                </entity-mappings>
                """.formatted(LegacyEntity.class.getName()));
    }
}

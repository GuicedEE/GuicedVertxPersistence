package com.guicedee.persistence.test;

import com.guicedee.persistence.implementations.PersistenceDescriptorParser;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceDescriptorParserTest {
    @TempDir Path directory;
    private static final String OPEN = "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"3.2\">";
    private static final String UNIT = """
            <persistence-unit name="test" transaction-type="RESOURCE_LOCAL">
              <provider>org.hibernate.reactive.provider.ReactivePersistenceProvider</provider>
              <class>example.First</class><class>example.Second</class>
              <exclude-unlisted-classes>true</exclude-unlisted-classes>
              <properties><property name="url" value="${DB_URL:jdbc:postgresql://localhost/test}"/>
                <property name="password" value="a&amp;b&quot;c"/></properties>
            </persistence-unit>
            """;

    private URL xml(String document) throws Exception {
        Path meta = Files.createDirectories(directory.resolve("META-INF"));
        return Files.writeString(meta.resolve("persistence.xml"), document).toUri().toURL();
    }

    private static Map<String, PersistenceUnitDescriptor> original(List<URL> urls) {
        return PersistenceXmlParser.create(Map.of(), null, null).parse(urls);
    }

    private static void equivalent(PersistenceUnitDescriptor expected, PersistenceUnitDescriptor actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getProviderClassName(), actual.getProviderClassName());
        assertEquals(expected.getPersistenceUnitRootUrl(), actual.getPersistenceUnitRootUrl());
        assertEquals(expected.getPersistenceUnitTransactionType(), actual.getPersistenceUnitTransactionType());
        assertEquals(expected.getManagedClassNames(), actual.getManagedClassNames());
        assertEquals(expected.getMappingFileNames(), actual.getMappingFileNames());
        assertEquals(expected.getJarFileUrls(), actual.getJarFileUrls());
        assertEquals(expected.isExcludeUnlistedClasses(), actual.isExcludeUnlistedClasses());
        assertEquals(expected.getValidationMode(), actual.getValidationMode());
        assertEquals(expected.getSharedCacheMode(), actual.getSharedCacheMode());
        assertEquals(expected.getProperties(), actual.getProperties());
    }

    @Test void commonDescriptorsMatchHibernateWithoutUsingItsParser() throws Exception {
        for (String version : List.of("3.0", "3.1", "3.2")) {
            List<URL> urls = List.of(xml(OPEN.replace("3.2", version) + UNIT + "</persistence>"));
            var actual = PersistenceDescriptorParser.parse(urls, snapshots -> fail("Unexpected JAXB fallback"));
            equivalent(original(urls).get("test"), actual.get("test"));
        }
    }

    @Test void defaultsModesPrefixesAndMultipleUnitsMatchHibernate() throws Exception {
        String document = OPEN + """
                <persistence-unit name="empty"/>
                <persistence-unit name="jta" transaction-type="JTA"><description>description</description>
                 <exclude-unlisted-classes>1</exclude-unlisted-classes>
                 <shared-cache-mode>NONE</shared-cache-mode><validation-mode>NONE</validation-mode>
                </persistence-unit></persistence>
                """;
        List<URL> urls = List.of(xml(document));
        var expected = original(urls);
        var actual = PersistenceDescriptorParser.parse(urls, snapshots -> fail("Unexpected JAXB fallback"));
        for (String name : expected.keySet()) equivalent(expected.get(name), actual.get(name));
        urls = List.of(xml((OPEN + UNIT + "</persistence>")
                .replace("xmlns=", "xmlns:p=").replaceAll("<(?![!?])(/?)([a-z])", "<$1p:$2")));
        var prefixed = PersistenceDescriptorParser.parse(urls, snapshots -> fail("Unexpected prefix fallback"));
        equivalent(original(urls).get("test"), prefixed.get("test"));
    }

    @Test void packagedJarRootMatchesHibernate() throws Exception {
        Path jar = directory.resolve("app.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("META-INF/persistence.xml"));
            output.write((OPEN + UNIT + "</persistence>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
        List<URL> urls = List.of(new URL("jar:" + jar.toUri() + "!/META-INF/persistence.xml"));
        var actual = PersistenceDescriptorParser.parse(urls, snapshots -> fail("Unexpected jar fallback"));
        equivalent(original(urls).get("test"), actual.get("test"));
    }

    @Test void fallbackPreservesPackagedJarRootAndUtf16Encoding() throws Exception {
        Path jar = directory.resolve("legacy.jar");
        String document = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>" + OPEN
                + "<persistence-unit name=\"mapped\"><mapping-file>orm.xml</mapping-file></persistence-unit></persistence>";
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("META-INF/persistence.xml"));
            output.write(document.getBytes(java.nio.charset.StandardCharsets.UTF_16));
            output.closeEntry();
        }
        List<URL> urls = List.of(new URL("jar:" + jar.toUri() + "!/META-INF/persistence.xml"));
        var actual = PersistenceDescriptorParser.parse(urls, PersistenceDescriptorParserTest::original);
        equivalent(original(urls).get("mapped"), actual.get("mapped"));
    }

    @Test void unsupportedGrammarsDelegateAsAWhole() throws Exception {
        var cases = List.of(
                OPEN + "<persistence-unit name=\"test\"><mapping-file>orm.xml</mapping-file></persistence-unit></persistence>",
                OPEN + "<persistence-unit name=\"test\"><jar-file>entities.jar</jar-file></persistence-unit></persistence>",
                OPEN + "<persistence-unit name=\"test\"><jta-data-source>ds</jta-data-source></persistence-unit></persistence>",
                OPEN + UNIT + UNIT + "</persistence>",
                (OPEN + UNIT + "</persistence>").replace("version=\"3.2\"", ""),
                (OPEN + UNIT + "</persistence>").replace("https://jakarta.ee/xml/ns/persistence", "http://xmlns.jcp.org/xml/ns/persistence"),
                OPEN + "<persistence-unit name=\"test\"><class>A</class><provider>late</provider></persistence-unit></persistence>",
                OPEN + "<persistence-unit name=\"test\" unknown=\"value\"/></persistence>",
                OPEN + "<persistence-unit name=\"test\"><exclude-unlisted-classes>perhaps</exclude-unlisted-classes></persistence-unit></persistence>",
                OPEN + "<persistence-unit name=\"test\"><class xmlns=\"urn:other\">A</class></persistence-unit></persistence>"
        );
        for (String document : cases) {
            var calls = new AtomicInteger();
            Map<String, PersistenceUnitDescriptor> sentinel = Map.of();
            assertSame(sentinel, PersistenceDescriptorParser.parse(List.of(xml(document)), snapshots -> {
                calls.incrementAndGet(); return sentinel;
            }));
            assertEquals(1, calls.get());
        }
    }

    @Test void fallbackPreservesHibernateMappingFilesAndDiagnostics() throws Exception {
        List<URL> urls = List.of(xml(OPEN + "<persistence-unit name=\"test\"><mapping-file>orm.xml</mapping-file></persistence-unit></persistence>"));
        var calls = new AtomicInteger();
        var result = PersistenceDescriptorParser.parse(urls, snapshots -> { calls.incrementAndGet(); return original(snapshots); });
        assertEquals(List.of("orm.xml"), result.get("test").getMappingFileNames());
        assertEquals(1, calls.get());
        List<URL> malformed = List.of(xml(OPEN + "<persistence-unit name=\"test\">"));
        assertThrows(RuntimeException.class, () -> PersistenceDescriptorParser.parse(malformed, PersistenceDescriptorParserTest::original));
    }

    @Test void explicitHibernateParserOverrideIsHonored() throws Exception {
        String setting = "GUICEDEE_PERSISTENCE_XML_PARSER";
        String previous = System.getProperty(setting);
        try {
            System.setProperty(setting, "hibernate");
            var calls = new AtomicInteger();
            List<URL> urls = List.of(xml(OPEN + UNIT + "</persistence>"));
            var result = PersistenceDescriptorParser.parse(urls, snapshots -> {
                calls.incrementAndGet(); return original(snapshots);
            });
            assertEquals(1, calls.get());
            assertEquals(List.of("example.First", "example.Second"), result.get("test").getManagedClassNames());
        } finally {
            if (previous == null) System.clearProperty(setting);
            else System.setProperty(setting, previous);
        }
    }
}

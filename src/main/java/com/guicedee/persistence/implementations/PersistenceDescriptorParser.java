package com.guicedee.persistence.implementations;

import com.guicedee.client.Environment;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceException;
import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * Reads the common Jakarta Persistence descriptor grammar without JAXB startup.
 * Anything outside this strict subset is delegated to Hibernate's original parser,
 * including legacy namespaces, mapping files, jar files and data sources.
 * DTDs, malformed XML and processing-limit violations fail closed, including on fallback.
 * The original parser receives validated snapshots with the original resource locations.
 */
public final class PersistenceDescriptorParser {
    private static final String NAMESPACE = "https://jakarta.ee/xml/ns/persistence";
    private PersistenceDescriptorParser() {}

    public static Map<String, PersistenceUnitDescriptor> parse(List<URL> urls,
            Function<List<URL>, Map<String, PersistenceUnitDescriptor>> original) {
        boolean fallback = "hibernate".equalsIgnoreCase(Environment.getSystemPropertyOrEnvironment(
                "GUICEDEE_PERSISTENCE_XML_PARSER", "auto"));
        int maxBytes = maximumDescriptorBytes();
        try {
            var factory = XMLInputFactory.newDefaultFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.USE_CATALOG, false);
            factory.setProperty("jdk.xml.maxElementDepth", 64);
            factory.setProperty("jdk.xml.elementAttributeLimit", 64);
            factory.setProperty("jdk.xml.maxXMLNameLimit", 1024);
            factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
                throw new PersistenceException("External XML references are not allowed in persistence descriptors");
            });
            var units = new LinkedHashMap<String, PersistenceUnitDescriptor>();
            var snapshots = new ArrayList<URL>(urls.size());
            for (URL url : urls) {
                var connection = url.openConnection();
                connection.setUseCaches(false);
                byte[] bytes;
                try (var input = connection.getInputStream()) {
                    bytes = input.readNBytes(maxBytes + 1);
                }
                if (bytes.length > maxBytes) {
                    throw new PersistenceException("Persistence descriptor exceeds GUICEDEE_PERSISTENCE_XML_MAX_BYTES: " + url);
                }
                var reader = factory.createXMLStreamReader(new ByteArrayInputStream(bytes));
                try {
                    if (!fallback) {
                        try { readDocument(reader, url, units); }
                        catch (UnsupportedDescriptor | IllegalArgumentException unsupported) { fallback = true; }
                    }
                    // Validate the entire document and every resource, even after discovering an
                    // unsupported grammar or when the original parser was explicitly selected.
                    while (reader.hasNext()) secureNext(reader);
                } finally {
                    reader.close();
                }
                snapshots.add(snapshot(url, bytes));
            }
            return fallback ? original.apply(List.copyOf(snapshots)) : units;
        } catch (XMLStreamException | IOException failure) {
            throw new PersistenceException("Cannot safely read persistence descriptor XML", failure);
        }
    }

    private static int maximumDescriptorBytes() {
        String configured = Environment.getSystemPropertyOrEnvironment("GUICEDEE_PERSISTENCE_XML_MAX_BYTES", "8388608");
        try {
            int limit = Integer.parseInt(configured);
            if (limit > 0 && limit < Integer.MAX_VALUE) return limit;
        } catch (NumberFormatException ignored) { }
        throw new PersistenceException("GUICEDEE_PERSISTENCE_XML_MAX_BYTES must be a positive integer below 2147483647");
    }

    private static URL snapshot(URL location, byte[] bytes) throws IOException {
        // Hibernate must parse the exact bytes that passed validation, without reopening a
        // changing file or network URL. Preserve the URL for persistence-unit root resolution.
        return new URL(null, location.toExternalForm(), new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    @Override public void connect() { }
                    @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
                };
            }
        });
    }

    private static int secureNext(XMLStreamReader reader) throws XMLStreamException {
        int event = reader.next();
        if (event == DTD || event == ENTITY_REFERENCE) {
            throw new PersistenceException("DTD declarations and entity references are not allowed in persistence descriptors");
        }
        return event;
    }

    private static void readDocument(XMLStreamReader reader, URL url,
                                     Map<String, PersistenceUnitDescriptor> units) throws XMLStreamException {
        require(nextTag(reader) == START_ELEMENT);
        element(reader, "persistence");
        attributes(reader, Set.of("version"), true);
        String version = reader.getAttributeValue(null, "version");
        require(version != null && Set.of("3.0", "3.1", "3.2").contains(version));
        int count = 0;
        while (nextTag(reader) == START_ELEMENT) {
            element(reader, "persistence-unit");
            attributes(reader, Set.of("name", "transaction-type"), false);
            String name = reader.getAttributeValue(null, "name");
            require(name != null && !name.isEmpty() && !units.containsKey(name));
            var unit = new ParsedPersistenceXmlDescriptor(ArchiveHelper.getJarURLFromURLEntry(url, "/META-INF/persistence.xml"));
            unit.setName(name);
            String transaction = reader.getAttributeValue(null, "transaction-type");
            unit.setTransactionType(transaction == null ? PersistenceUnitTransactionType.RESOURCE_LOCAL
                    : PersistenceUnitTransactionType.valueOf(transaction));
            readUnit(reader, unit);
            units.put(name, unit);
            count++;
        }
        element(reader, "persistence");
        require(count > 0);
        while (reader.hasNext()) {
            int event = secureNext(reader);
            require(event == END_DOCUMENT || event == COMMENT || event == PROCESSING_INSTRUCTION
                    || event == SPACE || event == CHARACTERS && reader.isWhiteSpace());
        }
    }

    private static void readUnit(XMLStreamReader reader, ParsedPersistenceXmlDescriptor unit) throws XMLStreamException {
        int order = 0;
        var seen = new HashSet<String>();
        while (nextTag(reader) == START_ELEMENT) {
            String name = reader.getLocalName();
            element(reader, name);
            int next = switch (name) {
                case "description" -> 1;
                case "provider" -> 2;
                case "class" -> 3;
                case "exclude-unlisted-classes" -> 4;
                case "shared-cache-mode" -> 5;
                case "validation-mode" -> 6;
                case "properties" -> 7;
                default -> throw new UnsupportedDescriptor();
            };
            require(next >= order && ("class".equals(name) || seen.add(name)));
            order = next;
            attributes(reader, Set.of(), false);
            if ("properties".equals(name)) {
                readProperties(reader, unit);
                continue;
            }
            String value = reader.getElementText();
            switch (name) {
                case "provider" -> unit.setProviderClassName(value);
                case "class" -> unit.addClasses(List.of(value));
                case "exclude-unlisted-classes" -> {
                    String booleanValue = value.trim();
                    require(Set.of("true", "false", "1", "0").contains(booleanValue));
                    unit.setExcludeUnlistedClasses("true".equals(booleanValue) || "1".equals(booleanValue));
                }
                case "shared-cache-mode" -> unit.setSharedCacheMode(SharedCacheMode.valueOf(value.trim()));
                case "validation-mode" -> unit.setValidationMode(ValidationMode.valueOf(value.trim()));
                default -> { /* Description has no corresponding descriptor field. */ }
            }
        }
        element(reader, "persistence-unit");
    }

    private static void readProperties(XMLStreamReader reader, ParsedPersistenceXmlDescriptor unit) throws XMLStreamException {
        int count = 0;
        while (nextTag(reader) == START_ELEMENT) {
            element(reader, "property");
            attributes(reader, Set.of("name", "value"), false);
            String name = reader.getAttributeValue(null, "name");
            String value = reader.getAttributeValue(null, "value");
            require(name != null && value != null);
            require(reader.getElementText().trim().isEmpty());
            unit.getProperties().put(name, value);
            count++;
        }
        element(reader, "properties");
        require(count > 0);
    }

    private static int nextTag(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = secureNext(reader);
            if (event == START_ELEMENT || event == END_ELEMENT) return event;
            require(event == COMMENT || event == PROCESSING_INSTRUCTION || event == SPACE
                    || (event == CHARACTERS || event == CDATA) && reader.isWhiteSpace());
        }
        throw new UnsupportedDescriptor();
    }

    private static void element(XMLStreamReader reader, String name) {
        require(name.equals(reader.getLocalName()) && NAMESPACE.equals(reader.getNamespaceURI()));
    }

    private static void attributes(XMLStreamReader reader, Set<String> allowed, boolean schemaLocation) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String namespace = reader.getAttributeNamespace(i);
            String name = reader.getAttributeLocalName(i);
            require((namespace == null || namespace.isEmpty()) && allowed.contains(name)
                    || schemaLocation && XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(namespace)
                    && "schemaLocation".equals(name));
        }
    }

    private static void require(boolean supported) {
        if (!supported) throw new UnsupportedDescriptor();
    }

    private static final class UnsupportedDescriptor extends RuntimeException {
        private UnsupportedDescriptor() { super(null, null, false, false); }
    }
}

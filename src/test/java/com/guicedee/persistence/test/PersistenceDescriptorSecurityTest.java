package com.guicedee.persistence.test;

import com.guicedee.persistence.implementations.PersistenceDescriptorParser;
import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.PersistenceException;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceDescriptorSecurityTest {
    @TempDir Path directory;
    private static final String OPEN = "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"3.2\">";
    private static final String UNIT = "<persistence-unit name=\"safe\"/></persistence>";

    private URL xml(String name, String content) throws Exception {
        Path file = Files.createDirectories(directory.resolve(name).resolve("META-INF")).resolve("persistence.xml");
        return Files.writeString(file, content).toUri().toURL();
    }

    private static void rejected(List<URL> urls) {
        assertThrows(PersistenceException.class, () -> PersistenceDescriptorParser.parse(urls,
                snapshots -> fail("Unsafe XML must never reach Hibernate fallback")));
    }

    @Test void rejectsDtdsEntitiesAndExpansionWithoutExternalRequests() throws Exception {
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/external.dtd";
            String secret = Files.writeString(directory.resolve("secret.txt"), "must-not-be-read").toUri().toString();
            for (String dtd : List.of(
                    "<!DOCTYPE persistence SYSTEM \"" + address + "\">",
                    "<!DOCTYPE persistence [<!ENTITY xxe SYSTEM \"" + secret + "\">]>",
                    "<!DOCTYPE persistence [<!ENTITY xxe SYSTEM \"" + address + "\">]>",
                    "<!DOCTYPE persistence [<!ENTITY % remote SYSTEM \"" + address + "\">%remote;]>",
                    "<!DOCTYPE persistence [<!ENTITY a '1234567890'><!ENTITY b '&a;&a;&a;&a;'><!ENTITY xxe '&b;&b;&b;&b;'>]>")) {
                rejected(List.of(xml("attack", dtd + OPEN + "<persistence-unit name=\"attack\"><description>&xxe;</description></persistence-unit></persistence>")));
            }
            assertEquals(0, requests.get(), "No external DTD or entity may contact the network");
        } finally { server.stop(0); }
    }

    @Test void validatesAllResourcesEvenWhenAnEarlierOneNeedsFallback() throws Exception {
        URL unsupported = xml("first", OPEN + "<persistence-unit name=\"safe\"><mapping-file>orm.xml</mapping-file></persistence-unit></persistence>");
        URL unsafe = xml("second", "<!DOCTYPE persistence [<!ENTITY a 'unsafe'>]>" + OPEN + UNIT);
        rejected(List.of(unsupported, unsafe));
        String previous = System.getProperty("GUICEDEE_PERSISTENCE_XML_PARSER");
        try {
            System.setProperty("GUICEDEE_PERSISTENCE_XML_PARSER", "hibernate");
            rejected(List.of(unsafe));
        } finally { restore("GUICEDEE_PERSISTENCE_XML_PARSER", previous); }
    }

    @Test void rejectsUtf16DtdWithoutAStringBasedSecurityFilter() throws Exception {
        String document = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><!DOCTYPE persistence [<!ENTITY a 'unsafe'>]>" + OPEN + UNIT;
        Path file = directory.resolve("wide.xml");
        Files.write(file, document.getBytes(java.nio.charset.StandardCharsets.UTF_16));
        rejected(List.of(file.toUri().toURL()));
    }

    @Test void schemaHintsAndXincludeDoNotFetchExternalResources() throws Exception {
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet(); exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        server.start();
        try {
            String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/external";
            String hinted = OPEN.replace("version=", "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://jakarta.ee/xml/ns/persistence " + address + "\" version=");
            var parsed = PersistenceDescriptorParser.parse(List.of(xml("hint", hinted + UNIT)), snapshots -> fail("Ordinary descriptor"));
            assertEquals(1, parsed.size());
            var fallback = PersistenceDescriptorParser.parse(List.of(xml("fallback", hinted + "<persistence-unit name=\"safe\"><mapping-file>orm.xml</mapping-file></persistence-unit></persistence>")),
                    snapshots -> PersistenceXmlParser.create(Map.of(), null, null).parse(snapshots));
            assertEquals(1, fallback.size());
            URL include = xml("include", OPEN + "<xi:include xmlns:xi=\"http://www.w3.org/2001/XInclude\" href=\"" + address + "\"/>" + UNIT);
            // Hibernate may reject or ignore an unknown element, but must never resolve XInclude.
            try { PersistenceDescriptorParser.parse(List.of(include), snapshots -> PersistenceXmlParser.create(Map.of(), null, null).parse(snapshots)); }
            catch (RuntimeException expected) { }
            assertEquals(0, requests.get());
        } finally { server.stop(0); }
    }

    @Test void fallbackUsesValidatedBytesEvenIfTheOriginalFileChanges() throws Exception {
        URL source = xml("snapshot", OPEN + "<persistence-unit name=\"safe\"><mapping-file>orm.xml</mapping-file></persistence-unit></persistence>");
        var units = PersistenceDescriptorParser.parse(List.of(source), snapshots -> {
            try { Files.writeString(Path.of(source.toURI()), "<!DOCTYPE persistence [<!ENTITY a 'changed'>]>" + OPEN + UNIT); }
            catch (Exception failure) { throw new AssertionError(failure); }
            assertEquals(source.toExternalForm(), snapshots.getFirst().toExternalForm());
            return PersistenceXmlParser.create(Map.of(), null, null).parse(snapshots);
        });
        assertEquals(List.of("orm.xml"), units.get("safe").getMappingFileNames());
    }

    @Test void malformedAndExcessivelyNestedXmlFailClosed() throws Exception {
        rejected(List.of(xml("malformed", OPEN + "<persistence-unit>")));
        rejected(List.of(xml("deep", OPEN + "<unknown>".repeat(70) + "</unknown>".repeat(70) + UNIT)));
        rejected(List.of(xml("attributes", OPEN + "<unknown " + java.util.stream.IntStream.range(0, 70)
                .mapToObj(i -> "a" + i + "=\"x\" ").collect(java.util.stream.Collectors.joining()) + "/>" + UNIT)));
    }

    @Test void descriptorSizeIsBoundedAndCanBeRaisedForLargeApplications() throws Exception {
        String property = "GUICEDEE_PERSISTENCE_XML_MAX_BYTES";
        String previous = System.getProperty(property);
        URL url = xml("size", OPEN + UNIT);
        try {
            System.setProperty(property, "64");
            rejected(List.of(url));
            System.setProperty(property, "1024");
            assertEquals(1, PersistenceDescriptorParser.parse(List.of(url), snapshots -> fail("Ordinary descriptor")).size());
            for (String invalid : List.of("0", "-1", "unbounded", "2147483647")) {
                System.setProperty(property, invalid);
                rejected(List.of(url));
            }
        } finally { restore(property, previous); }
    }

    private static void restore(String property, String previous) {
        if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
    }
}

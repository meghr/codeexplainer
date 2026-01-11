package com.codeexplainer.parser;

import com.codeexplainer.core.model.ManifestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ManifestParser.
 */
class ManifestParserTest {

    private ManifestParser manifestParser;

    @BeforeEach
    void setUp() {
        manifestParser = new ManifestParser();
    }

    @Test
    void parse_shouldReturnEmptyInfoForNullBytes() {
        ManifestInfo info = manifestParser.parse((byte[]) null);
        assertNotNull(info);
        assertNull(info.getMainClass());
    }

    @Test
    void parse_shouldReturnEmptyInfoForEmptyBytes() {
        byte[] emptyBytes = new byte[0];
        ManifestInfo info = manifestParser.parse(emptyBytes);
        assertNotNull(info);
        assertNull(info.getMainClass());
    }

    @Test
    void parse_shouldExtractMainClass() {
        String manifest = """
                Manifest-Version: 1.0
                Main-Class: com.example.Main
                """;

        ManifestInfo info = manifestParser.parse(manifest.getBytes(StandardCharsets.UTF_8));

        assertEquals("com.example.Main", info.getMainClass());
    }

    @Test
    void parse_shouldExtractImplementationInfo() {
        String manifest = """
                Manifest-Version: 1.0
                Implementation-Title: My Library
                Implementation-Version: 1.2.3
                Implementation-Vendor: ACME Corp
                """;

        ManifestInfo info = manifestParser.parse(manifest.getBytes(StandardCharsets.UTF_8));

        assertEquals("My Library", info.getImplementationTitle());
        assertEquals("1.2.3", info.getImplementationVersion());
        assertEquals("ACME Corp", info.getImplementationVendor());
    }

    @Test
    void parse_shouldExtractBuildInfo() {
        String manifest = """
                Manifest-Version: 1.0
                Built-By: jenkins
                Build-Jdk: 17.0.1
                Created-By: Maven
                """;

        ManifestInfo info = manifestParser.parse(manifest.getBytes(StandardCharsets.UTF_8));

        assertEquals("jenkins", info.getBuiltBy());
        assertEquals("17.0.1", info.getBuildJdk());
        assertEquals("Maven", info.getCreatedBy());
    }

    @Test
    void parse_shouldCollectCustomAttributes() {
        String manifest = """
                Manifest-Version: 1.0
                X-Custom-Attr: custom-value
                Bundle-Name: My Bundle
                """;

        ManifestInfo info = manifestParser.parse(manifest.getBytes(StandardCharsets.UTF_8));

        assertTrue(info.getCustomAttributes().containsKey("X-Custom-Attr"));
        assertEquals("custom-value", info.getCustomAttributes().get("X-Custom-Attr"));
    }
}

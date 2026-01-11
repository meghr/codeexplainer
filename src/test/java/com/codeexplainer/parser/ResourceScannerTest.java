package com.codeexplainer.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceScanner.
 */
class ResourceScannerTest {

    private ResourceScanner resourceScanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        resourceScanner = new ResourceScanner();
    }

    @Test
    void scanResource_shouldIdentifyXmlConfig() throws IOException {
        Path xmlFile = tempDir.resolve("config.xml");
        Files.writeString(xmlFile, "<beans></beans>");

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(xmlFile);

        assertNotNull(info);
        assertEquals(ResourceScanner.ResourceType.XML_CONFIG, info.getType());
        assertEquals("config.xml", info.getFileName());
    }

    @Test
    void scanResource_shouldIdentifyProperties() throws IOException {
        Path propsFile = tempDir.resolve("app.properties");
        Files.writeString(propsFile, "key=value");

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(propsFile);

        assertEquals(ResourceScanner.ResourceType.PROPERTIES, info.getType());
    }

    @Test
    void scanResource_shouldIdentifyYamlConfig() throws IOException {
        Path yamlFile = tempDir.resolve("config.yml");
        Files.writeString(yamlFile, "server:\n  port: 8080");

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(yamlFile);

        assertEquals(ResourceScanner.ResourceType.YAML_CONFIG, info.getType());
    }

    @Test
    void scanResource_shouldIdentifySpringConfig() throws IOException {
        Path springConfig = tempDir.resolve("application.yml");
        Files.writeString(springConfig, "spring:\n  application:\n    name: test");

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(springConfig);

        assertEquals(ResourceScanner.ResourceType.SPRING_CONFIG, info.getType());
    }

    @Test
    void scanResource_shouldIdentifySpringProfileConfig() throws IOException {
        Path profileConfig = tempDir.resolve("application-dev.properties");
        Files.writeString(profileConfig, "debug=true");

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(profileConfig);

        assertEquals(ResourceScanner.ResourceType.SPRING_CONFIG, info.getType());
        assertEquals("dev", info.getMetadata().get("profile"));
    }

    @Test
    void scanResource_shouldIncludeContentPreview() throws IOException {
        Path yamlFile = tempDir.resolve("config.yml");
        String content = "server:\n  port: 8080";
        Files.writeString(yamlFile, content);

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(yamlFile);

        assertEquals(content, info.getContentPreview());
    }

    @Test
    void scan_shouldProcessMultipleFiles() throws IOException {
        Path xml = tempDir.resolve("beans.xml");
        Path props = tempDir.resolve("app.properties");
        Files.writeString(xml, "<beans/>");
        Files.writeString(props, "key=value");

        List<ResourceScanner.ResourceInfo> results = resourceScanner.scan(
                List.of(xml.toString(), props.toString()));

        assertEquals(2, results.size());
    }

    @Test
    void scanResource_shouldReturnNullForDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectories(dir);

        ResourceScanner.ResourceInfo info = resourceScanner.scanResource(dir);

        assertNull(info);
    }

    @Test
    void findSpringConfigs_shouldFilterCorrectly() throws IOException {
        Path appYml = tempDir.resolve("application.yml");
        Path config = tempDir.resolve("other.xml");
        Files.writeString(appYml, "spring: {}");
        Files.writeString(config, "<root/>");

        List<ResourceScanner.ResourceInfo> allResources = resourceScanner.scan(
                List.of(appYml.toString(), config.toString()));

        List<ResourceScanner.ResourceInfo> springConfigs = resourceScanner.findSpringConfigs(allResources);

        assertEquals(1, springConfigs.size());
        assertEquals("application.yml", springConfigs.get(0).getFileName());
    }
}

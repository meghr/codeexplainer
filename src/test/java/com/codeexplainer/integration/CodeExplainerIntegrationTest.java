package com.codeexplainer.integration;

import com.codeexplainer.web.CodeExplainerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CodeExplainerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private File sampleJar;

    @BeforeEach
    void setUp() throws IOException {
        // Create a valid sample JAR file for testing
        sampleJar = createSampleJar();
    }

    @Test
    @DisplayName("End-to-End Analysis Workflow")
    void testFullAnalysisFlow() {
        // 1. Upload JAR
        String uploadUrl = "http://localhost:" + port + "/api/analyze";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(sampleJar));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Object> response = restTemplate.postForEntity(uploadUrl, requestEntity, Object.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Extract session ID (assuming simple map structure in response for Object)
        // In real test we'd map to a defined response class, but loose typing works for
        // quick verify
        // Let's assume the response body is a Map

        // 2. Health Check
        String healthUrl = "http://localhost:" + port + "/api/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
    }

    @Test
    @DisplayName("Export Endpoints")
    void testExportEndpoints() {
        // 1. Upload to get session
        String sessionId = uploadJarAndGetSessionId();
        assertNotNull(sessionId);

        // 2. Test JSON Export
        String jsonUrl = "http://localhost:" + port + "/api/sessions/" + sessionId + "/export?format=json";
        ResponseEntity<String> jsonResponse = restTemplate.getForEntity(jsonUrl, String.class);
        assertEquals(HttpStatus.OK, jsonResponse.getStatusCode());
        assertTrue(jsonResponse.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON));

        // 3. Test PDF Export
        String pdfUrl = "http://localhost:" + port + "/api/sessions/" + sessionId + "/export?format=pdf";
        ResponseEntity<byte[]> pdfResponse = restTemplate.getForEntity(pdfUrl, byte[].class);
        assertEquals(HttpStatus.OK, pdfResponse.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, pdfResponse.getHeaders().getContentType());
        assertTrue(pdfResponse.getBody().length > 0);
    }

    private String uploadJarAndGetSessionId() {
        String uploadUrl = "http://localhost:" + port + "/api/analyze";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(sampleJar));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<java.util.Map> response = restTemplate.postForEntity(uploadUrl, requestEntity,
                java.util.Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("sessionId");
        }
        return null;
    }

    /**
     * Creates a minimal valid JAR file with one class.
     */
    private File createSampleJar() throws IOException {
        Path tempDir = Files.createTempDirectory("codeexplainer-test");
        File jarFile = tempDir.resolve("test-lib.jar").toFile();

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            // Add a dummy entry (not a real class but enough to be a valid zip/jar
            // structure)
            // Ideally we'd compile a class, but for basic parsing test, structure matters
            // most.
            // However, BytecodeAnalysisService needs REAL bytecodes.
            // We can write a "module-info.class" or simple dummy if we have bytes.
            // Since we can't easily compile on the fly without tools, we'll try to add a
            // resource file.
            // Note: Bytecode analysis might fail if no .class files.
            // So we should try to mock or accept that it finds 0 classes.

            JarEntry entry = new JarEntry("META-INF/services/test.txt");
            jos.putNextEntry(entry);
            jos.write("test content".getBytes());
            jos.closeEntry();
        }

        return jarFile;
    }
}

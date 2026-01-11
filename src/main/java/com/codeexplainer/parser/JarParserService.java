package com.codeexplainer.parser;

import com.codeexplainer.core.exception.JarParsingException;
import com.codeexplainer.core.model.ManifestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Main service that coordinates JAR parsing operations.
 * Combines JarExtractor, ManifestParser, ClassFileReader, and ResourceScanner.
 */
@Service
public class JarParserService {

    private static final Logger log = LoggerFactory.getLogger(JarParserService.class);

    private final JarExtractor jarExtractor;
    private final ManifestParser manifestParser;
    private final ClassFileReader classFileReader;
    private final ResourceScanner resourceScanner;

    public JarParserService(JarExtractor jarExtractor,
            ManifestParser manifestParser,
            ClassFileReader classFileReader,
            ResourceScanner resourceScanner) {
        this.jarExtractor = jarExtractor;
        this.manifestParser = manifestParser;
        this.classFileReader = classFileReader;
        this.resourceScanner = resourceScanner;
    }

    /**
     * Result of parsing a JAR file.
     */
    public static class ParseResult {
        private JarContent jarContent;
        private ManifestInfo manifestInfo;
        private List<ResourceScanner.ResourceInfo> resources;
        private int classCount;
        private String javaVersion;

        // Getters and Setters
        public JarContent getJarContent() {
            return jarContent;
        }

        public void setJarContent(JarContent jarContent) {
            this.jarContent = jarContent;
        }

        public ManifestInfo getManifestInfo() {
            return manifestInfo;
        }

        public void setManifestInfo(ManifestInfo manifestInfo) {
            this.manifestInfo = manifestInfo;
        }

        public List<ResourceScanner.ResourceInfo> getResources() {
            return resources;
        }

        public void setResources(List<ResourceScanner.ResourceInfo> resources) {
            this.resources = resources;
        }

        public int getClassCount() {
            return classCount;
        }

        public void setClassCount(int classCount) {
            this.classCount = classCount;
        }

        public String getJavaVersion() {
            return javaVersion;
        }

        public void setJavaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
        }
    }

    /**
     * Parses a JAR file from a file path.
     *
     * @param jarPath Path to the JAR file
     * @return ParseResult containing all parsed information
     * @throws JarParsingException if parsing fails
     */
    public ParseResult parse(String jarPath) throws JarParsingException {
        return parse(new File(jarPath));
    }

    /**
     * Parses a JAR file.
     *
     * @param jarFile The JAR file
     * @return ParseResult containing all parsed information
     * @throws JarParsingException if parsing fails
     */
    public ParseResult parse(File jarFile) throws JarParsingException {
        log.info("Starting JAR parsing: {}", jarFile.getName());
        long startTime = System.currentTimeMillis();

        ParseResult result = new ParseResult();

        // Step 1: Extract JAR
        JarContent content = jarExtractor.extract(jarFile);
        result.setJarContent(content);
        result.setClassCount(content.getTotalClassCount());

        // Step 2: Parse manifest
        if (content.getManifestBytes() != null) {
            ManifestInfo manifestInfo = manifestParser.parse(content.getManifestBytes());
            result.setManifestInfo(manifestInfo);
        } else {
            result.setManifestInfo(new ManifestInfo());
        }

        // Step 3: Scan resources
        List<String> allResources = new java.util.ArrayList<>();
        allResources.addAll(content.getResourceFiles());
        allResources.addAll(content.getConfigFiles());
        result.setResources(resourceScanner.scan(allResources));

        // Step 4: Determine Java version from first class file
        if (!content.getClassFiles().isEmpty()) {
            try {
                // Major version is in bytes 6-7 of class file (big-endian)
                byte[] classBytes = Files.readAllBytes(java.nio.file.Paths.get(content.getClassFiles().get(0)));
                int majorVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
                result.setJavaVersion(classFileReader.getJavaVersion(majorVersion));
            } catch (Exception e) {
                log.debug("Could not determine Java version", e);
                result.setJavaVersion("Unknown");
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("JAR parsing complete in {}ms: {} classes, {} resources",
                duration, result.getClassCount(), result.getResources().size());

        return result;
    }

    /**
     * Parses a JAR from an uploaded file stream.
     *
     * @param inputStream The uploaded file stream
     * @param fileName    Original filename
     * @return ParseResult containing all parsed information
     * @throws JarParsingException if parsing fails
     */
    public ParseResult parse(InputStream inputStream, String fileName) throws JarParsingException {
        JarContent content = jarExtractor.extract(inputStream, fileName);

        ParseResult result = new ParseResult();
        result.setJarContent(content);
        result.setClassCount(content.getTotalClassCount());

        if (content.getManifestBytes() != null) {
            result.setManifestInfo(manifestParser.parse(content.getManifestBytes()));
        } else {
            result.setManifestInfo(new ManifestInfo());
        }

        List<String> allResources = new java.util.ArrayList<>();
        allResources.addAll(content.getResourceFiles());
        allResources.addAll(content.getConfigFiles());
        result.setResources(resourceScanner.scan(allResources));

        return result;
    }

    /**
     * Cleans up temporary files from parsing.
     *
     * @param result The parse result to clean up
     */
    public void cleanup(ParseResult result) {
        if (result != null && result.getJarContent() != null) {
            jarExtractor.cleanup(result.getJarContent());
        }
    }

    /**
     * Gets the JarExtractor for direct access.
     */
    public JarExtractor getJarExtractor() {
        return jarExtractor;
    }

    /**
     * Gets the ClassFileReader for direct access.
     */
    public ClassFileReader getClassFileReader() {
        return classFileReader;
    }

    /**
     * Gets the ResourceScanner for direct access.
     */
    public ResourceScanner getResourceScanner() {
        return resourceScanner;
    }
}

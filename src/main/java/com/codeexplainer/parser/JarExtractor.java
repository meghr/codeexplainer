package com.codeexplainer.parser;

import com.codeexplainer.config.CodeExplainerProperties;
import com.codeexplainer.core.exception.JarParsingException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Extracts JAR files to a temporary directory for analysis.
 * Handles both regular JARs and nested JARs (JARs within JARs).
 */
@Component
public class JarExtractor {

    private static final Logger log = LoggerFactory.getLogger(JarExtractor.class);

    private final CodeExplainerProperties properties;

    public JarExtractor(CodeExplainerProperties properties) {
        this.properties = properties;
    }

    /**
     * Extracts a JAR file to a temporary directory.
     *
     * @param jarPath Path to the JAR file
     * @return JarContent containing extracted file information
     * @throws JarParsingException if extraction fails
     */
    public JarContent extract(String jarPath) throws JarParsingException {
        return extract(new File(jarPath));
    }

    /**
     * Extracts a JAR file to a temporary directory.
     *
     * @param jarFile The JAR file to extract
     * @return JarContent containing extracted file information
     * @throws JarParsingException if extraction fails
     */
    public JarContent extract(File jarFile) throws JarParsingException {
        validateJarFile(jarFile);

        JarContent content = new JarContent();
        content.setJarName(jarFile.getName());
        content.setJarPath(jarFile.getAbsolutePath());
        content.setJarSize(jarFile.length());

        try {
            // Calculate JAR hash for caching
            content.setJarHash(calculateHash(jarFile));

            // Create extraction directory
            Path extractDir = createExtractionDirectory(jarFile.getName());
            content.setExtractedPath(extractDir.toString());

            log.info("Extracting JAR {} to {}", jarFile.getName(), extractDir);

            // Extract the JAR
            extractJarEntries(jarFile, extractDir, content);

            log.info("Extraction complete. Found {} classes, {} resources",
                    content.getTotalClassCount(), content.getTotalResourceCount());

            return content;

        } catch (IOException e) {
            throw new JarParsingException("Failed to extract JAR file: " + jarFile.getName(), e);
        }
    }

    /**
     * Extracts a JAR from an InputStream (for uploaded files).
     *
     * @param inputStream The input stream containing JAR data
     * @param fileName    Original filename
     * @return JarContent containing extracted file information
     * @throws JarParsingException if extraction fails
     */
    public JarContent extract(InputStream inputStream, String fileName) throws JarParsingException {
        try {
            // Save to temp file first
            Path tempFile = Files.createTempFile("upload-", "-" + fileName);
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            JarContent content = extract(tempFile.toFile());

            // Delete temp file after extraction
            Files.deleteIfExists(tempFile);

            return content;
        } catch (IOException e) {
            throw new JarParsingException("Failed to process uploaded JAR: " + fileName, e);
        }
    }

    /**
     * Cleans up extracted files for a JarContent.
     *
     * @param content The JarContent to clean up
     */
    public void cleanup(JarContent content) {
        if (content.getExtractedPath() != null) {
            try {
                FileUtils.deleteDirectory(new File(content.getExtractedPath()));
                log.debug("Cleaned up extraction directory: {}", content.getExtractedPath());
            } catch (IOException e) {
                log.warn("Failed to cleanup extraction directory: {}", content.getExtractedPath(), e);
            }
        }
    }

    private void validateJarFile(File jarFile) throws JarParsingException {
        if (!jarFile.exists()) {
            throw new JarParsingException("JAR file does not exist: " + jarFile.getAbsolutePath());
        }
        if (!jarFile.isFile()) {
            throw new JarParsingException("Path is not a file: " + jarFile.getAbsolutePath());
        }
        if (!jarFile.canRead()) {
            throw new JarParsingException("Cannot read JAR file: " + jarFile.getAbsolutePath());
        }
        String name = jarFile.getName().toLowerCase();
        if (!name.endsWith(".jar") && !name.endsWith(".war") && !name.endsWith(".ear")) {
            throw new JarParsingException("File is not a JAR/WAR/EAR: " + jarFile.getName());
        }
    }

    private Path createExtractionDirectory(String jarName) throws IOException {
        String tempDir = properties.getTempDir();
        if (tempDir == null || tempDir.isEmpty()) {
            tempDir = System.getProperty("java.io.tmpdir") + "/code-explainer";
        }

        Path baseDir = Paths.get(tempDir);
        Files.createDirectories(baseDir);

        String uniqueDir = jarName.replace(".jar", "") + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path extractDir = baseDir.resolve(uniqueDir);
        Files.createDirectories(extractDir);

        return extractDir;
    }

    private void extractJarEntries(File jarFile, Path extractDir, JarContent content) throws IOException {
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    Files.createDirectories(extractDir.resolve(entryName));
                    continue;
                }

                // Categorize and extract the entry
                Path targetPath = extractDir.resolve(entryName);
                Files.createDirectories(targetPath.getParent());

                try (InputStream is = zipFile.getInputStream(entry);
                        OutputStream os = Files.newOutputStream(targetPath)) {
                    is.transferTo(os);
                }

                // Categorize the file
                categorizeEntry(entryName, targetPath.toString(), content);
            }
        }
    }

    private void categorizeEntry(String entryName, String extractedPath, JarContent content) {
        String lowerName = entryName.toLowerCase();

        if (lowerName.endsWith(".class")) {
            content.addClassFile(extractedPath);
        } else if (lowerName.equals("meta-inf/manifest.mf")) {
            try {
                content.setManifestBytes(Files.readAllBytes(Paths.get(extractedPath)));
            } catch (IOException e) {
                log.warn("Failed to read manifest: {}", e.getMessage());
            }
        } else if (lowerName.endsWith(".jar")) {
            content.addNestedJar(extractedPath);
        } else if (isConfigFile(lowerName)) {
            content.addConfigFile(extractedPath);
        } else {
            content.addResourceFile(extractedPath);
        }
    }

    private boolean isConfigFile(String name) {
        return name.endsWith(".xml") ||
                name.endsWith(".properties") ||
                name.endsWith(".yml") ||
                name.endsWith(".yaml") ||
                name.endsWith(".json") ||
                name.endsWith(".conf") ||
                name.contains("config");
    }

    private String calculateHash(File file) throws JarParsingException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new JarParsingException("Failed to calculate JAR hash", e);
        }
    }
}

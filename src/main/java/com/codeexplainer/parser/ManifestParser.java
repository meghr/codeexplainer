package com.codeexplainer.parser;

import com.codeexplainer.core.exception.JarParsingException;
import com.codeexplainer.core.model.ManifestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Parses JAR manifest files (MANIFEST.MF) to extract metadata.
 */
@Component
public class ManifestParser {

    private static final Logger log = LoggerFactory.getLogger(ManifestParser.class);

    /**
     * Parses manifest from bytes.
     *
     * @param manifestBytes The manifest file bytes
     * @return ManifestInfo containing parsed metadata
     * @throws JarParsingException if parsing fails
     */
    public ManifestInfo parse(byte[] manifestBytes) throws JarParsingException {
        if (manifestBytes == null || manifestBytes.length == 0) {
            log.debug("No manifest data provided, returning empty ManifestInfo");
            return new ManifestInfo();
        }

        try (InputStream is = new ByteArrayInputStream(manifestBytes)) {
            return parseManifest(new Manifest(is));
        } catch (IOException e) {
            throw new JarParsingException("Failed to parse manifest", e);
        }
    }

    /**
     * Parses manifest from a file path.
     *
     * @param manifestPath Path to the manifest file
     * @return ManifestInfo containing parsed metadata
     * @throws JarParsingException if parsing fails
     */
    public ManifestInfo parse(Path manifestPath) throws JarParsingException {
        try (InputStream is = Files.newInputStream(manifestPath)) {
            return parseManifest(new Manifest(is));
        } catch (IOException e) {
            throw new JarParsingException("Failed to read manifest file: " + manifestPath, e);
        }
    }

    private ManifestInfo parseManifest(Manifest manifest) {
        ManifestInfo info = new ManifestInfo();
        Attributes mainAttributes = manifest.getMainAttributes();

        if (mainAttributes == null) {
            return info;
        }

        // Standard manifest attributes
        info.setMainClass(getAttributeValue(mainAttributes, Attributes.Name.MAIN_CLASS));
        info.setImplementationTitle(getAttributeValue(mainAttributes, Attributes.Name.IMPLEMENTATION_TITLE));
        info.setImplementationVersion(getAttributeValue(mainAttributes, Attributes.Name.IMPLEMENTATION_VERSION));
        info.setImplementationVendor(getAttributeValue(mainAttributes, Attributes.Name.IMPLEMENTATION_VENDOR));
        info.setSpecificationTitle(getAttributeValue(mainAttributes, Attributes.Name.SPECIFICATION_TITLE));
        info.setSpecificationVersion(getAttributeValue(mainAttributes, Attributes.Name.SPECIFICATION_VERSION));

        // Common custom attributes
        info.setBuiltBy(getStringValue(mainAttributes, "Built-By"));
        info.setBuildJdk(getStringValue(mainAttributes, "Build-Jdk"));
        if (info.getBuildJdk() == null) {
            info.setBuildJdk(getStringValue(mainAttributes, "Build-Jdk-Spec"));
        }
        info.setCreatedBy(getStringValue(mainAttributes, "Created-By"));

        // Collect all custom attributes
        for (Object key : mainAttributes.keySet()) {
            String keyName = key.toString();
            String value = mainAttributes.getValue(keyName);

            // Skip standard attributes we've already processed
            if (!isStandardAttribute(keyName) && value != null) {
                info.getCustomAttributes().put(keyName, value);
            }
        }

        log.debug("Parsed manifest: title={}, version={}",
                info.getImplementationTitle(), info.getImplementationVersion());

        return info;
    }

    private String getAttributeValue(Attributes attrs, Attributes.Name name) {
        Object value = attrs.get(name);
        return value != null ? value.toString() : null;
    }

    private String getStringValue(Attributes attrs, String name) {
        return attrs.getValue(name);
    }

    private boolean isStandardAttribute(String name) {
        return name.equals("Manifest-Version") ||
                name.equals("Main-Class") ||
                name.equals("Implementation-Title") ||
                name.equals("Implementation-Version") ||
                name.equals("Implementation-Vendor") ||
                name.equals("Specification-Title") ||
                name.equals("Specification-Version") ||
                name.equals("Built-By") ||
                name.equals("Build-Jdk") ||
                name.equals("Build-Jdk-Spec") ||
                name.equals("Created-By");
    }
}

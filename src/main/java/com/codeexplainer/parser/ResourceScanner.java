package com.codeexplainer.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans and categorizes resource files found in JAR.
 */
@Component
public class ResourceScanner {

    private static final Logger log = LoggerFactory.getLogger(ResourceScanner.class);

    /**
     * Resource types for categorization.
     */
    public enum ResourceType {
        XML_CONFIG,
        PROPERTIES,
        YAML_CONFIG,
        JSON_CONFIG,
        SQL_SCRIPT,
        SPRING_CONFIG,
        WEB_RESOURCE,
        STATIC_ASSET,
        TEMPLATE,
        OTHER
    }

    /**
     * Represents a scanned resource with metadata.
     */
    public static class ResourceInfo {
        private String path;
        private String fileName;
        private ResourceType type;
        private long size;
        private String contentPreview;
        private Map<String, String> metadata = new HashMap<>();

        // Getters and Setters
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public ResourceType getType() {
            return type;
        }

        public void setType(ResourceType type) {
            this.type = type;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getContentPreview() {
            return contentPreview;
        }

        public void setContentPreview(String contentPreview) {
            this.contentPreview = contentPreview;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * Scans a list of resource files and returns detailed information.
     *
     * @param resourcePaths List of paths to resource files
     * @return List of ResourceInfo with details
     */
    public List<ResourceInfo> scan(List<String> resourcePaths) {
        List<ResourceInfo> resources = new ArrayList<>();

        for (String pathStr : resourcePaths) {
            try {
                ResourceInfo info = scanResource(Paths.get(pathStr));
                if (info != null) {
                    resources.add(info);
                }
            } catch (Exception e) {
                log.warn("Failed to scan resource: {}", pathStr, e);
            }
        }

        return resources;
    }

    /**
     * Scans a single resource file.
     *
     * @param path Path to the resource
     * @return ResourceInfo or null if not a valid resource
     */
    public ResourceInfo scanResource(Path path) {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return null;
        }

        ResourceInfo info = new ResourceInfo();
        info.setPath(path.toString());
        info.setFileName(path.getFileName().toString());
        info.setType(determineResourceType(path));

        try {
            info.setSize(Files.size(path));

            // Get content preview for text files
            if (isTextResource(info.getType())) {
                info.setContentPreview(getContentPreview(path, 500));
            }

            // Extract metadata for specific types
            extractMetadata(path, info);

        } catch (IOException e) {
            log.debug("Failed to read resource details: {}", path, e);
        }

        return info;
    }

    /**
     * Find all Spring configuration files.
     *
     * @param resources List of resources to search
     * @return List of Spring config resources
     */
    public List<ResourceInfo> findSpringConfigs(List<ResourceInfo> resources) {
        return resources.stream()
                .filter(r -> r.getType() == ResourceType.SPRING_CONFIG ||
                        r.getFileName().startsWith("application") ||
                        r.getFileName().startsWith("bootstrap"))
                .toList();
    }

    /**
     * Find all web resources (WEB-INF contents).
     *
     * @param resources List of resources to search
     * @return List of web resources
     */
    public List<ResourceInfo> findWebResources(List<ResourceInfo> resources) {
        return resources.stream()
                .filter(r -> r.getPath().contains("WEB-INF") ||
                        r.getType() == ResourceType.WEB_RESOURCE)
                .toList();
    }

    private ResourceType determineResourceType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        String pathStr = path.toString().toLowerCase();

        // Spring configuration files
        if (fileName.startsWith("application") &&
                (fileName.endsWith(".yml") || fileName.endsWith(".yaml") || fileName.endsWith(".properties"))) {
            return ResourceType.SPRING_CONFIG;
        }
        if (fileName.equals("bootstrap.yml") || fileName.equals("bootstrap.yaml") ||
                fileName.equals("bootstrap.properties")) {
            return ResourceType.SPRING_CONFIG;
        }

        // By extension
        if (fileName.endsWith(".xml")) {
            return ResourceType.XML_CONFIG;
        }
        if (fileName.endsWith(".properties")) {
            return ResourceType.PROPERTIES;
        }
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return ResourceType.YAML_CONFIG;
        }
        if (fileName.endsWith(".json")) {
            return ResourceType.JSON_CONFIG;
        }
        if (fileName.endsWith(".sql")) {
            return ResourceType.SQL_SCRIPT;
        }

        // Web resources
        if (pathStr.contains("web-inf") || pathStr.contains("meta-inf")) {
            return ResourceType.WEB_RESOURCE;
        }
        if (fileName.endsWith(".html") || fileName.endsWith(".htm") ||
                fileName.endsWith(".jsp") || fileName.endsWith(".jspx")) {
            return ResourceType.TEMPLATE;
        }

        // Static assets
        if (fileName.endsWith(".css") || fileName.endsWith(".js") ||
                fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                fileName.endsWith(".gif") || fileName.endsWith(".svg") ||
                fileName.endsWith(".ico") || fileName.endsWith(".woff") ||
                fileName.endsWith(".woff2") || fileName.endsWith(".ttf")) {
            return ResourceType.STATIC_ASSET;
        }

        // Templates
        if (pathStr.contains("templates") || pathStr.contains("views")) {
            return ResourceType.TEMPLATE;
        }

        return ResourceType.OTHER;
    }

    private boolean isTextResource(ResourceType type) {
        return type != ResourceType.STATIC_ASSET && type != ResourceType.OTHER;
    }

    private String getContentPreview(Path path, int maxLength) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String content = new String(bytes, StandardCharsets.UTF_8);

        if (content.length() > maxLength) {
            return content.substring(0, maxLength) + "...";
        }
        return content;
    }

    private void extractMetadata(Path path, ResourceInfo info) {
        String fileName = info.getFileName().toLowerCase();

        // For Spring configs, note the profile if present
        if (info.getType() == ResourceType.SPRING_CONFIG) {
            if (fileName.contains("-")) {
                String profile = fileName.substring(
                        fileName.indexOf("-") + 1,
                        fileName.lastIndexOf("."));
                info.getMetadata().put("profile", profile);
            }
        }

        // For XML, try to extract root element
        if (info.getType() == ResourceType.XML_CONFIG && info.getContentPreview() != null) {
            String preview = info.getContentPreview();
            int rootStart = preview.indexOf('<');
            if (rootStart >= 0) {
                int rootEnd = preview.indexOf('>', rootStart);
                if (rootEnd > rootStart) {
                    String rootTag = preview.substring(rootStart + 1, rootEnd).split("\\s")[0];
                    if (!rootTag.startsWith("?") && !rootTag.startsWith("!")) {
                        info.getMetadata().put("rootElement", rootTag);
                    }
                }
            }
        }
    }
}

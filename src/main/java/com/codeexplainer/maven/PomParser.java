package com.codeexplainer.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Service for parsing Maven POM files and extracting project information.
 */
@Service
public class PomParser {

    private static final Logger log = LoggerFactory.getLogger(PomParser.class);

    /**
     * Parses a POM file and extracts project information.
     */
    public PomInfo parse(Path pomPath) throws IOException {
        String content = Files.readString(pomPath);
        return parse(content);
    }

    /**
     * Parses POM content string.
     */
    public PomInfo parse(String pomContent) {
        log.info("Parsing POM content");

        String groupId = extractValue(pomContent, "groupId");
        String artifactId = extractValue(pomContent, "artifactId");
        String version = extractValue(pomContent, "version");
        String packaging = extractValue(pomContent, "packaging");
        String name = extractValue(pomContent, "name");
        String description = extractValue(pomContent, "description");

        // Handle parent inheritance
        String parentSection = extractSection(pomContent, "parent");
        ParentInfo parent = null;
        if (parentSection != null) {
            parent = new ParentInfo(
                    extractValue(parentSection, "groupId"),
                    extractValue(parentSection, "artifactId"),
                    extractValue(parentSection, "version"));
            // Inherit groupId/version from parent if not specified
            if (groupId == null && parent.groupId() != null) {
                groupId = parent.groupId();
            }
            if (version == null && parent.version() != null) {
                version = parent.version();
            }
        }

        // Parse dependencies
        List<DependencyInfo> dependencies = parseDependencies(pomContent);

        // Parse dependency management
        List<DependencyInfo> managedDependencies = parseManagedDependencies(pomContent);

        // Parse properties
        Map<String, String> properties = parseProperties(pomContent);

        // Parse modules (for multi-module projects)
        List<String> modules = parseModules(pomContent);

        return new PomInfo(
                groupId,
                artifactId,
                version,
                packaging != null ? packaging : "jar",
                name,
                description,
                parent,
                dependencies,
                managedDependencies,
                properties,
                modules);
    }

    /**
     * Resolves property placeholders in a POM.
     */
    public String resolveProperties(String value, Map<String, String> properties) {
        if (value == null)
            return null;

        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(value);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String propName = matcher.group(1);
            String replacement = properties.getOrDefault(propName, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Gets effective dependencies (merging dependency management).
     */
    public List<DependencyInfo> getEffectiveDependencies(PomInfo pom) {
        List<DependencyInfo> effective = new ArrayList<>();
        Map<String, DependencyInfo> managed = new HashMap<>();

        // Build managed dependency map
        for (DependencyInfo dep : pom.managedDependencies()) {
            String key = dep.groupId() + ":" + dep.artifactId();
            managed.put(key, dep);
        }

        // Merge with dependencies
        for (DependencyInfo dep : pom.dependencies()) {
            String key = dep.groupId() + ":" + dep.artifactId();
            DependencyInfo merged = dep;

            if (managed.containsKey(key)) {
                DependencyInfo m = managed.get(key);
                merged = new DependencyInfo(
                        dep.groupId(),
                        dep.artifactId(),
                        dep.version() != null ? dep.version() : m.version(),
                        dep.scope() != null ? dep.scope() : m.scope(),
                        dep.type() != null ? dep.type() : m.type(),
                        dep.classifier(),
                        dep.optional() || m.optional(),
                        dep.exclusions().isEmpty() ? m.exclusions() : dep.exclusions());
            }

            // Resolve properties
            String version = resolveProperties(merged.version(), pom.properties());
            if (version != null && !version.startsWith("${")) {
                effective.add(new DependencyInfo(
                        merged.groupId(),
                        merged.artifactId(),
                        version,
                        merged.scope(),
                        merged.type(),
                        merged.classifier(),
                        merged.optional(),
                        merged.exclusions()));
            }
        }

        return effective;
    }

    // ============= Private Methods =============

    private List<DependencyInfo> parseDependencies(String pom) {
        String section = extractSection(pom, "dependencies");
        if (section == null)
            return List.of();

        // Exclude dependencyManagement section
        int dmStart = section.indexOf("<dependencyManagement>");
        if (dmStart != -1) {
            int dmEnd = section.indexOf("</dependencyManagement>");
            if (dmEnd > dmStart) {
                section = section.substring(0, dmStart) + section.substring(dmEnd + 24);
            }
        }

        return parseDependencyList(section);
    }

    private List<DependencyInfo> parseManagedDependencies(String pom) {
        String section = extractSection(pom, "dependencyManagement");
        if (section == null)
            return List.of();

        String deps = extractSection(section, "dependencies");
        if (deps == null)
            return List.of();

        return parseDependencyList(deps);
    }

    private List<DependencyInfo> parseDependencyList(String section) {
        List<DependencyInfo> deps = new ArrayList<>();

        String[] blocks = section.split("<dependency>");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            int endIdx = block.indexOf("</dependency>");
            if (endIdx > 0)
                block = block.substring(0, endIdx);

            String groupId = extractValue(block, "groupId");
            String artifactId = extractValue(block, "artifactId");
            String version = extractValue(block, "version");
            String scope = extractValue(block, "scope");
            String type = extractValue(block, "type");
            String classifier = extractValue(block, "classifier");
            String optional = extractValue(block, "optional");

            List<ExclusionInfo> exclusions = parseExclusions(block);

            if (groupId != null && artifactId != null) {
                deps.add(new DependencyInfo(
                        groupId,
                        artifactId,
                        version,
                        scope != null ? scope : "compile",
                        type != null ? type : "jar",
                        classifier,
                        "true".equals(optional),
                        exclusions));
            }
        }

        return deps;
    }

    private List<ExclusionInfo> parseExclusions(String block) {
        List<ExclusionInfo> exclusions = new ArrayList<>();
        String section = extractSection(block, "exclusions");
        if (section == null)
            return exclusions;

        String[] excBlocks = section.split("<exclusion>");
        for (int i = 1; i < excBlocks.length; i++) {
            String excBlock = excBlocks[i];
            String groupId = extractValue(excBlock, "groupId");
            String artifactId = extractValue(excBlock, "artifactId");
            if (groupId != null && artifactId != null) {
                exclusions.add(new ExclusionInfo(groupId, artifactId));
            }
        }

        return exclusions;
    }

    private Map<String, String> parseProperties(String pom) {
        Map<String, String> props = new HashMap<>();
        String section = extractSection(pom, "properties");
        if (section == null)
            return props;

        Pattern pattern = Pattern.compile("<([^>]+)>([^<]*)</\\1>");
        Matcher matcher = pattern.matcher(section);
        while (matcher.find()) {
            props.put(matcher.group(1), matcher.group(2).trim());
        }

        return props;
    }

    private List<String> parseModules(String pom) {
        List<String> modules = new ArrayList<>();
        String section = extractSection(pom, "modules");
        if (section == null)
            return modules;

        Pattern pattern = Pattern.compile("<module>([^<]+)</module>");
        Matcher matcher = pattern.matcher(section);
        while (matcher.find()) {
            modules.add(matcher.group(1).trim());
        }

        return modules;
    }

    private String extractValue(String content, String tagName) {
        String start = "<" + tagName + ">";
        String end = "</" + tagName + ">";
        int startIdx = content.indexOf(start);
        int endIdx = content.indexOf(end);

        if (startIdx != -1 && endIdx > startIdx) {
            return content.substring(startIdx + start.length(), endIdx).trim();
        }
        return null;
    }

    private String extractSection(String content, String tagName) {
        String start = "<" + tagName + ">";
        String startAlt = "<" + tagName + " "; // With attributes
        String end = "</" + tagName + ">";

        int startIdx = content.indexOf(start);
        if (startIdx == -1) {
            startIdx = content.indexOf(startAlt);
            if (startIdx != -1) {
                startIdx = content.indexOf(">", startIdx);
            }
        } else {
            startIdx += start.length();
        }

        int endIdx = content.lastIndexOf(end);

        if (startIdx != -1 && endIdx > startIdx) {
            return content.substring(startIdx, endIdx);
        }
        return null;
    }

    // ============= Records =============

    public record PomInfo(
            String groupId,
            String artifactId,
            String version,
            String packaging,
            String name,
            String description,
            ParentInfo parent,
            List<DependencyInfo> dependencies,
            List<DependencyInfo> managedDependencies,
            Map<String, String> properties,
            List<String> modules) {
        public String coordinates() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }

    public record ParentInfo(
            String groupId,
            String artifactId,
            String version) {
    }

    public record DependencyInfo(
            String groupId,
            String artifactId,
            String version,
            String scope,
            String type,
            String classifier,
            boolean optional,
            List<ExclusionInfo> exclusions) {
        public String coordinates() {
            return groupId + ":" + artifactId + ":" + version;
        }

        public boolean isCompileScope() {
            return "compile".equals(scope) || scope == null;
        }

        public boolean isRuntimeScope() {
            return "runtime".equals(scope);
        }

        public boolean isTestScope() {
            return "test".equals(scope);
        }
    }

    public record ExclusionInfo(
            String groupId,
            String artifactId) {
    }
}

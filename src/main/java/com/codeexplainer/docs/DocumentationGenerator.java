package com.codeexplainer.docs;

import com.codeexplainer.core.model.*;
import com.codeexplainer.detector.ComponentDetector;
import com.codeexplainer.detector.ComponentDetector.*;
import com.codeexplainer.detector.EndpointDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating comprehensive documentation from JAR analysis.
 * Produces markdown and HTML documentation for classes, endpoints, and
 * components.
 */
@Service
public class DocumentationGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocumentationGenerator.class);

    private final EndpointDetector endpointDetector;
    private final ComponentDetector componentDetector;

    public DocumentationGenerator(EndpointDetector endpointDetector,
            ComponentDetector componentDetector) {
        this.endpointDetector = endpointDetector;
        this.componentDetector = componentDetector;
    }

    /**
     * Generates complete documentation for a JAR analysis result.
     */
    public GeneratedDocumentation generate(JarAnalysisResult analysisResult, DocOptions options) {
        log.info("Generating documentation for {}", analysisResult.getJarName());

        StringBuilder markdown = new StringBuilder();

        // Title and Overview
        markdown.append("# ").append(analysisResult.getJarName()).append(" - API Documentation\n\n");
        markdown.append("*Generated: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("*\n\n");

        // Table of Contents
        markdown.append("## Table of Contents\n\n");
        markdown.append("1. [Overview](#overview)\n");
        markdown.append("2. [Package Structure](#package-structure)\n");
        markdown.append("3. [Components](#components)\n");
        markdown.append("4. [REST Endpoints](#rest-endpoints)\n");
        markdown.append("5. [Class Reference](#class-reference)\n\n");
        markdown.append("---\n\n");

        // Overview section
        generateOverview(markdown, analysisResult);

        // Package structure
        if (options.includePackageStructure()) {
            generatePackageStructure(markdown, analysisResult.getClasses());
        }

        // Components
        if (options.includeComponents()) {
            generateComponentsSection(markdown, analysisResult.getClasses());
        }

        // Endpoints
        if (options.includeEndpoints()) {
            generateEndpointsSection(markdown, analysisResult.getClasses());
        }

        // Class reference
        if (options.includeClassReference()) {
            generateClassReference(markdown, analysisResult.getClasses(), options);
        }

        String markdownContent = markdown.toString();
        String htmlContent = options.generateHtml() ? convertToHtml(markdownContent) : null;

        return new GeneratedDocumentation(
                markdownContent,
                htmlContent,
                analysisResult.getJarName(),
                LocalDateTime.now());
    }

    /**
     * Generates documentation for a single class.
     */
    public String generateClassDoc(ClassMetadata classMetadata) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Class: ").append(classMetadata.getClassName()).append("\n\n");
        sb.append("**Package:** `").append(classMetadata.getPackageName()).append("`\n\n");
        sb.append("**Type:** ").append(classMetadata.getClassType()).append("\n\n");

        // Annotations
        if (!classMetadata.getAnnotations().isEmpty()) {
            sb.append("## Annotations\n\n");
            for (String annotation : classMetadata.getAnnotations()) {
                sb.append("- `").append(simplifyAnnotation(annotation)).append("`\n");
            }
            sb.append("\n");
        }

        // Inheritance
        if (classMetadata.getSuperClassName() != null &&
                !classMetadata.getSuperClassName().equals("java.lang.Object")) {
            sb.append("**Extends:** `").append(classMetadata.getSuperClassName()).append("`\n\n");
        }

        if (!classMetadata.getInterfaces().isEmpty()) {
            sb.append("**Implements:** ");
            sb.append(classMetadata.getInterfaces().stream()
                    .map(i -> "`" + i + "`")
                    .collect(Collectors.joining(", ")));
            sb.append("\n\n");
        }

        // Fields
        if (!classMetadata.getFields().isEmpty()) {
            sb.append("## Fields\n\n");
            sb.append("| Name | Type | Modifiers |\n");
            sb.append("|------|------|----------|\n");
            for (FieldMetadata field : classMetadata.getFields()) {
                sb.append("| `").append(field.getFieldName()).append("` | `")
                        .append(simplifyType(field.getType())).append("` | ")
                        .append(formatModifiers(field.getAccessModifiers())).append(" |\n");
            }
            sb.append("\n");
        }

        // Methods
        if (!classMetadata.getMethods().isEmpty()) {
            sb.append("## Methods\n\n");
            for (MethodMetadata method : classMetadata.getMethods()) {
                if (method.getMethodName().startsWith("<"))
                    continue;

                sb.append("### `").append(method.getMethodName()).append("()`\n\n");
                sb.append("**Returns:** `")
                        .append(method.getReturnType() != null ? simplifyType(method.getReturnType()) : "void")
                        .append("`\n\n");

                if (!method.getParameters().isEmpty()) {
                    sb.append("**Parameters:**\n\n");
                    for (ParameterInfo param : method.getParameters()) {
                        sb.append("- `").append(param.getName()).append("`: `")
                                .append(simplifyType(param.getType())).append("`\n");
                    }
                    sb.append("\n");
                }

                if (!method.getAnnotations().isEmpty()) {
                    sb.append("**Annotations:** ");
                    sb.append(method.getAnnotations().stream()
                            .map(a -> "`" + simplifyAnnotation(a) + "`")
                            .collect(Collectors.joining(", ")));
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generates API reference documentation.
     */
    public String generateApiReference(List<ClassMetadata> classes) {
        List<EndpointInfo> endpoints = endpointDetector.detectEndpoints(classes);
        return endpointDetector.generateEndpointSummary(endpoints);
    }

    /**
     * Generates component reference documentation.
     */
    public String generateComponentReference(List<ClassMetadata> classes) {
        List<DetectedComponent> components = componentDetector.detectComponents(classes);
        return componentDetector.generateComponentReport(components);
    }

    /**
     * Saves documentation to files.
     */
    public void saveToFile(GeneratedDocumentation doc, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String safeName = doc.jarName().replaceAll("[^a-zA-Z0-9.-]", "_");

        // Save markdown
        Path mdPath = outputDir.resolve(safeName + "-docs.md");
        Files.writeString(mdPath, doc.markdown());
        log.info("Saved markdown documentation to {}", mdPath);

        // Save HTML if available
        if (doc.html() != null) {
            Path htmlPath = outputDir.resolve(safeName + "-docs.html");
            Files.writeString(htmlPath, doc.html());
            log.info("Saved HTML documentation to {}", htmlPath);
        }
    }

    // ============= Private Methods =============

    private void generateOverview(StringBuilder sb, JarAnalysisResult result) {
        sb.append("## Overview\n\n");

        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| **JAR Name** | ").append(result.getJarName()).append(" |\n");
        sb.append("| **Total Classes** | ").append(result.getClasses().size()).append(" |\n");

        int totalMethods = result.getClasses().stream()
                .mapToInt(c -> c.getMethods().size()).sum();
        sb.append("| **Total Methods** | ").append(totalMethods).append(" |\n");

        long packageCount = result.getClasses().stream()
                .map(ClassMetadata::getPackageName)
                .distinct().count();
        sb.append("| **Packages** | ").append(packageCount).append(" |\n");

        sb.append("\n");
    }

    private void generatePackageStructure(StringBuilder sb, List<ClassMetadata> classes) {
        sb.append("## Package Structure\n\n");

        Map<String, List<ClassMetadata>> byPackage = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getPackageName() != null ? c.getPackageName() : "(default)"));

        for (Map.Entry<String, List<ClassMetadata>> entry : byPackage.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n\n");
            for (ClassMetadata clazz : entry.getValue()) {
                String icon = getClassIcon(clazz);
                sb.append("- ").append(icon).append(" `").append(clazz.getClassName()).append("`\n");
            }
            sb.append("\n");
        }
    }

    private void generateComponentsSection(StringBuilder sb, List<ClassMetadata> classes) {
        sb.append("## Components\n\n");

        List<DetectedComponent> components = componentDetector.detectComponents(classes);
        Map<ComponentType, List<DetectedComponent>> byType = componentDetector.groupByType(components);

        for (Map.Entry<ComponentType, List<DetectedComponent>> entry : byType.entrySet()) {
            sb.append("### ").append(formatComponentType(entry.getKey())).append("\n\n");
            for (DetectedComponent comp : entry.getValue()) {
                sb.append("- **").append(comp.className()).append("**");
                if (!comp.injectedDependencies().isEmpty()) {
                    sb.append(" (").append(comp.injectedDependencies().size()).append(" dependencies)");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
    }

    private void generateEndpointsSection(StringBuilder sb, List<ClassMetadata> classes) {
        sb.append("## REST Endpoints\n\n");

        List<EndpointInfo> endpoints = endpointDetector.detectEndpoints(classes);

        if (endpoints.isEmpty()) {
            sb.append("*No REST endpoints detected.*\n\n");
            return;
        }

        Map<String, List<EndpointInfo>> byController = endpointDetector.groupByController(endpoints);

        for (Map.Entry<String, List<EndpointInfo>> entry : byController.entrySet()) {
            String controllerName = entry.getKey();
            if (controllerName.contains(".")) {
                controllerName = controllerName.substring(controllerName.lastIndexOf('.') + 1);
            }
            sb.append("### ").append(controllerName).append("\n\n");

            sb.append("| Method | Path | Handler |\n");
            sb.append("|--------|------|--------|\n");
            for (EndpointInfo endpoint : entry.getValue()) {
                sb.append("| `").append(endpoint.getHttpMethod()).append("` | `")
                        .append(endpoint.getPath()).append("` | `")
                        .append(endpoint.getMethodName()).append("()` |\n");
            }
            sb.append("\n");
        }
    }

    private void generateClassReference(StringBuilder sb, List<ClassMetadata> classes,
            DocOptions options) {
        sb.append("## Class Reference\n\n");

        for (ClassMetadata clazz : classes) {
            if (options.skipPrivateClasses() && !isPublicClass(clazz)) {
                continue;
            }

            sb.append("### ").append(clazz.getClassName()).append("\n\n");
            sb.append("**Package:** `").append(clazz.getPackageName()).append("`\n\n");

            // Quick method summary
            long publicMethods = clazz.getMethods().stream()
                    .filter(m -> m.getAccessModifiers().contains("public"))
                    .filter(m -> !m.getMethodName().startsWith("<"))
                    .count();

            sb.append("- **").append(publicMethods).append("** public methods\n");
            sb.append("- **").append(clazz.getFields().size()).append("** fields\n");
            sb.append("\n");
        }
    }

    private String convertToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>API Documentation</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; ");
        html.append("max-width: 900px; margin: 0 auto; padding: 20px; line-height: 1.6; }\n");
        html.append("h1 { border-bottom: 2px solid #2563eb; padding-bottom: 10px; }\n");
        html.append("h2 { color: #1f2937; margin-top: 30px; }\n");
        html.append("h3 { color: #374151; }\n");
        html.append("code { background: #f3f4f6; padding: 2px 6px; border-radius: 3px; }\n");
        html.append("pre { background: #1f2937; color: #f9fafb; padding: 15px; border-radius: 5px; ");
        html.append("overflow-x: auto; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 15px 0; }\n");
        html.append("th, td { border: 1px solid #e5e7eb; padding: 8px 12px; text-align: left; }\n");
        html.append("th { background: #f9fafb; }\n");
        html.append("</style>\n</head>\n<body>\n");

        // Simple markdown to HTML conversion
        String content = markdown
                .replaceAll("^# (.+)$", "<h1>$1</h1>")
                .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                .replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                .replaceAll("`([^`]+)`", "<code>$1</code>")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*([^*]+)\\*", "<em>$1</em>")
                .replaceAll("(?m)^- (.+)$", "<li>$1</li>")
                .replaceAll("(?s)(<li>.*?</li>)", "<ul>$1</ul>")
                .replaceAll("(?m)^---$", "<hr>");

        html.append(content);
        html.append("\n</body>\n</html>");
        return html.toString();
    }

    private String getClassIcon(ClassMetadata clazz) {
        return switch (clazz.getClassType()) {
            case INTERFACE -> "📘";
            case ENUM -> "📗";
            case ANNOTATION -> "📙";
            case ABSTRACT_CLASS -> "📕";
            default -> "📄";
        };
    }

    private String formatComponentType(ComponentType type) {
        return switch (type) {
            case SERVICE -> "🔧 Services";
            case REPOSITORY -> "🗄️ Repositories";
            case CONTROLLER -> "🌐 Controllers";
            case CONFIGURATION -> "⚙️ Configurations";
            case ENTITY -> "📦 Entities";
            case COMPONENT -> "🧩 Components";
        };
    }

    private String simplifyType(String type) {
        if (type == null)
            return "void";
        if (type.contains(".")) {
            return type.substring(type.lastIndexOf('.') + 1);
        }
        return type;
    }

    private String simplifyAnnotation(String annotation) {
        if (annotation.contains(".")) {
            int lastDot = annotation.lastIndexOf('.');
            int parenIdx = annotation.indexOf('(');
            if (parenIdx > 0) {
                return "@" + annotation.substring(lastDot + 1);
            }
            return "@" + annotation.substring(lastDot + 1);
        }
        return annotation.startsWith("@") ? annotation : "@" + annotation;
    }

    private String formatModifiers(Set<String> modifiers) {
        if (modifiers == null || modifiers.isEmpty())
            return "";
        return modifiers.stream().sorted().collect(Collectors.joining(" "));
    }

    private boolean isPublicClass(ClassMetadata clazz) {
        return clazz.getAccessModifiers() == null ||
                clazz.getAccessModifiers().contains("public");
    }

    // ============= Records =============

    public record GeneratedDocumentation(
            String markdown,
            String html,
            String jarName,
            LocalDateTime generatedAt) {
    }

    public record DocOptions(
            boolean includePackageStructure,
            boolean includeComponents,
            boolean includeEndpoints,
            boolean includeClassReference,
            boolean generateHtml,
            boolean skipPrivateClasses) {
        public static DocOptions full() {
            return new DocOptions(true, true, true, true, true, true);
        }

        public static DocOptions minimal() {
            return new DocOptions(false, true, true, false, false, true);
        }
    }
}

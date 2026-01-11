package com.codeexplainer.diagram;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.graph.DependencyGraphBuilder;
import com.codeexplainer.graph.DependencyGraphBuilder.*;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating visual diagrams using PlantUML.
 * Creates class diagrams, sequence diagrams, component diagrams, and more.
 */
@Service
public class DiagramGenerator {

    private static final Logger log = LoggerFactory.getLogger(DiagramGenerator.class);

    private final DependencyGraphBuilder graphBuilder;

    public DiagramGenerator(DependencyGraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    /**
     * Generates a class diagram from class metadata.
     */
    public DiagramResult generateClassDiagram(List<ClassMetadata> classes, DiagramOptions options) {
        log.info("Generating class diagram for {} classes", classes.size());

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("skinparam classAttributeIconSize 0\n");
        puml.append("skinparam classFontSize 12\n");
        puml.append("title Class Diagram\n\n");

        // Apply package grouping if requested
        if (options.groupByPackage()) {
            Map<String, List<ClassMetadata>> byPackage = classes.stream()
                    .collect(Collectors.groupingBy(c -> c.getPackageName() != null ? c.getPackageName() : "(default)"));

            for (Map.Entry<String, List<ClassMetadata>> entry : byPackage.entrySet()) {
                puml.append("package \"").append(entry.getKey()).append("\" {\n");
                for (ClassMetadata clazz : entry.getValue()) {
                    appendClassDefinition(puml, clazz, options);
                }
                puml.append("}\n\n");
            }
        } else {
            for (ClassMetadata clazz : classes) {
                appendClassDefinition(puml, clazz, options);
            }
        }

        // Add relationships
        puml.append("\n' Relationships\n");
        for (ClassMetadata clazz : classes) {
            String className = sanitizeName(clazz.getFullyQualifiedName());

            // Inheritance
            if (clazz.getSuperClassName() != null &&
                    !clazz.getSuperClassName().equals("java.lang.Object")) {
                String parentName = sanitizeName(clazz.getSuperClassName());
                if (hasClass(classes, clazz.getSuperClassName())) {
                    puml.append(className).append(" --|> ").append(parentName).append("\n");
                }
            }

            // Interfaces
            for (String iface : clazz.getInterfaces()) {
                if (hasClass(classes, iface) || options.showExternalDependencies()) {
                    puml.append(className).append(" ..|> ").append(sanitizeName(iface)).append("\n");
                }
            }
        }

        puml.append("@enduml\n");

        return new DiagramResult(puml.toString(), DiagramType.CLASS);
    }

    /**
     * Generates a sequence diagram for a method's call flow.
     */
    public DiagramResult generateSequenceDiagram(ClassMetadata startClass,
            String methodName,
            List<ClassMetadata> allClasses,
            int maxDepth) {
        log.info("Generating sequence diagram for {}.{}", startClass.getClassName(), methodName);

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Sequence: ").append(startClass.getClassName())
                .append(".").append(methodName).append("()\n\n");

        // Find the method
        MethodMetadata method = startClass.getMethods().stream()
                .filter(m -> m.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        if (method == null) {
            puml.append("note over Actor: Method not found\n");
        } else {
            Set<String> participants = new LinkedHashSet<>();
            participants.add(startClass.getClassName());

            // Collect all participants
            collectParticipants(method, allClasses, participants, 0, maxDepth);

            // Declare participants
            for (String participant : participants) {
                puml.append("participant \"").append(participant).append("\" as ")
                        .append(sanitizeName(participant)).append("\n");
            }
            puml.append("\n");

            // Generate sequence
            generateSequence(puml, startClass.getClassName(), method, allClasses, 0, maxDepth);
        }

        puml.append("@enduml\n");

        return new DiagramResult(puml.toString(), DiagramType.SEQUENCE);
    }

    /**
     * Generates a component diagram showing package dependencies.
     */
    public DiagramResult generateComponentDiagram(List<ClassMetadata> classes) {
        log.info("Generating component diagram");

        DependencyGraph packageGraph = graphBuilder.buildPackageGraph(classes);

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Package Dependencies\n\n");

        // Add components (packages)
        for (GraphNode node : packageGraph.nodes()) {
            puml.append("[").append(node.label()).append("] as ")
                    .append(sanitizeName(node.id())).append("\n");
        }

        puml.append("\n");

        // Add dependencies
        for (GraphEdge edge : packageGraph.edges()) {
            puml.append(sanitizeName(edge.source())).append(" --> ")
                    .append(sanitizeName(edge.target())).append("\n");
        }

        puml.append("@enduml\n");

        return new DiagramResult(puml.toString(), DiagramType.COMPONENT);
    }

    /**
     * Generates a package diagram with classes inside.
     */
    public DiagramResult generatePackageDiagram(List<ClassMetadata> classes) {
        log.info("Generating package diagram");

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Package Structure\n\n");

        Map<String, List<ClassMetadata>> byPackage = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getPackageName() != null ? c.getPackageName() : "(default)"));

        for (Map.Entry<String, List<ClassMetadata>> entry : byPackage.entrySet()) {
            puml.append("package \"").append(entry.getKey()).append("\" {\n");
            for (ClassMetadata clazz : entry.getValue()) {
                String type = switch (clazz.getClassType()) {
                    case INTERFACE -> "interface";
                    case ENUM -> "enum";
                    case ANNOTATION -> "annotation";
                    case ABSTRACT_CLASS -> "abstract class";
                    default -> "class";
                };
                puml.append("  ").append(type).append(" ").append(clazz.getClassName()).append("\n");
            }
            puml.append("}\n\n");
        }

        puml.append("@enduml\n");

        return new DiagramResult(puml.toString(), DiagramType.PACKAGE);
    }

    /**
     * Generates an inheritance hierarchy diagram.
     */
    public DiagramResult generateInheritanceDiagram(List<ClassMetadata> classes) {
        log.info("Generating inheritance diagram");

        DependencyGraph inheritanceGraph = graphBuilder.buildInheritanceGraph(classes);

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Inheritance Hierarchy\n\n");

        // Define classes
        for (GraphNode node : inheritanceGraph.nodes()) {
            String type = "class";
            Object nodeType = node.properties().get("type");
            if (nodeType != null) {
                String typeStr = nodeType.toString();
                if (typeStr.equals("INTERFACE"))
                    type = "interface";
                else if (typeStr.equals("ABSTRACT_CLASS"))
                    type = "abstract class";
                else if (typeStr.equals("ENUM"))
                    type = "enum";
            }
            puml.append(type).append(" ").append(node.label())
                    .append(" as ").append(sanitizeName(node.id())).append("\n");
        }

        puml.append("\n");

        // Add relationships
        for (GraphEdge edge : inheritanceGraph.edges()) {
            String arrow = edge.type() == EdgeType.INHERITANCE ? "--|>" : "..|>";
            puml.append(sanitizeName(edge.source())).append(" ").append(arrow)
                    .append(" ").append(sanitizeName(edge.target())).append("\n");
        }

        puml.append("@enduml\n");

        return new DiagramResult(puml.toString(), DiagramType.INHERITANCE);
    }

    /**
     * Renders PlantUML source to an image file.
     */
    public Path renderToImage(String pumlSource, Path outputPath, ImageFormat format) throws IOException {
        log.info("Rendering diagram to {} format", format);

        SourceStringReader reader = new SourceStringReader(pumlSource);
        FileFormat fileFormat = switch (format) {
            case PNG -> FileFormat.PNG;
            case SVG -> FileFormat.SVG;
            case EPS -> FileFormat.EPS;
        };

        try (OutputStream out = Files.newOutputStream(outputPath)) {
            reader.outputImage(out, new FileFormatOption(fileFormat));
        }

        return outputPath;
    }

    /**
     * Renders diagram and returns as byte array.
     */
    public byte[] renderToBytes(String pumlSource, ImageFormat format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SourceStringReader reader = new SourceStringReader(pumlSource);

        FileFormat fileFormat = switch (format) {
            case PNG -> FileFormat.PNG;
            case SVG -> FileFormat.SVG;
            case EPS -> FileFormat.EPS;
        };

        reader.outputImage(baos, new FileFormatOption(fileFormat));
        return baos.toByteArray();
    }

    // ============= Helper Methods =============

    private void appendClassDefinition(StringBuilder puml, ClassMetadata clazz, DiagramOptions options) {
        String classType = switch (clazz.getClassType()) {
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case ANNOTATION -> "annotation";
            case ABSTRACT_CLASS -> "abstract class";
            default -> "class";
        };

        String className = sanitizeName(clazz.getFullyQualifiedName());
        puml.append(classType).append(" \"").append(clazz.getClassName())
                .append("\" as ").append(className);

        // Check for stereotype annotations
        if (hasAnnotation(clazz, "Service")) {
            puml.append(" << Service >>");
        } else if (hasAnnotation(clazz, "Repository")) {
            puml.append(" << Repository >>");
        } else if (hasAnnotation(clazz, "Controller", "RestController")) {
            puml.append(" << Controller >>");
        } else if (hasAnnotation(clazz, "Component")) {
            puml.append(" << Component >>");
        }

        puml.append(" {\n");

        // Add fields if requested
        if (options.showFields()) {
            for (var field : clazz.getFields()) {
                String visibility = getVisibilitySymbol(field.getAccessModifiers());
                puml.append("  ").append(visibility).append(" ")
                        .append(simplifyType(field.getType())).append(" ")
                        .append(field.getFieldName()).append("\n");
            }
        }

        // Add methods if requested
        if (options.showMethods()) {
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getMethodName().startsWith("<"))
                    continue; // Skip constructors/clinit
                String visibility = getVisibilitySymbol(method.getAccessModifiers());
                puml.append("  ").append(visibility).append(" ")
                        .append(simplifyType(method.getReturnType())).append(" ")
                        .append(method.getMethodName()).append("()\n");
            }
        }

        puml.append("}\n\n");
    }

    private void collectParticipants(MethodMetadata method, List<ClassMetadata> allClasses,
            Set<String> participants, int depth, int maxDepth) {
        if (depth >= maxDepth || method.getInvocations() == null)
            return;

        for (MethodCall call : method.getInvocations()) {
            String targetClass = getSimpleClassName(call.getOwnerClass());
            participants.add(targetClass);

            // Find the target method and recurse
            ClassMetadata targetMetadata = findClass(allClasses, call.getOwnerClass());
            if (targetMetadata != null) {
                MethodMetadata targetMethod = targetMetadata.getMethods().stream()
                        .filter(m -> m.getMethodName().equals(call.getMethodName()))
                        .findFirst().orElse(null);
                if (targetMethod != null) {
                    collectParticipants(targetMethod, allClasses, participants, depth + 1, maxDepth);
                }
            }
        }
    }

    private void generateSequence(StringBuilder puml, String callerClass, MethodMetadata method,
            List<ClassMetadata> allClasses, int depth, int maxDepth) {
        if (method.getInvocations() == null || depth >= maxDepth)
            return;

        String caller = sanitizeName(callerClass);

        for (MethodCall call : method.getInvocations()) {
            String targetClass = getSimpleClassName(call.getOwnerClass());
            String target = sanitizeName(targetClass);

            puml.append(caller).append(" -> ").append(target).append(": ")
                    .append(call.getMethodName()).append("()\n");

            // Recurse if we have the target class
            ClassMetadata targetMetadata = findClass(allClasses, call.getOwnerClass());
            if (targetMetadata != null && depth < maxDepth - 1) {
                MethodMetadata targetMethod = targetMetadata.getMethods().stream()
                        .filter(m -> m.getMethodName().equals(call.getMethodName()))
                        .findFirst().orElse(null);
                if (targetMethod != null) {
                    puml.append("activate ").append(target).append("\n");
                    generateSequence(puml, targetClass, targetMethod, allClasses, depth + 1, maxDepth);
                    puml.append(target).append(" --> ").append(caller).append("\n");
                    puml.append("deactivate ").append(target).append("\n");
                }
            } else {
                puml.append(target).append(" --> ").append(caller).append("\n");
            }
        }
    }

    private boolean hasClass(List<ClassMetadata> classes, String fqn) {
        return classes.stream().anyMatch(c -> c.getFullyQualifiedName().equals(fqn));
    }

    private ClassMetadata findClass(List<ClassMetadata> classes, String fqn) {
        return classes.stream()
                .filter(c -> c.getFullyQualifiedName().equals(fqn))
                .findFirst().orElse(null);
    }

    private boolean hasAnnotation(ClassMetadata clazz, String... names) {
        for (String name : names) {
            if (clazz.getAnnotations().stream().anyMatch(a -> a.contains(name))) {
                return true;
            }
        }
        return false;
    }

    private String getVisibilitySymbol(Set<String> modifiers) {
        if (modifiers == null)
            return "~";
        if (modifiers.contains("public"))
            return "+";
        if (modifiers.contains("private"))
            return "-";
        if (modifiers.contains("protected"))
            return "#";
        return "~";
    }

    private String simplifyType(String type) {
        if (type == null)
            return "void";
        if (type.contains(".")) {
            type = type.substring(type.lastIndexOf('.') + 1);
        }
        return type.replace("[]", " []");
    }

    private String getSimpleClassName(String fqn) {
        if (fqn == null)
            return "Unknown";
        if (fqn.contains(".")) {
            return fqn.substring(fqn.lastIndexOf('.') + 1);
        }
        return fqn;
    }

    private String sanitizeName(String name) {
        if (name == null)
            return "Unknown";
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    // ============= Records and Enums =============

    public record DiagramResult(String source, DiagramType type) {
    }

    public record DiagramOptions(
            boolean groupByPackage,
            boolean showFields,
            boolean showMethods,
            boolean showExternalDependencies) {
        public static DiagramOptions defaults() {
            return new DiagramOptions(true, true, true, false);
        }

        public static DiagramOptions minimal() {
            return new DiagramOptions(false, false, false, false);
        }
    }

    public enum DiagramType {
        CLASS, SEQUENCE, COMPONENT, PACKAGE, INHERITANCE, ACTIVITY
    }

    public enum ImageFormat {
        PNG, SVG, EPS
    }
}

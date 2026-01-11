package com.codeexplainer.docs;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.FieldMetadata;
import com.codeexplainer.core.model.JarAnalysisResult;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.graph.DependencyGraphBuilder;
import com.codeexplainer.graph.DependencyGraphBuilder.DependencyGraph;
import com.codeexplainer.graph.DependencyGraphBuilder.GraphEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeexplainer.core.model.EndpointInfo;
import com.codeexplainer.detector.ComponentDetector;
import com.codeexplainer.detector.ComponentDetector.DetectedComponent;
import com.codeexplainer.detector.EndpointDetector;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating analysis reports from extracted metadata.
 * Produces statistical summaries, quality reports, and architecture overviews.
 */
@Service
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    private final DependencyGraphBuilder graphBuilder;
    private final EndpointDetector endpointDetector;
    private final ComponentDetector componentDetector;

    public ReportGenerator(DependencyGraphBuilder graphBuilder,
            EndpointDetector endpointDetector,
            ComponentDetector componentDetector) {
        this.graphBuilder = graphBuilder;
        this.endpointDetector = endpointDetector;
        this.componentDetector = componentDetector;
    }

    /**
     * Generates a comprehensive analysis report.
     */
    public AnalysisReport generateReport(JarAnalysisResult analysisResult) {
        log.info("Generating analysis report for {}", analysisResult.getJarName());

        List<ClassMetadata> classes = analysisResult.getClasses();

        // Calculate metrics
        ClassMetrics classMetrics = calculateClassMetrics(classes);
        MethodMetrics methodMetrics = calculateMethodMetrics(classes);
        DependencyMetrics dependencyMetrics = calculateDependencyMetrics(classes);
        List<QualityIssue> qualityIssues = detectQualityIssues(classes);

        // Detect architecture components
        List<EndpointInfo> endpoints = endpointDetector.detectEndpoints(classes);
        List<DetectedComponent> components = componentDetector.detectComponents(classes);
        ArchitectureOverview architecture = new ArchitectureOverview(components, endpoints);

        return new AnalysisReport(
                analysisResult.getJarName(),
                classMetrics,
                methodMetrics,
                dependencyMetrics,
                qualityIssues,
                architecture);
    }

    /**
     * Generates a markdown report.
     */
    public String generateMarkdownReport(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Analysis Report: ").append(report.jarName()).append("\n\n");

        // Class Metrics
        sb.append("## Class Metrics\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Total Classes | ").append(report.classMetrics().totalClasses()).append(" |\n");
        sb.append("| Interfaces | ").append(report.classMetrics().interfaces()).append(" |\n");
        sb.append("| Abstract Classes | ").append(report.classMetrics().abstractClasses()).append(" |\n");
        sb.append("| Enums | ").append(report.classMetrics().enums()).append(" |\n");
        sb.append("| Annotations | ").append(report.classMetrics().annotations()).append(" |\n");
        sb.append("| Packages | ").append(report.classMetrics().packages()).append(" |\n\n");

        // Method Metrics
        sb.append("## Method Metrics\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Total Methods | ").append(report.methodMetrics().totalMethods()).append(" |\n");
        sb.append("| Public Methods | ").append(report.methodMetrics().publicMethods()).append(" |\n");
        sb.append("| Private Methods | ").append(report.methodMetrics().privateMethods()).append(" |\n");
        sb.append("| Avg Methods/Class | ")
                .append(String.format("%.1f", report.methodMetrics().avgMethodsPerClass())).append(" |\n\n");

        // Dependency Metrics
        sb.append("## Dependency Analysis\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Inheritance Depth (Max) | ")
                .append(report.dependencyMetrics().maxInheritanceDepth()).append(" |\n");
        sb.append("| Avg Class Dependencies | ")
                .append(String.format("%.1f", report.dependencyMetrics().avgDependenciesPerClass())).append(" |\n");
        sb.append("| Circular Dependencies | ")
                .append(report.dependencyMetrics().circularDependencies()).append(" |\n\n");

        // Quality Issues
        if (!report.qualityIssues().isEmpty()) {
            sb.append("## Quality Issues\n\n");

            Map<IssueSeverity, List<QualityIssue>> bySeverity = report.qualityIssues().stream()
                    .collect(Collectors.groupingBy(QualityIssue::severity));

            for (IssueSeverity severity : IssueSeverity.values()) {
                List<QualityIssue> issues = bySeverity.getOrDefault(severity, List.of());
                if (!issues.isEmpty()) {
                    sb.append("### ").append(severity).append(" (")
                            .append(issues.size()).append(")\n\n");
                    for (QualityIssue issue : issues) {
                        sb.append("- **").append(issue.className()).append("**: ")
                                .append(issue.message()).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generates an architecture overview.
     */
    public String generateArchitectureOverview(List<ClassMetadata> classes) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Architecture Overview\n\n");

        // Layer analysis
        Map<String, List<ClassMetadata>> layers = analyzeArchitectureLayers(classes);

        sb.append("## Application Layers\n\n");
        for (Map.Entry<String, List<ClassMetadata>> entry : layers.entrySet()) {
            sb.append("### ").append(entry.getKey()).append(" (")
                    .append(entry.getValue().size()).append(" classes)\n\n");
            for (ClassMetadata clazz : entry.getValue()) {
                sb.append("- `").append(clazz.getClassName()).append("`\n");
            }
            sb.append("\n");
        }

        // Package dependencies
        sb.append("## Package Dependencies\n\n");
        DependencyGraph packageGraph = graphBuilder.buildPackageGraph(classes);

        if (!packageGraph.edges().isEmpty()) {
            sb.append("```\n");
            for (GraphEdge edge : packageGraph.edges()) {
                sb.append(simplifyPackage(edge.source())).append(" → ")
                        .append(simplifyPackage(edge.target())).append("\n");
            }
            sb.append("```\n\n");
        } else {
            sb.append("*No inter-package dependencies detected.*\n\n");
        }

        return sb.toString();
    }

    // ============= Private Methods =============

    private ClassMetrics calculateClassMetrics(List<ClassMetadata> classes) {
        int totalClasses = classes.size();
        int interfaces = (int) classes.stream()
                .filter(c -> c.getClassType() == ClassMetadata.ClassType.INTERFACE).count();
        int abstractClasses = (int) classes.stream()
                .filter(c -> c.getClassType() == ClassMetadata.ClassType.ABSTRACT_CLASS).count();
        int enums = (int) classes.stream()
                .filter(c -> c.getClassType() == ClassMetadata.ClassType.ENUM).count();
        int annotations = (int) classes.stream()
                .filter(c -> c.getClassType() == ClassMetadata.ClassType.ANNOTATION).count();
        int packages = (int) classes.stream()
                .map(ClassMetadata::getPackageName).distinct().count();

        return new ClassMetrics(totalClasses, interfaces, abstractClasses, enums, annotations, packages);
    }

    private MethodMetrics calculateMethodMetrics(List<ClassMetadata> classes) {
        int totalMethods = 0;
        int publicMethods = 0;
        int privateMethods = 0;

        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getMethodName().startsWith("<"))
                    continue;
                totalMethods++;
                if (method.getAccessModifiers().contains("public"))
                    publicMethods++;
                if (method.getAccessModifiers().contains("private"))
                    privateMethods++;
            }
        }

        double avgMethods = classes.isEmpty() ? 0 : (double) totalMethods / classes.size();

        return new MethodMetrics(totalMethods, publicMethods, privateMethods, avgMethods);
    }

    private DependencyMetrics calculateDependencyMetrics(List<ClassMetadata> classes) {
        // Calculate max inheritance depth
        int maxDepth = 0;
        for (ClassMetadata clazz : classes) {
            int depth = calculateInheritanceDepth(clazz, classes, 0);
            maxDepth = Math.max(maxDepth, depth);
        }

        // Calculate average dependencies
        int totalDeps = 0;
        for (ClassMetadata clazz : classes) {
            totalDeps += countDependencies(clazz);
        }
        double avgDeps = classes.isEmpty() ? 0 : (double) totalDeps / classes.size();

        // Check for circular dependencies
        DependencyGraph graph = graphBuilder.buildFullGraph(classes);
        int circularDeps = graphBuilder.findCircularDependencies(graph).size();

        return new DependencyMetrics(maxDepth, avgDeps, circularDeps);
    }

    private int calculateInheritanceDepth(ClassMetadata clazz, List<ClassMetadata> allClasses, int current) {
        if (clazz.getSuperClassName() == null ||
                clazz.getSuperClassName().equals("java.lang.Object")) {
            return current;
        }

        Optional<ClassMetadata> parent = allClasses.stream()
                .filter(c -> c.getFullyQualifiedName().equals(clazz.getSuperClassName()))
                .findFirst();

        if (parent.isPresent()) {
            return calculateInheritanceDepth(parent.get(), allClasses, current + 1);
        }

        return current + 1;
    }

    private int countDependencies(ClassMetadata clazz) {
        Set<String> deps = new HashSet<>();

        // From superclass
        if (clazz.getSuperClassName() != null) {
            deps.add(clazz.getSuperClassName());
        }

        // From interfaces
        deps.addAll(clazz.getInterfaces());

        // From fields
        for (FieldMetadata field : clazz.getFields()) {
            if (field.getType() != null) {
                deps.add(field.getType());
            }
        }

        return deps.size();
    }

    private List<QualityIssue> detectQualityIssues(List<ClassMetadata> classes) {
        List<QualityIssue> issues = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            // Large class detection
            if (clazz.getMethods().size() > 30) {
                issues.add(new QualityIssue(
                        clazz.getClassName(),
                        "Class has too many methods (" + clazz.getMethods().size() + ")",
                        IssueType.LARGE_CLASS,
                        IssueSeverity.WARNING));
            }

            // Too many fields
            if (clazz.getFields().size() > 15) {
                issues.add(new QualityIssue(
                        clazz.getClassName(),
                        "Class has too many fields (" + clazz.getFields().size() + ")",
                        IssueType.LARGE_CLASS,
                        IssueSeverity.WARNING));
            }

            // Deep inheritance
            int depth = calculateInheritanceDepth(clazz, classes, 0);
            if (depth > 4) {
                issues.add(new QualityIssue(
                        clazz.getClassName(),
                        "Deep inheritance hierarchy (depth: " + depth + ")",
                        IssueType.DEEP_INHERITANCE,
                        IssueSeverity.INFO));
            }
        }

        return issues;
    }

    private Map<String, List<ClassMetadata>> analyzeArchitectureLayers(List<ClassMetadata> classes) {
        Map<String, List<ClassMetadata>> layers = new LinkedHashMap<>();
        layers.put("Controllers", new ArrayList<>());
        layers.put("Services", new ArrayList<>());
        layers.put("Repositories", new ArrayList<>());
        layers.put("Entities", new ArrayList<>());
        layers.put("Configuration", new ArrayList<>());
        layers.put("Other", new ArrayList<>());

        for (ClassMetadata clazz : classes) {
            String layer = detectLayer(clazz);
            layers.get(layer).add(clazz);
        }

        // Remove empty layers
        layers.entrySet().removeIf(e -> e.getValue().isEmpty());

        return layers;
    }

    private String detectLayer(ClassMetadata clazz) {
        String className = clazz.getClassName();
        List<String> annotations = clazz.getAnnotations();

        if (annotations.stream().anyMatch(a -> a.contains("Controller")))
            return "Controllers";
        if (annotations.stream().anyMatch(a -> a.contains("Service")))
            return "Services";
        if (annotations.stream().anyMatch(a -> a.contains("Repository")))
            return "Repositories";
        if (annotations.stream().anyMatch(a -> a.contains("Entity")))
            return "Entities";
        if (annotations.stream().anyMatch(a -> a.contains("Configuration")))
            return "Configuration";

        if (className.endsWith("Controller"))
            return "Controllers";
        if (className.endsWith("Service"))
            return "Services";
        if (className.endsWith("Repository") || className.endsWith("Dao"))
            return "Repositories";

        return "Other";
    }

    private String simplifyPackage(String pkg) {
        if (pkg == null)
            return "(default)";
        String[] parts = pkg.split("\\.");
        if (parts.length > 2) {
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return pkg;
    }

    // ============= Records and Enums =============

    public record AnalysisReport(
            String jarName,
            ClassMetrics classMetrics,
            MethodMetrics methodMetrics,
            DependencyMetrics dependencyMetrics,
            List<QualityIssue> qualityIssues,
            ArchitectureOverview architecture) {
    }

    public record ArchitectureOverview(
            List<DetectedComponent> components,
            List<EndpointInfo> endpoints) {
    }

    public record ClassMetrics(
            int totalClasses,
            int interfaces,
            int abstractClasses,
            int enums,
            int annotations,
            int packages) {
    }

    public record MethodMetrics(
            int totalMethods,
            int publicMethods,
            int privateMethods,
            double avgMethodsPerClass) {
    }

    public record DependencyMetrics(
            int maxInheritanceDepth,
            double avgDependenciesPerClass,
            int circularDependencies) {
    }

    public record QualityIssue(
            String className,
            String message,
            IssueType type,
            IssueSeverity severity) {
    }

    public enum IssueType {
        LARGE_CLASS, DEEP_INHERITANCE, CIRCULAR_DEPENDENCY, HIGH_COUPLING
    }

    public enum IssueSeverity {
        ERROR, WARNING, INFO
    }
}

package com.codeexplainer.graph;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for building dependency graphs from class metadata.
 * Creates various graph representations for visualization and analysis.
 */
@Service
public class DependencyGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(DependencyGraphBuilder.class);

    /**
     * Builds a complete dependency graph from class metadata.
     * Includes class dependencies, method calls, and inheritance relationships.
     */
    public DependencyGraph buildFullGraph(List<ClassMetadata> classes) {
        log.info("Building dependency graph for {} classes", classes.size());

        Set<GraphNode> nodes = new HashSet<>();
        Set<GraphEdge> edges = new HashSet<>();

        Map<String, GraphNode> nodeMap = new HashMap<>();

        // Create nodes for all classes
        for (ClassMetadata classMetadata : classes) {
            GraphNode node = createClassNode(classMetadata);
            nodes.add(node);
            nodeMap.put(classMetadata.getFullyQualifiedName(), node);
        }

        // Create edges for dependencies
        for (ClassMetadata classMetadata : classes) {
            String sourceClass = classMetadata.getFullyQualifiedName();
            GraphNode sourceNode = nodeMap.get(sourceClass);

            // Inheritance edge
            if (classMetadata.getSuperClassName() != null &&
                    !classMetadata.getSuperClassName().equals("java.lang.Object")) {
                addEdge(edges, nodeMap, sourceClass, classMetadata.getSuperClassName(),
                        EdgeType.INHERITANCE, "extends");
            }

            // Interface implementation edges
            for (String iface : classMetadata.getInterfaces()) {
                addEdge(edges, nodeMap, sourceClass, iface, EdgeType.IMPLEMENTATION, "implements");
            }

            // Field type dependencies
            classMetadata.getFields().forEach(field -> {
                String fieldType = normalizeTypeName(field.getType());
                if (isProjectClass(fieldType, nodeMap)) {
                    addEdge(edges, nodeMap, sourceClass, fieldType, EdgeType.FIELD_DEPENDENCY,
                            "field: " + field.getFieldName());
                }
            });

            // Method invocation dependencies
            for (MethodMetadata method : classMetadata.getMethods()) {
                if (method.getInvocations() != null) {
                    for (MethodCall call : method.getInvocations()) {
                        String targetClass = normalizeTypeName(call.getOwnerClass());
                        if (isProjectClass(targetClass, nodeMap) && !targetClass.equals(sourceClass)) {
                            addEdge(edges, nodeMap, sourceClass, targetClass, EdgeType.METHOD_CALL,
                                    method.getMethodName() + " -> " + call.getMethodName());
                        }
                    }
                }
            }
        }

        return new DependencyGraph(nodes, edges, calculateStats(nodes, edges));
    }

    /**
     * Builds a package-level dependency graph.
     */
    public DependencyGraph buildPackageGraph(List<ClassMetadata> classes) {
        log.info("Building package dependency graph");

        Set<GraphNode> nodes = new HashSet<>();
        Set<GraphEdge> edges = new HashSet<>();
        Map<String, GraphNode> nodeMap = new HashMap<>();
        Map<String, Set<String>> packageDependencies = new HashMap<>();

        // Group classes by package
        Map<String, List<ClassMetadata>> packageClasses = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getPackageName() != null ? c.getPackageName() : "(default)"));

        // Create nodes for packages
        for (String packageName : packageClasses.keySet()) {
            List<ClassMetadata> pkgClasses = packageClasses.get(packageName);
            GraphNode node = new GraphNode(
                    packageName,
                    packageName,
                    NodeType.PACKAGE,
                    Map.of(
                            "classCount", pkgClasses.size(),
                            "classes", pkgClasses.stream()
                                    .map(ClassMetadata::getClassName)
                                    .collect(Collectors.toList())));
            nodes.add(node);
            nodeMap.put(packageName, node);
            packageDependencies.put(packageName, new HashSet<>());
        }

        // Build package dependencies from class dependencies
        for (ClassMetadata classMetadata : classes) {
            String sourcePackage = classMetadata.getPackageName() != null
                    ? classMetadata.getPackageName()
                    : "(default)";

            Set<String> deps = packageDependencies.get(sourcePackage);

            // From inheritance
            if (classMetadata.getSuperClassName() != null) {
                String targetPackage = getPackageName(classMetadata.getSuperClassName());
                if (!targetPackage.equals(sourcePackage) && packageClasses.containsKey(targetPackage)) {
                    deps.add(targetPackage);
                }
            }

            // From interfaces
            for (String iface : classMetadata.getInterfaces()) {
                String targetPackage = getPackageName(iface);
                if (!targetPackage.equals(sourcePackage) && packageClasses.containsKey(targetPackage)) {
                    deps.add(targetPackage);
                }
            }

            // From field types
            classMetadata.getFields().forEach(field -> {
                String targetPackage = getPackageName(field.getType());
                if (!targetPackage.equals(sourcePackage) && packageClasses.containsKey(targetPackage)) {
                    deps.add(targetPackage);
                }
            });

            // From method invocations
            for (MethodMetadata method : classMetadata.getMethods()) {
                if (method.getInvocations() != null) {
                    for (MethodCall call : method.getInvocations()) {
                        String targetPackage = getPackageName(call.getOwnerClass());
                        if (!targetPackage.equals(sourcePackage) && packageClasses.containsKey(targetPackage)) {
                            deps.add(targetPackage);
                        }
                    }
                }
            }
        }

        // Create edges for package dependencies
        for (Map.Entry<String, Set<String>> entry : packageDependencies.entrySet()) {
            String sourcePackage = entry.getKey();
            for (String targetPackage : entry.getValue()) {
                edges.add(new GraphEdge(
                        sourcePackage + "->" + targetPackage,
                        sourcePackage,
                        targetPackage,
                        EdgeType.PACKAGE_DEPENDENCY,
                        "depends on",
                        1));
            }
        }

        return new DependencyGraph(nodes, edges, calculateStats(nodes, edges));
    }

    /**
     * Builds a class hierarchy graph (inheritance only).
     */
    public DependencyGraph buildInheritanceGraph(List<ClassMetadata> classes) {
        Set<GraphNode> nodes = new HashSet<>();
        Set<GraphEdge> edges = new HashSet<>();
        Map<String, GraphNode> nodeMap = new HashMap<>();

        for (ClassMetadata classMetadata : classes) {
            GraphNode node = createClassNode(classMetadata);
            nodes.add(node);
            nodeMap.put(classMetadata.getFullyQualifiedName(), node);
        }

        for (ClassMetadata classMetadata : classes) {
            String sourceClass = classMetadata.getFullyQualifiedName();

            if (classMetadata.getSuperClassName() != null &&
                    !classMetadata.getSuperClassName().equals("java.lang.Object")) {
                addEdge(edges, nodeMap, sourceClass, classMetadata.getSuperClassName(),
                        EdgeType.INHERITANCE, "extends");
            }

            for (String iface : classMetadata.getInterfaces()) {
                addEdge(edges, nodeMap, sourceClass, iface, EdgeType.IMPLEMENTATION, "implements");
            }
        }

        return new DependencyGraph(nodes, edges, calculateStats(nodes, edges));
    }

    /**
     * Builds a method call graph.
     */
    public DependencyGraph buildCallGraph(List<ClassMetadata> classes) {
        Set<GraphNode> nodes = new HashSet<>();
        Set<GraphEdge> edges = new HashSet<>();
        Map<String, GraphNode> nodeMap = new HashMap<>();

        // Create nodes for methods
        for (ClassMetadata classMetadata : classes) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                String methodId = classMetadata.getFullyQualifiedName() + "." + method.getMethodName();
                GraphNode node = new GraphNode(
                        methodId,
                        classMetadata.getClassName() + "." + method.getMethodName(),
                        NodeType.METHOD,
                        Map.of(
                                "class", classMetadata.getFullyQualifiedName(),
                                "returnType", method.getReturnType() != null ? method.getReturnType() : "void",
                                "parameterCount", method.getParameters().size()));
                nodes.add(node);
                nodeMap.put(methodId, node);
            }
        }

        // Create edges for method calls
        for (ClassMetadata classMetadata : classes) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                String sourceMethod = classMetadata.getFullyQualifiedName() + "." + method.getMethodName();

                if (method.getInvocations() != null) {
                    for (MethodCall call : method.getInvocations()) {
                        String targetMethod = call.getOwnerClass() + "." + call.getMethodName();
                        if (nodeMap.containsKey(targetMethod)) {
                            edges.add(new GraphEdge(
                                    sourceMethod + "->" + targetMethod,
                                    sourceMethod,
                                    targetMethod,
                                    EdgeType.METHOD_CALL,
                                    "calls",
                                    1));
                        }
                    }
                }
            }
        }

        return new DependencyGraph(nodes, edges, calculateStats(nodes, edges));
    }

    /**
     * Finds circular dependencies in the graph.
     */
    public List<List<String>> findCircularDependencies(DependencyGraph graph) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);

        for (GraphNode node : graph.nodes()) {
            if (!visited.contains(node.id())) {
                List<String> path = new ArrayList<>();
                findCyclesDFS(node.id(), adjacencyList, visited, recursionStack, path, cycles);
            }
        }

        return cycles;
    }

    /**
     * Calculates various metrics for the dependency graph.
     */
    public GraphMetrics calculateMetrics(DependencyGraph graph) {
        int nodeCount = graph.nodes().size();
        int edgeCount = graph.edges().size();

        // Calculate in-degree and out-degree for each node
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();

        for (GraphNode node : graph.nodes()) {
            inDegree.put(node.id(), 0);
            outDegree.put(node.id(), 0);
        }

        for (GraphEdge edge : graph.edges()) {
            outDegree.merge(edge.source(), 1, Integer::sum);
            inDegree.merge(edge.target(), 1, Integer::sum);
        }

        // Find nodes with high coupling
        List<String> highlyConnected = new ArrayList<>();
        int threshold = Math.max(3, edgeCount / nodeCount);
        for (GraphNode node : graph.nodes()) {
            int total = inDegree.getOrDefault(node.id(), 0) + outDegree.getOrDefault(node.id(), 0);
            if (total >= threshold) {
                highlyConnected.add(node.id());
            }
        }

        // Find orphan nodes (no connections)
        List<String> orphans = new ArrayList<>();
        for (GraphNode node : graph.nodes()) {
            if (inDegree.getOrDefault(node.id(), 0) == 0 &&
                    outDegree.getOrDefault(node.id(), 0) == 0) {
                orphans.add(node.id());
            }
        }

        double density = nodeCount > 1
                ? (double) edgeCount / (nodeCount * (nodeCount - 1))
                : 0;

        return new GraphMetrics(
                nodeCount,
                edgeCount,
                density,
                highlyConnected,
                orphans,
                findCircularDependencies(graph));
    }

    // ============= Helper Methods =============

    private GraphNode createClassNode(ClassMetadata classMetadata) {
        return new GraphNode(
                classMetadata.getFullyQualifiedName(),
                classMetadata.getClassName(),
                NodeType.CLASS,
                Map.of(
                        "package", classMetadata.getPackageName() != null ? classMetadata.getPackageName() : "",
                        "type", classMetadata.getClassType().name(),
                        "methodCount", classMetadata.getMethods().size(),
                        "fieldCount", classMetadata.getFields().size()));
    }

    private void addEdge(Set<GraphEdge> edges, Map<String, GraphNode> nodeMap,
            String source, String target, EdgeType type, String label) {
        if (nodeMap.containsKey(target)) {
            edges.add(new GraphEdge(
                    source + "->" + target,
                    source,
                    target,
                    type,
                    label,
                    1));
        }
    }

    private boolean isProjectClass(String className, Map<String, GraphNode> nodeMap) {
        return nodeMap.containsKey(className);
    }

    private String normalizeTypeName(String typeName) {
        if (typeName == null)
            return "";
        // Remove array notation
        typeName = typeName.replace("[]", "");
        // Remove generics
        int genericIndex = typeName.indexOf('<');
        if (genericIndex > 0) {
            typeName = typeName.substring(0, genericIndex);
        }
        return typeName;
    }

    private String getPackageName(String fullyQualifiedName) {
        if (fullyQualifiedName == null)
            return "(default)";
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(0, lastDot) : "(default)";
    }

    private Map<String, List<String>> buildAdjacencyList(DependencyGraph graph) {
        Map<String, List<String>> adjacencyList = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            adjacencyList.put(node.id(), new ArrayList<>());
        }
        for (GraphEdge edge : graph.edges()) {
            adjacencyList.computeIfAbsent(edge.source(), k -> new ArrayList<>()).add(edge.target());
        }
        return adjacencyList;
    }

    private void findCyclesDFS(String node, Map<String, List<String>> adjacencyList,
            Set<String> visited, Set<String> recursionStack,
            List<String> path, List<List<String>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        path.add(node);

        for (String neighbor : adjacencyList.getOrDefault(node, List.of())) {
            if (!visited.contains(neighbor)) {
                findCyclesDFS(neighbor, adjacencyList, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a cycle
                int cycleStart = path.indexOf(neighbor);
                if (cycleStart >= 0) {
                    List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycle.add(neighbor); // Complete the cycle
                    cycles.add(cycle);
                }
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(node);
    }

    private GraphStats calculateStats(Set<GraphNode> nodes, Set<GraphEdge> edges) {
        Map<NodeType, Long> nodesByType = nodes.stream()
                .collect(Collectors.groupingBy(GraphNode::type, Collectors.counting()));
        Map<EdgeType, Long> edgesByType = edges.stream()
                .collect(Collectors.groupingBy(GraphEdge::type, Collectors.counting()));
        return new GraphStats(nodes.size(), edges.size(), nodesByType, edgesByType);
    }

    // ============= Records and Enums =============

    public record DependencyGraph(
            Set<GraphNode> nodes,
            Set<GraphEdge> edges,
            GraphStats stats) {
    }

    public record GraphNode(
            String id,
            String label,
            NodeType type,
            Map<String, Object> properties) {
    }

    public record GraphEdge(
            String id,
            String source,
            String target,
            EdgeType type,
            String label,
            int weight) {
    }

    public record GraphStats(
            int nodeCount,
            int edgeCount,
            Map<NodeType, Long> nodesByType,
            Map<EdgeType, Long> edgesByType) {
    }

    public record GraphMetrics(
            int nodeCount,
            int edgeCount,
            double density,
            List<String> highlyConnectedNodes,
            List<String> orphanNodes,
            List<List<String>> circularDependencies) {
    }

    public enum NodeType {
        CLASS, INTERFACE, ENUM, ANNOTATION, PACKAGE, METHOD
    }

    public enum EdgeType {
        INHERITANCE, IMPLEMENTATION, FIELD_DEPENDENCY, METHOD_CALL, PACKAGE_DEPENDENCY
    }
}

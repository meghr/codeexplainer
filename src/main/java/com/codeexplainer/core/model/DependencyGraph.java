package com.codeexplainer.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the dependency graph of classes within the JAR.
 */
public class DependencyGraph {

    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();
    private Map<String, List<String>> classDependencies = new HashMap<>();
    private Map<String, List<String>> packageDependencies = new HashMap<>();
    private List<String> circularDependencies = new ArrayList<>();

    /**
     * Represents a node in the dependency graph (a class or package)
     */
    public static class GraphNode {
        private String id;
        private String label;
        private NodeType type;
        private String packageName;
        private Map<String, Object> metadata = new HashMap<>();

        public enum NodeType {
            CLASS,
            INTERFACE,
            PACKAGE,
            EXTERNAL
        }

        public GraphNode() {
        }

        public GraphNode(String id, String label, NodeType type) {
            this.id = id;
            this.label = label;
            this.type = type;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public NodeType getType() {
            return type;
        }

        public void setType(NodeType type) {
            this.type = type;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * Represents an edge (dependency) in the graph
     */
    public static class GraphEdge {
        private String source;
        private String target;
        private EdgeType type;
        private String label;

        public enum EdgeType {
            EXTENDS,
            IMPLEMENTS,
            USES,
            AGGREGATES,
            CREATES_INSTANCE
        }

        public GraphEdge() {
        }

        public GraphEdge(String source, String target, EdgeType type) {
            this.source = source;
            this.target = target;
            this.type = type;
        }

        // Getters and Setters
        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public EdgeType getType() {
            return type;
        }

        public void setType(EdgeType type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    // Getters and Setters
    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdge> edges) {
        this.edges = edges;
    }

    public Map<String, List<String>> getClassDependencies() {
        return classDependencies;
    }

    public void setClassDependencies(Map<String, List<String>> classDependencies) {
        this.classDependencies = classDependencies;
    }

    public Map<String, List<String>> getPackageDependencies() {
        return packageDependencies;
    }

    public void setPackageDependencies(Map<String, List<String>> packageDependencies) {
        this.packageDependencies = packageDependencies;
    }

    public List<String> getCircularDependencies() {
        return circularDependencies;
    }

    public void setCircularDependencies(List<String> circularDependencies) {
        this.circularDependencies = circularDependencies;
    }

    public void addNode(GraphNode node) {
        this.nodes.add(node);
    }

    public void addEdge(GraphEdge edge) {
        this.edges.add(edge);
    }
}

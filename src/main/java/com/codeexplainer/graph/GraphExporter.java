package com.codeexplainer.graph;

import com.codeexplainer.graph.DependencyGraphBuilder.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting dependency graphs to various formats.
 * Supports DOT (Graphviz), JSON, PlantUML, and Mermaid formats.
 */
@Service
public class GraphExporter {

    /**
     * Exports the graph to DOT format for Graphviz.
     */
    public String exportToDot(DependencyGraph graph, String graphName) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(sanitizeName(graphName)).append(" {\n");
        sb.append("    rankdir=TB;\n");
        sb.append("    node [shape=box, style=filled];\n\n");

        // Add nodes with styling based on type
        for (GraphNode node : graph.nodes()) {
            sb.append("    \"").append(node.id()).append("\" [");
            sb.append("label=\"").append(escapeLabel(node.label())).append("\"");
            sb.append(", fillcolor=\"").append(getNodeColor(node.type())).append("\"");
            sb.append("];\n");
        }

        sb.append("\n");

        // Add edges with styling based on type
        for (GraphEdge edge : graph.edges()) {
            sb.append("    \"").append(edge.source()).append("\" -> \"")
                    .append(edge.target()).append("\" [");
            sb.append("style=\"").append(getEdgeStyle(edge.type())).append("\"");
            sb.append(", color=\"").append(getEdgeColor(edge.type())).append("\"");
            if (edge.label() != null && !edge.label().isEmpty()) {
                sb.append(", label=\"").append(escapeLabel(edge.label())).append("\"");
            }
            sb.append("];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Exports the graph to JSON format.
     */
    public String exportToJson(DependencyGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Nodes
        sb.append("  \"nodes\": [\n");
        List<GraphNode> nodeList = new ArrayList<>(graph.nodes());
        for (int i = 0; i < nodeList.size(); i++) {
            GraphNode node = nodeList.get(i);
            sb.append("    {\"id\": \"").append(escapeJson(node.id())).append("\", ");
            sb.append("\"label\": \"").append(escapeJson(node.label())).append("\", ");
            sb.append("\"type\": \"").append(node.type()).append("\", ");
            sb.append("\"properties\": ").append(mapToJson(node.properties())).append("}");
            if (i < nodeList.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Edges
        sb.append("  \"edges\": [\n");
        List<GraphEdge> edgeList = new ArrayList<>(graph.edges());
        for (int i = 0; i < edgeList.size(); i++) {
            GraphEdge edge = edgeList.get(i);
            sb.append("    {\"id\": \"").append(escapeJson(edge.id())).append("\", ");
            sb.append("\"source\": \"").append(escapeJson(edge.source())).append("\", ");
            sb.append("\"target\": \"").append(escapeJson(edge.target())).append("\", ");
            sb.append("\"type\": \"").append(edge.type()).append("\", ");
            sb.append("\"label\": \"").append(escapeJson(edge.label())).append("\", ");
            sb.append("\"weight\": ").append(edge.weight()).append("}");
            if (i < edgeList.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Stats
        sb.append("  \"stats\": {");
        sb.append("\"nodeCount\": ").append(graph.stats().nodeCount()).append(", ");
        sb.append("\"edgeCount\": ").append(graph.stats().edgeCount()).append("}\n");

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Exports the graph to PlantUML format.
     */
    public String exportToPlantUml(DependencyGraph graph, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("title ").append(title).append("\n\n");

        // Separate by node type
        Map<NodeType, List<GraphNode>> nodesByType = graph.nodes().stream()
                .collect(Collectors.groupingBy(GraphNode::type));

        // Define classes/interfaces
        for (GraphNode node : nodesByType.getOrDefault(NodeType.INTERFACE, List.of())) {
            sb.append("interface \"").append(node.label()).append("\" as ")
                    .append(sanitizeName(node.id())).append("\n");
        }

        for (GraphNode node : nodesByType.getOrDefault(NodeType.CLASS, List.of())) {
            sb.append("class \"").append(node.label()).append("\" as ")
                    .append(sanitizeName(node.id())).append("\n");
        }

        for (GraphNode node : nodesByType.getOrDefault(NodeType.ENUM, List.of())) {
            sb.append("enum \"").append(node.label()).append("\" as ")
                    .append(sanitizeName(node.id())).append("\n");
        }

        for (GraphNode node : nodesByType.getOrDefault(NodeType.PACKAGE, List.of())) {
            sb.append("package \"").append(node.label()).append("\" as ")
                    .append(sanitizeName(node.id())).append(" {\n}\n");
        }

        sb.append("\n");

        // Add relationships
        for (GraphEdge edge : graph.edges()) {
            String sourceId = sanitizeName(edge.source());
            String targetId = sanitizeName(edge.target());
            String arrow = getPlantUmlArrow(edge.type());
            sb.append(sourceId).append(" ").append(arrow).append(" ").append(targetId);
            if (edge.type() == EdgeType.IMPLEMENTATION) {
                sb.append(" : implements");
            }
            sb.append("\n");
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    /**
     * Exports the graph to Mermaid format.
     */
    public String exportToMermaid(DependencyGraph graph, String direction) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("graph ").append(direction != null ? direction : "TD").append("\n");

        // Define nodes with styling
        for (GraphNode node : graph.nodes()) {
            String nodeId = sanitizeMermaidId(node.id());
            String shape = getMermaidShape(node.type());
            sb.append("    ").append(nodeId).append(shape.charAt(0))
                    .append("\"").append(node.label()).append("\"").append(shape.charAt(1)).append("\n");
        }

        sb.append("\n");

        // Add edges
        for (GraphEdge edge : graph.edges()) {
            String sourceId = sanitizeMermaidId(edge.source());
            String targetId = sanitizeMermaidId(edge.target());
            String arrow = getMermaidArrow(edge.type());
            sb.append("    ").append(sourceId).append(" ").append(arrow).append(" ").append(targetId);
            if (edge.label() != null && !edge.label().isEmpty() && edge.label().length() < 20) {
                sb.append(" : ").append(edge.label());
            }
            sb.append("\n");
        }

        // Add styling classes
        sb.append("\n");
        sb.append("    classDef interface fill:#9cf,stroke:#36f\n");
        sb.append("    classDef class fill:#fc9,stroke:#f60\n");
        sb.append("    classDef package fill:#9f9,stroke:#090\n");

        sb.append("```\n");
        return sb.toString();
    }

    /**
     * Exports graph metrics as a formatted report.
     */
    public String exportMetricsReport(GraphMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Dependency Graph Metrics Report\n\n");

        sb.append("## Summary\n");
        sb.append("- **Total Nodes:** ").append(metrics.nodeCount()).append("\n");
        sb.append("- **Total Edges:** ").append(metrics.edgeCount()).append("\n");
        sb.append("- **Graph Density:** ").append(String.format("%.4f", metrics.density())).append("\n");
        sb.append("\n");

        if (!metrics.highlyConnectedNodes().isEmpty()) {
            sb.append("## Highly Connected Nodes (Potential Hotspots)\n");
            for (String node : metrics.highlyConnectedNodes()) {
                sb.append("- ").append(node).append("\n");
            }
            sb.append("\n");
        }

        if (!metrics.orphanNodes().isEmpty()) {
            sb.append("## Orphan Nodes (No Connections)\n");
            for (String node : metrics.orphanNodes()) {
                sb.append("- ").append(node).append("\n");
            }
            sb.append("\n");
        }

        if (!metrics.circularDependencies().isEmpty()) {
            sb.append("## Circular Dependencies ⚠️\n");
            for (List<String> cycle : metrics.circularDependencies()) {
                sb.append("- ").append(String.join(" → ", cycle)).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ============= Helper Methods =============

    private String getNodeColor(NodeType type) {
        return switch (type) {
            case CLASS -> "#ffcc99";
            case INTERFACE -> "#99ccff";
            case ENUM -> "#cc99ff";
            case ANNOTATION -> "#ffff99";
            case PACKAGE -> "#99ff99";
            case METHOD -> "#ff9999";
        };
    }

    private String getEdgeStyle(EdgeType type) {
        return switch (type) {
            case INHERITANCE -> "solid";
            case IMPLEMENTATION -> "dashed";
            case FIELD_DEPENDENCY -> "dotted";
            case METHOD_CALL -> "solid";
            case PACKAGE_DEPENDENCY -> "bold";
        };
    }

    private String getEdgeColor(EdgeType type) {
        return switch (type) {
            case INHERITANCE -> "#0066cc";
            case IMPLEMENTATION -> "#009900";
            case FIELD_DEPENDENCY -> "#666666";
            case METHOD_CALL -> "#cc6600";
            case PACKAGE_DEPENDENCY -> "#990099";
        };
    }

    private String getPlantUmlArrow(EdgeType type) {
        return switch (type) {
            case INHERITANCE -> "--|>";
            case IMPLEMENTATION -> "..|>";
            case FIELD_DEPENDENCY -> "-->";
            case METHOD_CALL -> "-[#orange]->";
            case PACKAGE_DEPENDENCY -> "-->";
        };
    }

    private String getMermaidShape(NodeType type) {
        return switch (type) {
            case INTERFACE -> "([])";
            case CLASS -> "[]";
            case ENUM -> "{}";
            case ANNOTATION -> "(())";
            case PACKAGE -> "[[]]";
            case METHOD -> "()";
        };
    }

    private String getMermaidArrow(EdgeType type) {
        return switch (type) {
            case INHERITANCE -> "-->|extends|";
            case IMPLEMENTATION -> "-.->|implements|";
            case FIELD_DEPENDENCY -> "--->";
            case METHOD_CALL -> "-->|calls|";
            case PACKAGE_DEPENDENCY -> "==>";
        };
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String sanitizeMermaidId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private String escapeLabel(String label) {
        if (label == null)
            return "";
        return label.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty())
            return "{}";
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0)
                sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Collection) {
                sb.append("[");
                int j = 0;
                for (Object item : (Collection<?>) value) {
                    if (j > 0)
                        sb.append(", ");
                    sb.append("\"").append(escapeJson(item.toString())).append("\"");
                    j++;
                }
                sb.append("]");
            } else {
                sb.append(value);
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}

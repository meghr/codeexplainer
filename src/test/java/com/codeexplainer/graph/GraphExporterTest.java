package com.codeexplainer.graph;

import com.codeexplainer.graph.DependencyGraphBuilder.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GraphExporter.
 */
@DisplayName("GraphExporter Tests")
class GraphExporterTest {

    private GraphExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new GraphExporter();
    }

    @Nested
    @DisplayName("DOT Export")
    class DotExportTests {

        @Test
        @DisplayName("Should export to DOT format")
        void shouldExportToDot() {
            DependencyGraph graph = createSampleGraph();

            String dot = exporter.exportToDot(graph, "TestGraph");

            assertNotNull(dot);
            assertTrue(dot.contains("digraph"));
            assertTrue(dot.contains("TestGraph"));
        }

        @Test
        @DisplayName("Should include nodes in DOT")
        void shouldIncludeNodesInDot() {
            DependencyGraph graph = createSampleGraph();

            String dot = exporter.exportToDot(graph, "TestGraph");

            assertTrue(dot.contains("ClassA"));
            assertTrue(dot.contains("ClassB"));
        }

        @Test
        @DisplayName("Should include edges in DOT")
        void shouldIncludeEdgesInDot() {
            DependencyGraph graph = createSampleGraph();

            String dot = exporter.exportToDot(graph, "TestGraph");

            assertTrue(dot.contains("->"));
        }
    }

    @Nested
    @DisplayName("JSON Export")
    class JsonExportTests {

        @Test
        @DisplayName("Should export to JSON format")
        void shouldExportToJson() {
            DependencyGraph graph = createSampleGraph();

            String json = exporter.exportToJson(graph);

            assertNotNull(json);
            assertTrue(json.contains("\"nodes\""));
            assertTrue(json.contains("\"edges\""));
        }

        @Test
        @DisplayName("Should include stats in JSON")
        void shouldIncludeStatsInJson() {
            DependencyGraph graph = createSampleGraph();

            String json = exporter.exportToJson(graph);

            assertTrue(json.contains("\"stats\""));
            assertTrue(json.contains("\"nodeCount\""));
        }
    }

    @Nested
    @DisplayName("PlantUML Export")
    class PlantUmlExportTests {

        @Test
        @DisplayName("Should export to PlantUML format")
        void shouldExportToPlantUml() {
            DependencyGraph graph = createSampleGraph();

            String plantuml = exporter.exportToPlantUml(graph, "Test Diagram");

            assertNotNull(plantuml);
            assertTrue(plantuml.contains("@startuml"));
            assertTrue(plantuml.contains("@enduml"));
        }

        @Test
        @DisplayName("Should include title in PlantUML")
        void shouldIncludeTitleInPlantUml() {
            DependencyGraph graph = createSampleGraph();

            String plantuml = exporter.exportToPlantUml(graph, "My Title");

            assertTrue(plantuml.contains("title My Title"));
        }
    }

    @Nested
    @DisplayName("Mermaid Export")
    class MermaidExportTests {

        @Test
        @DisplayName("Should export to Mermaid format")
        void shouldExportToMermaid() {
            DependencyGraph graph = createSampleGraph();

            String mermaid = exporter.exportToMermaid(graph, "TD");

            assertNotNull(mermaid);
            assertTrue(mermaid.contains("```mermaid"));
            assertTrue(mermaid.contains("graph TD"));
        }

        @Test
        @DisplayName("Should support different directions")
        void shouldSupportDifferentDirections() {
            DependencyGraph graph = createSampleGraph();

            String lrMermaid = exporter.exportToMermaid(graph, "LR");

            assertTrue(lrMermaid.contains("graph LR"));
        }
    }

    @Nested
    @DisplayName("Metrics Report")
    class MetricsReportTests {

        @Test
        @DisplayName("Should export metrics report")
        void shouldExportMetricsReport() {
            GraphMetrics metrics = new GraphMetrics(
                    10, 15, 0.35,
                    List.of("HighlyConnected1", "HighlyConnected2"),
                    List.of("Orphan1"),
                    List.of(List.of("A", "B", "A")));

            String report = exporter.exportMetricsReport(metrics);

            assertNotNull(report);
            assertTrue(report.contains("# Dependency Graph Metrics Report"));
            assertTrue(report.contains("Total Nodes"));
            assertTrue(report.contains("Total Edges"));
        }

        @Test
        @DisplayName("Should include circular dependencies warning")
        void shouldIncludeCircularDependencies() {
            GraphMetrics metrics = new GraphMetrics(
                    10, 15, 0.35,
                    List.of(),
                    List.of(),
                    List.of(List.of("ClassA", "ClassB", "ClassA")));

            String report = exporter.exportMetricsReport(metrics);

            assertTrue(report.contains("Circular Dependencies"));
            assertTrue(report.contains("ClassA"));
        }
    }

    // ============= Helper Methods =============

    private DependencyGraph createSampleGraph() {
        Set<GraphNode> nodes = new HashSet<>();
        nodes.add(new GraphNode("com.example.ClassA", "ClassA", NodeType.CLASS, Map.of()));
        nodes.add(new GraphNode("com.example.ClassB", "ClassB", NodeType.CLASS, Map.of()));
        nodes.add(new GraphNode("com.example.IService", "IService", NodeType.INTERFACE, Map.of()));

        Set<GraphEdge> edges = new HashSet<>();
        edges.add(new GraphEdge("edge1", "com.example.ClassA", "com.example.ClassB",
                EdgeType.METHOD_CALL, "calls", 1));
        edges.add(new GraphEdge("edge2", "com.example.ClassA", "com.example.IService",
                EdgeType.IMPLEMENTATION, "implements", 1));

        GraphStats stats = new GraphStats(3, 2, Map.of(), Map.of());

        return new DependencyGraph(nodes, edges, stats);
    }
}

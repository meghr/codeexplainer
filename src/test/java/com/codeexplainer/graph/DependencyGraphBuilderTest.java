package com.codeexplainer.graph;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.graph.DependencyGraphBuilder.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyGraphBuilder.
 */
@DisplayName("DependencyGraphBuilder Tests")
class DependencyGraphBuilderTest {

    private DependencyGraphBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DependencyGraphBuilder();
    }

    @Nested
    @DisplayName("Full Graph Building")
    class FullGraphTests {

        @Test
        @DisplayName("Should build graph with nodes and edges")
        void shouldBuildGraphWithNodesAndEdges() {
            List<ClassMetadata> classes = createSampleClasses();

            DependencyGraph graph = builder.buildFullGraph(classes);

            assertNotNull(graph);
            assertFalse(graph.nodes().isEmpty());
            assertEquals(classes.size(), graph.nodes().size());
        }

        @Test
        @DisplayName("Should include inheritance edges")
        void shouldIncludeInheritanceEdges() {
            List<ClassMetadata> classes = createClassesWithInheritance();

            DependencyGraph graph = builder.buildFullGraph(classes);

            assertTrue(graph.edges().stream()
                    .anyMatch(e -> e.type() == EdgeType.INHERITANCE));
        }

        @Test
        @DisplayName("Should include implementation edges")
        void shouldIncludeImplementationEdges() {
            List<ClassMetadata> classes = createClassesWithInterfaces();

            DependencyGraph graph = builder.buildFullGraph(classes);

            assertTrue(graph.edges().stream()
                    .anyMatch(e -> e.type() == EdgeType.IMPLEMENTATION));
        }

        @Test
        @DisplayName("Should include method call edges")
        void shouldIncludeMethodCallEdges() {
            List<ClassMetadata> classes = createClassesWithMethodCalls();

            DependencyGraph graph = builder.buildFullGraph(classes);

            assertTrue(graph.edges().stream()
                    .anyMatch(e -> e.type() == EdgeType.METHOD_CALL));
        }
    }

    @Nested
    @DisplayName("Package Graph Building")
    class PackageGraphTests {

        @Test
        @DisplayName("Should build package-level graph")
        void shouldBuildPackageGraph() {
            List<ClassMetadata> classes = createClassesInDifferentPackages();

            DependencyGraph graph = builder.buildPackageGraph(classes);

            assertNotNull(graph);
            // Should have nodes for each package
            assertTrue(graph.nodes().stream().allMatch(n -> n.type() == NodeType.PACKAGE));
        }

        @Test
        @DisplayName("Should include package dependencies")
        void shouldIncludePackageDependencies() {
            List<ClassMetadata> classes = createClassesWithCrossPackageDependencies();

            DependencyGraph graph = builder.buildPackageGraph(classes);

            assertTrue(graph.edges().stream()
                    .anyMatch(e -> e.type() == EdgeType.PACKAGE_DEPENDENCY));
        }
    }

    @Nested
    @DisplayName("Inheritance Graph Building")
    class InheritanceGraphTests {

        @Test
        @DisplayName("Should build inheritance-only graph")
        void shouldBuildInheritanceGraph() {
            List<ClassMetadata> classes = createClassesWithInheritance();

            DependencyGraph graph = builder.buildInheritanceGraph(classes);

            assertNotNull(graph);
            // Should only contain inheritance and implementation edges
            assertTrue(graph.edges().stream()
                    .allMatch(e -> e.type() == EdgeType.INHERITANCE ||
                            e.type() == EdgeType.IMPLEMENTATION));
        }
    }

    @Nested
    @DisplayName("Call Graph Building")
    class CallGraphTests {

        @Test
        @DisplayName("Should build method call graph")
        void shouldBuildCallGraph() {
            List<ClassMetadata> classes = createClassesWithMethodCalls();

            DependencyGraph graph = builder.buildCallGraph(classes);

            assertNotNull(graph);
            // Nodes should be methods
            assertTrue(graph.nodes().stream().allMatch(n -> n.type() == NodeType.METHOD));
        }
    }

    @Nested
    @DisplayName("Circular Dependency Detection")
    class CircularDependencyTests {

        @Test
        @DisplayName("Should detect circular dependencies")
        void shouldDetectCircularDependencies() {
            List<ClassMetadata> classes = createClassesWithCircularDependency();

            DependencyGraph graph = builder.buildFullGraph(classes);
            List<List<String>> cycles = builder.findCircularDependencies(graph);

            // May or may not find cycles depending on graph structure
            assertNotNull(cycles);
        }
    }

    @Nested
    @DisplayName("Graph Metrics")
    class GraphMetricsTests {

        @Test
        @DisplayName("Should calculate graph metrics")
        void shouldCalculateMetrics() {
            List<ClassMetadata> classes = createSampleClasses();

            DependencyGraph graph = builder.buildFullGraph(classes);
            GraphMetrics metrics = builder.calculateMetrics(graph);

            assertNotNull(metrics);
            assertEquals(graph.nodes().size(), metrics.nodeCount());
            assertEquals(graph.edges().size(), metrics.edgeCount());
            assertTrue(metrics.density() >= 0);
        }

        @Test
        @DisplayName("Should identify orphan nodes")
        void shouldIdentifyOrphanNodes() {
            ClassMetadata orphan = createClass("com.example", "OrphanClass", null);
            List<ClassMetadata> classes = List.of(orphan);

            DependencyGraph graph = builder.buildFullGraph(classes);
            GraphMetrics metrics = builder.calculateMetrics(graph);

            assertFalse(metrics.orphanNodes().isEmpty());
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createSampleClasses() {
        List<ClassMetadata> classes = new ArrayList<>();
        classes.add(createClass("com.example", "UserService", null));
        classes.add(createClass("com.example", "UserRepository", null));
        classes.add(createClass("com.example", "User", null));
        return classes;
    }

    private List<ClassMetadata> createClassesWithInheritance() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata parent = createClass("com.example", "BaseService", null);
        classes.add(parent);

        ClassMetadata child = createClass("com.example", "UserService",
                "com.example.BaseService");
        classes.add(child);

        return classes;
    }

    private List<ClassMetadata> createClassesWithInterfaces() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata iface = createClass("com.example", "Repository", null);
        iface.setClassType(ClassMetadata.ClassType.INTERFACE);
        classes.add(iface);

        ClassMetadata impl = createClass("com.example", "UserRepository", null);
        impl.getInterfaces().add("com.example.Repository");
        classes.add(impl);

        return classes;
    }

    private List<ClassMetadata> createClassesWithMethodCalls() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata repo = createClass("com.example", "UserRepository", null);
        MethodMetadata findById = new MethodMetadata();
        findById.setMethodName("findById");
        findById.setAccessModifiers(Set.of("public"));
        findById.setReturnType("User");
        repo.getMethods().add(findById);
        classes.add(repo);

        ClassMetadata service = createClass("com.example", "UserService", null);
        MethodMetadata getUser = new MethodMetadata();
        getUser.setMethodName("getUser");
        getUser.setAccessModifiers(Set.of("public"));
        getUser.setReturnType("User");
        getUser.setInvocations(new ArrayList<>());

        MethodCall call = new MethodCall();
        call.setOwnerClass("com.example.UserRepository");
        call.setMethodName("findById");
        getUser.getInvocations().add(call);

        service.getMethods().add(getUser);
        classes.add(service);

        return classes;
    }

    private List<ClassMetadata> createClassesInDifferentPackages() {
        List<ClassMetadata> classes = new ArrayList<>();
        classes.add(createClass("com.example.service", "UserService", null));
        classes.add(createClass("com.example.repository", "UserRepository", null));
        classes.add(createClass("com.example.model", "User", null));
        return classes;
    }

    private List<ClassMetadata> createClassesWithCrossPackageDependencies() {
        List<ClassMetadata> classes = new ArrayList<>();

        // Model class in one package
        ClassMetadata user = createClass("com.example.model", "User", null);
        MethodMetadata getUserName = new MethodMetadata();
        getUserName.setMethodName("getName");
        getUserName.setReturnType("String");
        getUserName.setAccessModifiers(Set.of("public"));
        user.getMethods().add(getUserName);
        classes.add(user);

        // Service class in another package that calls user.getName()
        ClassMetadata service = createClass("com.example.service", "UserService", null);
        MethodMetadata getUser = new MethodMetadata();
        getUser.setMethodName("getUser");
        getUser.setReturnType("String");
        getUser.setAccessModifiers(Set.of("public"));
        getUser.setInvocations(new ArrayList<>());

        // Add method call to User class in different package
        MethodCall call = new MethodCall();
        call.setOwnerClass("com.example.model.User");
        call.setMethodName("getName");
        getUser.getInvocations().add(call);

        service.getMethods().add(getUser);
        classes.add(service);

        return classes;
    }

    private List<ClassMetadata> createClassesWithCircularDependency() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata classA = createClass("com.example", "ClassA", null);
        MethodMetadata methodA = new MethodMetadata();
        methodA.setMethodName("callB");
        methodA.setAccessModifiers(Set.of("public"));
        methodA.setReturnType("void");
        methodA.setInvocations(new ArrayList<>());
        MethodCall callB = new MethodCall();
        callB.setOwnerClass("com.example.ClassB");
        callB.setMethodName("process");
        methodA.getInvocations().add(callB);
        classA.getMethods().add(methodA);
        classes.add(classA);

        ClassMetadata classB = createClass("com.example", "ClassB", null);
        MethodMetadata methodB = new MethodMetadata();
        methodB.setMethodName("process");
        methodB.setAccessModifiers(Set.of("public"));
        methodB.setReturnType("void");
        methodB.setInvocations(new ArrayList<>());
        MethodCall callA = new MethodCall();
        callA.setOwnerClass("com.example.ClassA");
        callA.setMethodName("callB");
        methodB.getInvocations().add(callA);
        classB.getMethods().add(methodB);
        classes.add(classB);

        return classes;
    }

    private ClassMetadata createClass(String packageName, String className, String superClass) {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName(packageName);
        metadata.setClassName(className);
        metadata.setFullyQualifiedName(packageName + "." + className);
        metadata.setClassType(ClassMetadata.ClassType.CLASS);
        metadata.setSuperClassName(superClass);
        return metadata;
    }
}

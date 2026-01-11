package com.codeexplainer.diagram;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.diagram.DiagramGenerator.*;
import com.codeexplainer.graph.DependencyGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiagramGenerator.
 */
@DisplayName("DiagramGenerator Tests")
class DiagramGeneratorTest {

    private DiagramGenerator generator;

    @BeforeEach
    void setUp() {
        DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder();
        generator = new DiagramGenerator(graphBuilder);
    }

    @Nested
    @DisplayName("Class Diagram")
    class ClassDiagramTests {

        @Test
        @DisplayName("Should generate class diagram")
        void shouldGenerateClassDiagram() {
            List<ClassMetadata> classes = createSampleClasses();

            DiagramResult result = generator.generateClassDiagram(classes, DiagramOptions.defaults());

            assertNotNull(result);
            assertEquals(DiagramType.CLASS, result.type());
            assertTrue(result.source().contains("@startuml"));
            assertTrue(result.source().contains("@enduml"));
        }

        @Test
        @DisplayName("Should include class definitions")
        void shouldIncludeClassDefinitions() {
            List<ClassMetadata> classes = createSampleClasses();

            DiagramResult result = generator.generateClassDiagram(classes, DiagramOptions.defaults());

            assertTrue(result.source().contains("UserService"));
            assertTrue(result.source().contains("UserRepository"));
        }

        @Test
        @DisplayName("Should include inheritance relationships")
        void shouldIncludeInheritance() {
            List<ClassMetadata> classes = createClassesWithInheritance();

            DiagramResult result = generator.generateClassDiagram(classes, DiagramOptions.defaults());

            assertTrue(result.source().contains("--|>"));
        }

        @Test
        @DisplayName("Should group by package when requested")
        void shouldGroupByPackage() {
            List<ClassMetadata> classes = createSampleClasses();
            DiagramOptions options = new DiagramOptions(true, true, true, false);

            DiagramResult result = generator.generateClassDiagram(classes, options);

            assertTrue(result.source().contains("package"));
        }
    }

    @Nested
    @DisplayName("Sequence Diagram")
    class SequenceDiagramTests {

        @Test
        @DisplayName("Should generate sequence diagram")
        void shouldGenerateSequenceDiagram() {
            List<ClassMetadata> classes = createClassesWithMethodCalls();
            ClassMetadata service = classes.stream()
                    .filter(c -> c.getClassName().equals("UserService"))
                    .findFirst().orElseThrow();

            DiagramResult result = generator.generateSequenceDiagram(
                    service, "getUser", classes, 3);

            assertNotNull(result);
            assertEquals(DiagramType.SEQUENCE, result.type());
            assertTrue(result.source().contains("@startuml"));
        }

        @Test
        @DisplayName("Should include method calls in sequence")
        void shouldIncludeMethodCalls() {
            List<ClassMetadata> classes = createClassesWithMethodCalls();
            ClassMetadata service = classes.stream()
                    .filter(c -> c.getClassName().equals("UserService"))
                    .findFirst().orElseThrow();

            DiagramResult result = generator.generateSequenceDiagram(
                    service, "getUser", classes, 3);

            assertTrue(result.source().contains("->"));
        }
    }

    @Nested
    @DisplayName("Component Diagram")
    class ComponentDiagramTests {

        @Test
        @DisplayName("Should generate component diagram")
        void shouldGenerateComponentDiagram() {
            List<ClassMetadata> classes = createClassesInDifferentPackages();

            DiagramResult result = generator.generateComponentDiagram(classes);

            assertNotNull(result);
            assertEquals(DiagramType.COMPONENT, result.type());
            assertTrue(result.source().contains("@startuml"));
        }
    }

    @Nested
    @DisplayName("Package Diagram")
    class PackageDiagramTests {

        @Test
        @DisplayName("Should generate package diagram")
        void shouldGeneratePackageDiagram() {
            List<ClassMetadata> classes = createClassesInDifferentPackages();

            DiagramResult result = generator.generatePackageDiagram(classes);

            assertNotNull(result);
            assertEquals(DiagramType.PACKAGE, result.type());
            assertTrue(result.source().contains("package"));
        }
    }

    @Nested
    @DisplayName("Inheritance Diagram")
    class InheritanceDiagramTests {

        @Test
        @DisplayName("Should generate inheritance diagram")
        void shouldGenerateInheritanceDiagram() {
            List<ClassMetadata> classes = createClassesWithInheritance();

            DiagramResult result = generator.generateInheritanceDiagram(classes);

            assertNotNull(result);
            assertEquals(DiagramType.INHERITANCE, result.type());
            assertTrue(result.source().contains("@startuml"));
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createSampleClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata service = createClass("com.example.service", "UserService", null);
        service.getAnnotations().add("org.springframework.stereotype.Service");
        MethodMetadata getUser = new MethodMetadata();
        getUser.setMethodName("getUser");
        getUser.setAccessModifiers(Set.of("public"));
        getUser.setReturnType("User");
        service.getMethods().add(getUser);
        classes.add(service);

        ClassMetadata repo = createClass("com.example.repository", "UserRepository", null);
        repo.setClassType(ClassMetadata.ClassType.INTERFACE);
        classes.add(repo);

        return classes;
    }

    private List<ClassMetadata> createClassesWithInheritance() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata parent = createClass("com.example", "BaseService", null);
        classes.add(parent);

        ClassMetadata child = createClass("com.example", "UserService", "com.example.BaseService");
        classes.add(child);

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
        classes.add(createClass("com.example.controller", "UserController", null));
        classes.add(createClass("com.example.service", "UserService", null));
        classes.add(createClass("com.example.repository", "UserRepository", null));
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

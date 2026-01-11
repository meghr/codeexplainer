package com.codeexplainer.analysis;

import com.codeexplainer.analysis.DataFlowAnalyzer.*;
import com.codeexplainer.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataFlowAnalyzer.
 */
@DisplayName("DataFlowAnalyzer Tests")
class DataFlowAnalyzerTest {

    private DataFlowAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DataFlowAnalyzer();
    }

    @Nested
    @DisplayName("Flow Analysis")
    class FlowAnalysisTests {

        @Test
        @DisplayName("Should analyze data flow")
        void shouldAnalyzeDataFlow() {
            List<ClassMetadata> classes = createProducerConsumerClasses();

            DataFlowResult result = analyzer.analyzeFlow(classes);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should find type producers")
        void shouldFindTypeProducers() {
            List<ClassMetadata> classes = createProducerConsumerClasses();
            DataFlowResult result = analyzer.analyzeFlow(classes);

            assertNotNull(result.typeProducers());
        }

        @Test
        @DisplayName("Should find type consumers")
        void shouldFindTypeConsumers() {
            List<ClassMetadata> classes = createProducerConsumerClasses();
            DataFlowResult result = analyzer.analyzeFlow(classes);

            assertNotNull(result.typeConsumers());
        }
    }

    @Nested
    @DisplayName("Producer/Consumer Finding")
    class ProducerConsumerTests {

        @Test
        @DisplayName("Should find producers of type")
        void shouldFindProducersOfType() {
            List<ClassMetadata> classes = createProducerConsumerClasses();

            List<String> producers = analyzer.findProducersOf(classes, "User");

            assertFalse(producers.isEmpty());
        }

        @Test
        @DisplayName("Should find consumers of type")
        void shouldFindConsumersOfType() {
            List<ClassMetadata> classes = createProducerConsumerClasses();

            List<String> consumers = analyzer.findConsumersOf(classes, "User");

            assertFalse(consumers.isEmpty());
        }
    }

    @Nested
    @DisplayName("Call Chain Analysis")
    class CallChainTests {

        @Test
        @DisplayName("Should analyze call chains")
        void shouldAnalyzeCallChains() {
            List<ClassMetadata> classes = createClassesWithInvocations();

            List<CallChain> chains = analyzer.analyzeCallChains(classes, 5);

            assertNotNull(chains);
        }
    }

    @Nested
    @DisplayName("Flow Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should get flow statistics")
        void shouldGetFlowStatistics() {
            List<ClassMetadata> classes = createProducerConsumerClasses();
            DataFlowResult result = analyzer.analyzeFlow(classes);

            FlowStatistics stats = analyzer.getStatistics(result);

            assertNotNull(stats);
            assertTrue(stats.totalPaths() >= 0);
        }
    }

    @Nested
    @DisplayName("Diagram Generation")
    class DiagramTests {

        @Test
        @DisplayName("Should generate flow diagram")
        void shouldGenerateFlowDiagram() {
            List<ClassMetadata> classes = createProducerConsumerClasses();
            DataFlowResult result = analyzer.analyzeFlow(classes);

            String diagram = analyzer.generateFlowDiagram(result, 10);

            assertNotNull(diagram);
            assertTrue(diagram.contains("@startuml"));
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createProducerConsumerClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        // Service that produces User
        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("UserService");
        service.setFullyQualifiedName("com.example.service.UserService");
        service.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata getUser = new MethodMetadata();
        getUser.setMethodName("getUser");
        getUser.setAccessModifiers(Set.of("public"));
        getUser.setReturnType("User");
        service.getMethods().add(getUser);
        classes.add(service);

        // Controller that consumes User
        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata handleUser = new MethodMetadata();
        handleUser.setMethodName("handleUser");
        handleUser.setAccessModifiers(Set.of("public"));
        ParameterInfo userParam = new ParameterInfo();
        userParam.setName("user");
        userParam.setType("User");
        handleUser.getParameters().add(userParam);
        controller.getMethods().add(handleUser);
        classes.add(controller);

        return classes;
    }

    private List<ClassMetadata> createClassesWithInvocations() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata getUsers = new MethodMetadata();
        getUsers.setMethodName("getUsers");
        getUsers.setAccessModifiers(Set.of("public"));

        MethodCall invocation = new MethodCall();
        invocation.setOwnerClass("UserService");
        invocation.setMethodName("findAll");
        getUsers.getInvocations().add(invocation);

        controller.getMethods().add(getUsers);
        classes.add(controller);

        return classes;
    }
}

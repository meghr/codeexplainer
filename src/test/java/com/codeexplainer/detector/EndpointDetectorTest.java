package com.codeexplainer.detector;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.EndpointInfo;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EndpointDetector.
 */
@DisplayName("EndpointDetector Tests")
class EndpointDetectorTest {

    private EndpointDetector detector;

    @BeforeEach
    void setUp() {
        detector = new EndpointDetector();
    }

    @Nested
    @DisplayName("Endpoint Detection")
    class EndpointDetectionTests {

        @Test
        @DisplayName("Should detect REST endpoints from controller")
        void shouldDetectRestEndpoints() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());

            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            assertFalse(endpoints.isEmpty());
        }

        @Test
        @DisplayName("Should detect GET endpoints")
        void shouldDetectGetEndpoints() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());

            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            assertTrue(endpoints.stream().anyMatch(e -> e.getHttpMethod().equals("GET")));
        }

        @Test
        @DisplayName("Should detect POST endpoints")
        void shouldDetectPostEndpoints() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());

            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            assertTrue(endpoints.stream().anyMatch(e -> e.getHttpMethod().equals("POST")));
        }

        @Test
        @DisplayName("Should not detect endpoints from non-controller classes")
        void shouldNotDetectFromNonController() {
            ClassMetadata service = new ClassMetadata();
            service.setClassName("UserService");
            service.setFullyQualifiedName("com.example.UserService");
            service.setClassType(ClassMetadata.ClassType.CLASS);
            service.getAnnotations().add("org.springframework.stereotype.Service");

            List<EndpointInfo> endpoints = detector.detectEndpoints(List.of(service));

            assertTrue(endpoints.isEmpty());
        }
    }

    @Nested
    @DisplayName("Endpoint Grouping")
    class EndpointGroupingTests {

        @Test
        @DisplayName("Should group by HTTP method")
        void shouldGroupByHttpMethod() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());
            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            Map<String, List<EndpointInfo>> grouped = detector.groupByHttpMethod(endpoints);

            assertNotNull(grouped);
            assertTrue(grouped.containsKey("GET"));
        }

        @Test
        @DisplayName("Should group by controller")
        void shouldGroupByController() {
            List<ClassMetadata> classes = List.of(
                    createControllerWithEndpoints(),
                    createSecondController());
            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            Map<String, List<EndpointInfo>> grouped = detector.groupByController(endpoints);

            assertEquals(2, grouped.size());
        }

        @Test
        @DisplayName("Should group by path prefix")
        void shouldGroupByPathPrefix() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());
            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            Map<String, List<EndpointInfo>> grouped = detector.groupByPathPrefix(endpoints);

            assertNotNull(grouped);
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate endpoint statistics")
        void shouldCalculateStatistics() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());
            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            EndpointDetector.EndpointStatistics stats = detector.getStatistics(endpoints);

            assertNotNull(stats);
            assertTrue(stats.totalEndpoints() > 0);
            assertNotNull(stats.byHttpMethod());
        }
    }

    @Nested
    @DisplayName("Endpoint Summary")
    class EndpointSummaryTests {

        @Test
        @DisplayName("Should generate endpoint summary")
        void shouldGenerateEndpointSummary() {
            List<ClassMetadata> classes = List.of(createControllerWithEndpoints());
            List<EndpointInfo> endpoints = detector.detectEndpoints(classes);

            String summary = detector.generateEndpointSummary(endpoints);

            assertNotNull(summary);
            assertTrue(summary.contains("# API Endpoints"));
        }
    }

    // ============= Helper Methods =============

    private ClassMetadata createControllerWithEndpoints() {
        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RestController");
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RequestMapping(\"/api/users\")");

        // GET endpoint
        MethodMetadata getUsers = new MethodMetadata();
        getUsers.setMethodName("getUsers");
        getUsers.setAccessModifiers(Set.of("public"));
        getUsers.setReturnType("List<User>");
        getUsers.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping");
        controller.getMethods().add(getUsers);

        // GET by ID
        MethodMetadata getUserById = new MethodMetadata();
        getUserById.setMethodName("getUserById");
        getUserById.setAccessModifiers(Set.of("public"));
        getUserById.setReturnType("User");
        getUserById.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping(\"/{id}\")");
        ParameterInfo idParam = new ParameterInfo();
        idParam.setName("id");
        idParam.setType("Long");
        getUserById.getParameters().add(idParam);
        controller.getMethods().add(getUserById);

        // POST endpoint
        MethodMetadata createUser = new MethodMetadata();
        createUser.setMethodName("createUser");
        createUser.setAccessModifiers(Set.of("public"));
        createUser.setReturnType("User");
        createUser.getAnnotations().add("org.springframework.web.bind.annotation.PostMapping");
        controller.getMethods().add(createUser);

        // PUT endpoint
        MethodMetadata updateUser = new MethodMetadata();
        updateUser.setMethodName("updateUser");
        updateUser.setAccessModifiers(Set.of("public"));
        updateUser.setReturnType("User");
        updateUser.getAnnotations().add("org.springframework.web.bind.annotation.PutMapping(\"/{id}\")");
        controller.getMethods().add(updateUser);

        // DELETE endpoint
        MethodMetadata deleteUser = new MethodMetadata();
        deleteUser.setMethodName("deleteUser");
        deleteUser.setAccessModifiers(Set.of("public"));
        deleteUser.setReturnType("void");
        deleteUser.getAnnotations().add("org.springframework.web.bind.annotation.DeleteMapping(\"/{id}\")");
        controller.getMethods().add(deleteUser);

        return controller;
    }

    private ClassMetadata createSecondController() {
        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("OrderController");
        controller.setFullyQualifiedName("com.example.controller.OrderController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RestController");

        MethodMetadata getOrders = new MethodMetadata();
        getOrders.setMethodName("getOrders");
        getOrders.setAccessModifiers(Set.of("public"));
        getOrders.setReturnType("List<Order>");
        getOrders.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping(\"/orders\")");
        controller.getMethods().add(getOrders);

        return controller;
    }
}

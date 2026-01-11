package com.codeexplainer.diagram;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlowDiagramGenerator.
 */
@DisplayName("FlowDiagramGenerator Tests")
class FlowDiagramGeneratorTest {

    private FlowDiagramGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new FlowDiagramGenerator();
    }

    @Nested
    @DisplayName("Activity Diagram")
    class ActivityDiagramTests {

        @Test
        @DisplayName("Should generate activity diagram")
        void shouldGenerateActivityDiagram() {
            ClassMetadata service = createServiceWithMethods();

            String puml = generator.generateActivityDiagram(service, "processOrder");

            assertNotNull(puml);
            assertTrue(puml.contains("@startuml"));
            assertTrue(puml.contains("@enduml"));
            assertTrue(puml.contains("start"));
            assertTrue(puml.contains("stop"));
        }

        @Test
        @DisplayName("Should handle missing method")
        void shouldHandleMissingMethod() {
            ClassMetadata service = createServiceWithMethods();

            String puml = generator.generateActivityDiagram(service, "nonExistent");

            assertTrue(puml.contains("Method not found"));
        }
    }

    @Nested
    @DisplayName("Request Flow Diagram")
    class RequestFlowTests {

        @Test
        @DisplayName("Should generate request flow diagram")
        void shouldGenerateRequestFlowDiagram() {
            List<ClassMetadata> classes = createLayeredClasses();
            ClassMetadata controller = classes.stream()
                    .filter(c -> c.getClassName().equals("UserController"))
                    .findFirst().orElseThrow();

            String puml = generator.generateRequestFlowDiagram(
                    controller, "getUsers", classes, 3);

            assertNotNull(puml);
            assertTrue(puml.contains("@startuml"));
            assertTrue(puml.contains("actor Client"));
            assertTrue(puml.contains("Controller"));
        }
    }

    @Nested
    @DisplayName("Swimlane Diagram")
    class SwimlaneDiagramTests {

        @Test
        @DisplayName("Should generate swimlane diagram")
        void shouldGenerateSwimlaneDiagram() {
            List<ClassMetadata> classes = createLayeredClasses();

            String puml = generator.generateSwimlaneDiagram(classes);

            assertNotNull(puml);
            assertTrue(puml.contains("@startuml"));
            assertTrue(puml.contains("|Controllers|"));
            assertTrue(puml.contains("|Services|"));
        }
    }

    @Nested
    @DisplayName("Use Case Diagram")
    class UseCaseDiagramTests {

        @Test
        @DisplayName("Should generate use case diagram")
        void shouldGenerateUseCaseDiagram() {
            List<ClassMetadata> controllers = createControllersWithEndpoints();

            String puml = generator.generateUseCaseDiagram(controllers);

            assertNotNull(puml);
            assertTrue(puml.contains("@startuml"));
            assertTrue(puml.contains("actor User"));
            assertTrue(puml.contains("usecase"));
        }
    }

    @Nested
    @DisplayName("State Diagram")
    class StateDiagramTests {

        @Test
        @DisplayName("Should generate state diagram for enum")
        void shouldGenerateStateDiagramForEnum() {
            ClassMetadata enumClass = createEnumClass();

            String puml = generator.generateStateDiagram(enumClass);

            assertNotNull(puml);
            assertTrue(puml.contains("@startuml"));
            assertTrue(puml.contains("State"));
        }
    }

    @Nested
    @DisplayName("Deployment Diagram")
    class DeploymentDiagramTests {

        @Test
        @DisplayName("Should generate deployment diagram")
        void shouldGenerateDeploymentDiagram() {
            String puml = generator.generateDeploymentDiagram(
                    "MyApp", List.of("API", "Web", "Scheduler"));

            assertNotNull(puml);
            assertTrue(puml.contains("@startuml"));
            assertTrue(puml.contains("MyApp"));
            assertTrue(puml.contains("Database"));
        }
    }

    // ============= Helper Methods =============

    private ClassMetadata createServiceWithMethods() {
        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("OrderService");
        service.setFullyQualifiedName("com.example.service.OrderService");
        service.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata processOrder = new MethodMetadata();
        processOrder.setMethodName("processOrder");
        processOrder.setAccessModifiers(Set.of("public"));
        processOrder.setReturnType("Order");
        processOrder.setInvocations(new ArrayList<>());

        MethodCall validateCall = new MethodCall();
        validateCall.setOwnerClass("com.example.service.ValidationService");
        validateCall.setMethodName("validate");
        processOrder.getInvocations().add(validateCall);

        MethodCall saveCall = new MethodCall();
        saveCall.setOwnerClass("com.example.repository.OrderRepository");
        saveCall.setMethodName("save");
        processOrder.getInvocations().add(saveCall);

        service.getMethods().add(processOrder);

        return service;
    }

    private List<ClassMetadata> createLayeredClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        // Controller
        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RestController");

        MethodMetadata getUsers = new MethodMetadata();
        getUsers.setMethodName("getUsers");
        getUsers.setAccessModifiers(Set.of("public"));
        getUsers.setReturnType("List");
        getUsers.setInvocations(new ArrayList<>());
        getUsers.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping");

        MethodCall serviceCall = new MethodCall();
        serviceCall.setOwnerClass("com.example.service.UserService");
        serviceCall.setMethodName("findAll");
        getUsers.getInvocations().add(serviceCall);

        controller.getMethods().add(getUsers);
        classes.add(controller);

        // Service
        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("UserService");
        service.setFullyQualifiedName("com.example.service.UserService");
        service.setClassType(ClassMetadata.ClassType.CLASS);
        service.getAnnotations().add("org.springframework.stereotype.Service");

        MethodMetadata findAll = new MethodMetadata();
        findAll.setMethodName("findAll");
        findAll.setAccessModifiers(Set.of("public"));
        findAll.setReturnType("List");
        findAll.setInvocations(new ArrayList<>());

        MethodCall repoCall = new MethodCall();
        repoCall.setOwnerClass("com.example.repository.UserRepository");
        repoCall.setMethodName("findAll");
        findAll.getInvocations().add(repoCall);

        service.getMethods().add(findAll);
        classes.add(service);

        // Repository
        ClassMetadata repo = new ClassMetadata();
        repo.setPackageName("com.example.repository");
        repo.setClassName("UserRepository");
        repo.setFullyQualifiedName("com.example.repository.UserRepository");
        repo.setClassType(ClassMetadata.ClassType.INTERFACE);
        repo.getAnnotations().add("org.springframework.stereotype.Repository");
        classes.add(repo);

        return classes;
    }

    private List<ClassMetadata> createControllersWithEndpoints() {
        List<ClassMetadata> controllers = new ArrayList<>();

        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata getUsers = new MethodMetadata();
        getUsers.setMethodName("getUsers");
        getUsers.setAccessModifiers(Set.of("public"));
        getUsers.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping");
        controller.getMethods().add(getUsers);

        MethodMetadata createUser = new MethodMetadata();
        createUser.setMethodName("createUser");
        createUser.setAccessModifiers(Set.of("public"));
        createUser.getAnnotations().add("org.springframework.web.bind.annotation.PostMapping");
        controller.getMethods().add(createUser);

        controllers.add(controller);
        return controllers;
    }

    private ClassMetadata createEnumClass() {
        ClassMetadata enumClass = new ClassMetadata();
        enumClass.setPackageName("com.example.model");
        enumClass.setClassName("OrderStatus");
        enumClass.setFullyQualifiedName("com.example.model.OrderStatus");
        enumClass.setClassType(ClassMetadata.ClassType.ENUM);

        // Enum constants are stored as static final fields
        var pending = new com.codeexplainer.core.model.FieldMetadata();
        pending.setFieldName("PENDING");
        pending.setStatic(true);
        pending.setFinal(true);
        enumClass.getFields().add(pending);

        var processing = new com.codeexplainer.core.model.FieldMetadata();
        processing.setFieldName("PROCESSING");
        processing.setStatic(true);
        processing.setFinal(true);
        enumClass.getFields().add(processing);

        var completed = new com.codeexplainer.core.model.FieldMetadata();
        completed.setFieldName("COMPLETED");
        completed.setStatic(true);
        completed.setFinal(true);
        enumClass.getFields().add(completed);

        return enumClass;
    }
}

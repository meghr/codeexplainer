package com.codeexplainer.extractor;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import com.codeexplainer.extractor.MethodExtractionService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MethodExtractionService.
 */
@DisplayName("MethodExtractionService Tests")
class MethodExtractionServiceTest {

    private MethodExtractionService service;

    @BeforeEach
    void setUp() {
        service = new MethodExtractionService();
    }

    @Nested
    @DisplayName("Method Extraction")
    class MethodExtractionTests {

        @Test
        @DisplayName("Should extract all methods from classes")
        void shouldExtractAllMethods() {
            List<ClassMetadata> classes = createSampleClasses();

            List<EnrichedMethod> methods = service.extractAllMethods(classes);

            assertFalse(methods.isEmpty());
            assertTrue(methods.stream().anyMatch(m -> m.methodName().equals("findUser")));
            assertTrue(methods.stream().anyMatch(m -> m.methodName().equals("getName")));
        }

        @Test
        @DisplayName("Should enrich method with computed metadata")
        void shouldEnrichMethod() {
            ClassMetadata classMetadata = createClassWithMethods();
            MethodMetadata method = classMetadata.getMethods().get(0);

            EnrichedMethod enriched = service.enrichMethod(method, classMetadata);

            assertEquals(classMetadata.getFullyQualifiedName(), enriched.ownerClass());
            assertNotNull(enriched.category());
            assertNotNull(enriched.signature());
            assertNotNull(enriched.fullyQualifiedName());
        }
    }

    @Nested
    @DisplayName("Method Categorization")
    class MethodCategorizationTests {

        @Test
        @DisplayName("Should categorize getter methods")
        void shouldCategorizeGetters() {
            List<ClassMetadata> classes = List.of(createClassWithMethods());

            Map<MethodCategory, List<EnrichedMethod>> grouped = service.groupByCategory(classes);

            assertTrue(grouped.containsKey(MethodCategory.GETTER));
            assertTrue(grouped.get(MethodCategory.GETTER).stream()
                    .anyMatch(m -> m.methodName().startsWith("get")));
        }

        @Test
        @DisplayName("Should categorize setter methods")
        void shouldCategorizeSetters() {
            List<ClassMetadata> classes = List.of(createClassWithMethods());

            Map<MethodCategory, List<EnrichedMethod>> grouped = service.groupByCategory(classes);

            assertTrue(grouped.containsKey(MethodCategory.SETTER));
        }

        @Test
        @DisplayName("Should categorize constructors")
        void shouldCategorizeConstructors() {
            List<ClassMetadata> classes = List.of(createClassWithMethods());

            Map<MethodCategory, List<EnrichedMethod>> grouped = service.groupByCategory(classes);

            assertTrue(grouped.containsKey(MethodCategory.CONSTRUCTOR));
        }

        @Test
        @DisplayName("Should categorize REST endpoints")
        void shouldCategorizeRestEndpoints() {
            List<ClassMetadata> classes = List.of(createControllerClass());

            List<EnrichedMethod> endpoints = service.findMethods(classes,
                    MethodFilter.byCategory(MethodCategory.REST_ENDPOINT));

            assertFalse(endpoints.isEmpty());
        }
    }

    @Nested
    @DisplayName("Call Graph")
    class CallGraphTests {

        @Test
        @DisplayName("Should build call graph from methods")
        void shouldBuildCallGraph() {
            List<ClassMetadata> classes = createClassesWithInvocations();

            CallGraph callGraph = service.buildCallGraph(classes);

            assertNotNull(callGraph);
            assertFalse(callGraph.allMethods().isEmpty());
        }

        @Test
        @DisplayName("Should track outgoing calls")
        void shouldTrackOutgoingCalls() {
            List<ClassMetadata> classes = createClassesWithInvocations();

            CallGraph callGraph = service.buildCallGraph(classes);

            // UserService.findUser calls userRepository.findById
            Set<String> callees = callGraph.getCallees("com.example.UserService.findUser");
            assertFalse(callees.isEmpty());
        }

        @Test
        @DisplayName("Should track incoming calls")
        void shouldTrackIncomingCalls() {
            List<ClassMetadata> classes = createClassesWithInvocations();

            CallGraph callGraph = service.buildCallGraph(classes);

            // Repository methods should have callers
            assertFalse(callGraph.incoming().isEmpty());
        }

        @Test
        @DisplayName("Should find most called methods")
        void shouldFindMostCalledMethods() {
            List<ClassMetadata> classes = createClassesWithInvocations();

            CallGraph callGraph = service.buildCallGraph(classes);
            List<MethodCallCount> mostCalled = service.getMostCalledMethods(callGraph, 5);

            assertNotNull(mostCalled);
        }
    }

    @Nested
    @DisplayName("Method Finding")
    class MethodFindingTests {

        @Test
        @DisplayName("Should find public methods")
        void shouldFindPublicMethods() {
            List<ClassMetadata> classes = createSampleClasses();

            List<EnrichedMethod> publicMethods = service.findMethods(classes,
                    MethodFilter.publicMethods());

            assertTrue(publicMethods.stream()
                    .allMatch(m -> m.accessModifiers().contains("public")));
        }

        @Test
        @DisplayName("Should find entry points")
        void shouldFindEntryPoints() {
            List<ClassMetadata> classes = createSampleClasses();
            classes.add(createControllerClass());
            classes.add(createClassWithMain());

            List<EnrichedMethod> entryPoints = service.findEntryPoints(classes);

            assertFalse(entryPoints.isEmpty());
        }

        @Test
        @DisplayName("Should find overloaded methods")
        void shouldFindOverloadedMethods() {
            List<ClassMetadata> classes = List.of(createClassWithOverloads());

            Map<String, List<EnrichedMethod>> overloaded = service.findOverloadedMethods(classes);

            assertFalse(overloaded.isEmpty());
        }
    }

    @Nested
    @DisplayName("Parameter Analysis")
    class ParameterAnalysisTests {

        @Test
        @DisplayName("Should analyze parameter distribution")
        void shouldAnalyzeParameterDistribution() {
            List<ClassMetadata> classes = createSampleClasses();

            ParameterAnalysis analysis = service.analyzeParameters(classes);

            assertNotNull(analysis.parameterCountDistribution());
            assertNotNull(analysis.typeFrequency());
            assertTrue(analysis.maxParameters() >= 0);
        }

        @Test
        @DisplayName("Should analyze return types")
        void shouldAnalyzeReturnTypes() {
            List<ClassMetadata> classes = createSampleClasses();

            Map<String, Long> returnTypes = service.analyzeReturnTypes(classes);

            assertNotNull(returnTypes);
            assertFalse(returnTypes.isEmpty());
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createSampleClasses() {
        List<ClassMetadata> classes = new ArrayList<>();
        classes.add(createClassWithMethods());
        classes.add(createServiceClass());
        return classes;
    }

    private ClassMetadata createClassWithMethods() {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName("com.example");
        metadata.setClassName("User");
        metadata.setFullyQualifiedName("com.example.User");
        metadata.setClassType(ClassMetadata.ClassType.CLASS);

        // Constructor
        MethodMetadata constructor = new MethodMetadata();
        constructor.setMethodName("<init>");
        constructor.setAccessModifiers(Set.of("public"));
        constructor.setReturnType("void");
        metadata.getMethods().add(constructor);

        // Getter
        MethodMetadata getter = new MethodMetadata();
        getter.setMethodName("getName");
        getter.setAccessModifiers(Set.of("public"));
        getter.setReturnType("String");
        metadata.getMethods().add(getter);

        // Setter
        MethodMetadata setter = new MethodMetadata();
        setter.setMethodName("setName");
        setter.setAccessModifiers(Set.of("public"));
        setter.setReturnType("void");
        setter.getParameters().add(createParam("name", "String", 0));
        metadata.getMethods().add(setter);

        return metadata;
    }

    private ClassMetadata createServiceClass() {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName("com.example");
        metadata.setClassName("UserService");
        metadata.setFullyQualifiedName("com.example.UserService");
        metadata.setClassType(ClassMetadata.ClassType.CLASS);
        metadata.getAnnotations().add("org.springframework.stereotype.Service");

        MethodMetadata findUser = new MethodMetadata();
        findUser.setMethodName("findUser");
        findUser.setAccessModifiers(Set.of("public"));
        findUser.setReturnType("User");
        findUser.getParameters().add(createParam("id", "Long", 0));
        metadata.getMethods().add(findUser);

        return metadata;
    }

    private ClassMetadata createControllerClass() {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName("com.example");
        metadata.setClassName("UserController");
        metadata.setFullyQualifiedName("com.example.UserController");
        metadata.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata endpoint = new MethodMetadata();
        endpoint.setMethodName("handleRequest");
        endpoint.setAccessModifiers(Set.of("public"));
        endpoint.setReturnType("Response");
        endpoint.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping");
        metadata.getMethods().add(endpoint);

        return metadata;
    }

    private ClassMetadata createClassWithMain() {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName("com.example");
        metadata.setClassName("Application");
        metadata.setFullyQualifiedName("com.example.Application");
        metadata.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata main = new MethodMetadata();
        main.setMethodName("main");
        main.setAccessModifiers(Set.of("public"));
        main.setStatic(true);
        main.setReturnType("void");
        main.getParameters().add(createParam("args", "String[]", 0));
        metadata.getMethods().add(main);

        return metadata;
    }

    private ClassMetadata createClassWithOverloads() {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName("com.example");
        metadata.setClassName("Calculator");
        metadata.setFullyQualifiedName("com.example.Calculator");
        metadata.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata add1 = new MethodMetadata();
        add1.setMethodName("add");
        add1.setAccessModifiers(Set.of("public"));
        add1.setReturnType("int");
        add1.getParameters().add(createParam("a", "int", 0));
        add1.getParameters().add(createParam("b", "int", 1));
        metadata.getMethods().add(add1);

        MethodMetadata add2 = new MethodMetadata();
        add2.setMethodName("add");
        add2.setAccessModifiers(Set.of("public"));
        add2.setReturnType("double");
        add2.getParameters().add(createParam("a", "double", 0));
        add2.getParameters().add(createParam("b", "double", 1));
        metadata.getMethods().add(add2);

        return metadata;
    }

    private List<ClassMetadata> createClassesWithInvocations() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata userService = new ClassMetadata();
        userService.setPackageName("com.example");
        userService.setClassName("UserService");
        userService.setFullyQualifiedName("com.example.UserService");
        userService.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata findUser = new MethodMetadata();
        findUser.setMethodName("findUser");
        findUser.setAccessModifiers(Set.of("public"));
        findUser.setReturnType("User");
        findUser.setInvocations(new ArrayList<>());

        MethodCall call = new MethodCall();
        call.setOwnerClass("com.example.UserRepository");
        call.setMethodName("findById");
        call.setLineNumber(10);
        findUser.getInvocations().add(call);

        userService.getMethods().add(findUser);
        classes.add(userService);

        return classes;
    }

    private ParameterInfo createParam(String name, String type, int index) {
        ParameterInfo param = new ParameterInfo();
        param.setName(name);
        param.setType(type);
        param.setIndex(index);
        return param;
    }
}

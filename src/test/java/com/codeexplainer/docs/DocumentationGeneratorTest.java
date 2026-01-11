package com.codeexplainer.docs;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.JarAnalysisResult;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import com.codeexplainer.detector.ComponentDetector;
import com.codeexplainer.detector.EndpointDetector;
import com.codeexplainer.docs.DocumentationGenerator.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentationGenerator.
 */
@DisplayName("DocumentationGenerator Tests")
class DocumentationGeneratorTest {

    private DocumentationGenerator generator;

    @BeforeEach
    void setUp() {
        EndpointDetector endpointDetector = new EndpointDetector();
        ComponentDetector componentDetector = new ComponentDetector();
        generator = new DocumentationGenerator(endpointDetector, componentDetector);
    }

    @Nested
    @DisplayName("Full Documentation Generation")
    class FullDocumentationTests {

        @Test
        @DisplayName("Should generate complete documentation")
        void shouldGenerateCompleteDocumentation() {
            JarAnalysisResult result = createSampleAnalysisResult();

            GeneratedDocumentation doc = generator.generate(result, DocOptions.full());

            assertNotNull(doc);
            assertNotNull(doc.markdown());
            assertFalse(doc.markdown().isEmpty());
        }

        @Test
        @DisplayName("Should include table of contents")
        void shouldIncludeTableOfContents() {
            JarAnalysisResult result = createSampleAnalysisResult();

            GeneratedDocumentation doc = generator.generate(result, DocOptions.full());

            assertTrue(doc.markdown().contains("Table of Contents"));
        }

        @Test
        @DisplayName("Should generate HTML when option enabled")
        void shouldGenerateHtml() {
            JarAnalysisResult result = createSampleAnalysisResult();

            GeneratedDocumentation doc = generator.generate(result, DocOptions.full());

            assertNotNull(doc.html());
            assertTrue(doc.html().contains("<!DOCTYPE html>"));
        }
    }

    @Nested
    @DisplayName("Class Documentation")
    class ClassDocumentationTests {

        @Test
        @DisplayName("Should generate class documentation")
        void shouldGenerateClassDoc() {
            ClassMetadata clazz = createSampleClass();

            String doc = generator.generateClassDoc(clazz);

            assertNotNull(doc);
            assertTrue(doc.contains("UserService"));
            assertTrue(doc.contains("Package"));
        }

        @Test
        @DisplayName("Should include methods")
        void shouldIncludeMethods() {
            ClassMetadata clazz = createSampleClass();

            String doc = generator.generateClassDoc(clazz);

            assertTrue(doc.contains("Methods"));
            assertTrue(doc.contains("getUser"));
        }
    }

    @Nested
    @DisplayName("API Reference")
    class ApiReferenceTests {

        @Test
        @DisplayName("Should generate API reference")
        void shouldGenerateApiReference() {
            List<ClassMetadata> classes = createControllerClasses();

            String apiRef = generator.generateApiReference(classes);

            assertNotNull(apiRef);
            assertTrue(apiRef.contains("API Endpoints"));
        }
    }

    @Nested
    @DisplayName("Component Reference")
    class ComponentReferenceTests {

        @Test
        @DisplayName("Should generate component reference")
        void shouldGenerateComponentReference() {
            List<ClassMetadata> classes = createSpringComponents();

            String compRef = generator.generateComponentReference(classes);

            assertNotNull(compRef);
            assertTrue(compRef.contains("Component Summary"));
        }
    }

    // ============= Helper Methods =============

    private JarAnalysisResult createSampleAnalysisResult() {
        JarAnalysisResult result = new JarAnalysisResult();
        result.setJarName("sample-lib-1.0.0.jar");
        result.setClasses(createSpringComponents());
        return result;
    }

    private ClassMetadata createSampleClass() {
        ClassMetadata clazz = new ClassMetadata();
        clazz.setPackageName("com.example.service");
        clazz.setClassName("UserService");
        clazz.setFullyQualifiedName("com.example.service.UserService");
        clazz.setClassType(ClassMetadata.ClassType.CLASS);
        clazz.getAnnotations().add("org.springframework.stereotype.Service");

        MethodMetadata method = new MethodMetadata();
        method.setMethodName("getUser");
        method.setAccessModifiers(Set.of("public"));
        method.setReturnType("User");
        ParameterInfo param = new ParameterInfo();
        param.setName("id");
        param.setType("Long");
        method.getParameters().add(param);
        clazz.getMethods().add(method);

        return clazz;
    }

    private List<ClassMetadata> createSpringComponents() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("UserService");
        service.setFullyQualifiedName("com.example.service.UserService");
        service.setClassType(ClassMetadata.ClassType.CLASS);
        service.getAnnotations().add("org.springframework.stereotype.Service");
        classes.add(service);

        ClassMetadata repo = new ClassMetadata();
        repo.setPackageName("com.example.repository");
        repo.setClassName("UserRepository");
        repo.setFullyQualifiedName("com.example.repository.UserRepository");
        repo.setClassType(ClassMetadata.ClassType.INTERFACE);
        repo.getAnnotations().add("org.springframework.stereotype.Repository");
        classes.add(repo);

        return classes;
    }

    private List<ClassMetadata> createControllerClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RestController");

        MethodMetadata getUsers = new MethodMetadata();
        getUsers.setMethodName("getUsers");
        getUsers.setAccessModifiers(Set.of("public"));
        getUsers.setReturnType("List<User>");
        getUsers.getAnnotations().add("org.springframework.web.bind.annotation.GetMapping");
        controller.getMethods().add(getUsers);

        classes.add(controller);
        return classes;
    }
}

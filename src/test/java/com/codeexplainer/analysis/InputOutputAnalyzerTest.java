package com.codeexplainer.analysis;

import com.codeexplainer.analysis.InputOutputAnalyzer.*;
import com.codeexplainer.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputOutputAnalyzer.
 */
@DisplayName("InputOutputAnalyzer Tests")
class InputOutputAnalyzerTest {

    private InputOutputAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new InputOutputAnalyzer();
    }

    @Nested
    @DisplayName("I/O Analysis")
    class IOAnalysisTests {

        @Test
        @DisplayName("Should analyze method I/O")
        void shouldAnalyzeMethodIO() {
            List<ClassMetadata> classes = createSampleClasses();

            IOAnalysisResult result = analyzer.analyze(classes);

            assertNotNull(result);
            assertFalse(result.methodIOs().isEmpty());
        }

        @Test
        @DisplayName("Should track input type frequency")
        void shouldTrackInputTypeFrequency() {
            List<ClassMetadata> classes = createSampleClasses();

            IOAnalysisResult result = analyzer.analyze(classes);

            assertNotNull(result.inputTypeFrequency());
        }

        @Test
        @DisplayName("Should track output type frequency")
        void shouldTrackOutputTypeFrequency() {
            List<ClassMetadata> classes = createSampleClasses();

            IOAnalysisResult result = analyzer.analyze(classes);

            assertNotNull(result.outputTypeFrequency());
        }
    }

    @Nested
    @DisplayName("DTO Detection")
    class DTODetectionTests {

        @Test
        @DisplayName("Should detect DTOs by naming")
        void shouldDetectDtosByNaming() {
            List<ClassMetadata> classes = createDtoClasses();

            List<DTOInfo> dtos = analyzer.findDataTransferObjects(classes);

            assertFalse(dtos.isEmpty());
        }

        @Test
        @DisplayName("Should identify DTO type")
        void shouldIdentifyDtoType() {
            List<ClassMetadata> classes = createDtoClasses();

            List<DTOInfo> dtos = analyzer.findDataTransferObjects(classes);

            assertTrue(dtos.stream().anyMatch(d -> d.type() == DTOType.REQUEST));
        }
    }

    @Nested
    @DisplayName("Method Categorization")
    class CategorizationTests {

        @Test
        @DisplayName("Should categorize query methods")
        void shouldCategorizeQueryMethods() {
            ClassMetadata clazz = createServiceClass();
            MethodMetadata getUser = clazz.getMethods().stream()
                    .filter(m -> m.getMethodName().equals("getUser"))
                    .findFirst().orElseThrow();

            MethodIOInfo info = analyzer.analyzeMethod(clazz, getUser);

            assertEquals(IOCategory.QUERY, info.category());
        }
    }

    @Nested
    @DisplayName("Report Generation")
    class ReportTests {

        @Test
        @DisplayName("Should generate I/O report")
        void shouldGenerateReport() {
            List<ClassMetadata> classes = createSampleClasses();
            IOAnalysisResult result = analyzer.analyze(classes);

            String report = analyzer.generateReport(result);

            assertNotNull(report);
            assertTrue(report.contains("Input/Output Analysis Report"));
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createSampleClasses() {
        List<ClassMetadata> classes = new ArrayList<>();
        classes.add(createServiceClass());
        return classes;
    }

    private ClassMetadata createServiceClass() {
        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("UserService");
        service.setFullyQualifiedName("com.example.service.UserService");
        service.setClassType(ClassMetadata.ClassType.CLASS);

        MethodMetadata getUser = new MethodMetadata();
        getUser.setMethodName("getUser");
        getUser.setAccessModifiers(Set.of("public"));
        getUser.setReturnType("User");
        ParameterInfo idParam = new ParameterInfo();
        idParam.setName("id");
        idParam.setType("Long");
        getUser.getParameters().add(idParam);
        service.getMethods().add(getUser);

        MethodMetadata saveUser = new MethodMetadata();
        saveUser.setMethodName("saveUser");
        saveUser.setAccessModifiers(Set.of("public"));
        saveUser.setReturnType("User");
        ParameterInfo userParam = new ParameterInfo();
        userParam.setName("user");
        userParam.setType("User");
        saveUser.getParameters().add(userParam);
        service.getMethods().add(saveUser);

        return service;
    }

    private List<ClassMetadata> createDtoClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata request = new ClassMetadata();
        request.setPackageName("com.example.dto");
        request.setClassName("CreateUserRequest");
        request.setFullyQualifiedName("com.example.dto.CreateUserRequest");
        request.setClassType(ClassMetadata.ClassType.CLASS);

        FieldMetadata nameField = new FieldMetadata();
        nameField.setFieldName("name");
        nameField.setType("String");
        request.getFields().add(nameField);

        MethodMetadata getName = new MethodMetadata();
        getName.setMethodName("getName");
        getName.setAccessModifiers(Set.of("public"));
        getName.setReturnType("String");
        request.getMethods().add(getName);

        MethodMetadata setName = new MethodMetadata();
        setName.setMethodName("setName");
        setName.setAccessModifiers(Set.of("public"));
        ParameterInfo param = new ParameterInfo();
        param.setName("name");
        param.setType("String");
        setName.getParameters().add(param);
        request.getMethods().add(setName);

        classes.add(request);
        return classes;
    }
}

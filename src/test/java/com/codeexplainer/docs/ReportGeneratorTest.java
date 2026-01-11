package com.codeexplainer.docs;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.JarAnalysisResult;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.docs.ReportGenerator.*;
import com.codeexplainer.graph.DependencyGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import com.codeexplainer.detector.ComponentDetector;
import com.codeexplainer.detector.EndpointDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportGenerator.
 */
@DisplayName("ReportGenerator Tests")
@ExtendWith(MockitoExtension.class)
class ReportGeneratorTest {

    @Mock
    private DependencyGraphBuilder graphBuilder;
    @Mock
    private EndpointDetector endpointDetector;
    @Mock
    private ComponentDetector componentDetector;
    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ReportGenerator(graphBuilder, endpointDetector, componentDetector);
    }

    @Nested
    @DisplayName("Analysis Report Generation")
    class AnalysisReportTests {

        @Test
        @DisplayName("Should generate analysis report")
        void shouldGenerateAnalysisReport() {
            JarAnalysisResult result = createSampleAnalysisResult();

            AnalysisReport report = generator.generateReport(result);

            assertNotNull(report);
            assertNotNull(report.classMetrics());
            assertNotNull(report.methodMetrics());
        }

        @Test
        @DisplayName("Should calculate class metrics")
        void shouldCalculateClassMetrics() {
            JarAnalysisResult result = createSampleAnalysisResult();

            AnalysisReport report = generator.generateReport(result);

            assertTrue(report.classMetrics().totalClasses() > 0);
        }

        @Test
        @DisplayName("Should calculate method metrics")
        void shouldCalculateMethodMetrics() {
            JarAnalysisResult result = createSampleAnalysisResult();

            AnalysisReport report = generator.generateReport(result);

            assertTrue(report.methodMetrics().totalMethods() >= 0);
        }
    }

    @Nested
    @DisplayName("Markdown Report Generation")
    class MarkdownReportTests {

        @Test
        @DisplayName("Should generate markdown report")
        void shouldGenerateMarkdownReport() {
            JarAnalysisResult result = createSampleAnalysisResult();
            AnalysisReport report = generator.generateReport(result);

            String markdown = generator.generateMarkdownReport(report);

            assertNotNull(markdown);
            assertTrue(markdown.contains("# Analysis Report"));
        }

        @Test
        @DisplayName("Should include class metrics")
        void shouldIncludeClassMetrics() {
            JarAnalysisResult result = createSampleAnalysisResult();
            AnalysisReport report = generator.generateReport(result);

            String markdown = generator.generateMarkdownReport(report);

            assertTrue(markdown.contains("Class Metrics"));
        }

        @Test
        @DisplayName("Should include method metrics")
        void shouldIncludeMethodMetrics() {
            JarAnalysisResult result = createSampleAnalysisResult();
            AnalysisReport report = generator.generateReport(result);

            String markdown = generator.generateMarkdownReport(report);

            assertTrue(markdown.contains("Method Metrics"));
        }
    }

    @Nested
    @DisplayName("Architecture Overview")
    class ArchitectureOverviewTests {

        @Test
        @DisplayName("Should generate architecture overview")
        void shouldGenerateArchitectureOverview() {
            List<ClassMetadata> classes = createLayeredClasses();

            String overview = generator.generateArchitectureOverview(classes);

            assertNotNull(overview);
            assertTrue(overview.contains("Architecture Overview"));
        }

        @Test
        @DisplayName("Should identify application layers")
        void shouldIdentifyLayers() {
            List<ClassMetadata> classes = createLayeredClasses();

            String overview = generator.generateArchitectureOverview(classes);

            assertTrue(overview.contains("Application Layers"));
        }
    }

    @Nested
    @DisplayName("Quality Issues Detection")
    class QualityIssuesTests {

        @Test
        @DisplayName("Should detect large class")
        void shouldDetectLargeClass() {
            JarAnalysisResult result = createResultWithLargeClass();

            AnalysisReport report = generator.generateReport(result);

            assertTrue(report.qualityIssues().stream()
                    .anyMatch(i -> i.type() == IssueType.LARGE_CLASS));
        }
    }

    // ============= Helper Methods =============

    private JarAnalysisResult createSampleAnalysisResult() {
        JarAnalysisResult result = new JarAnalysisResult();
        result.setJarName("sample-lib-1.0.0.jar");
        result.setClasses(createLayeredClasses());
        return result;
    }

    private JarAnalysisResult createResultWithLargeClass() {
        JarAnalysisResult result = new JarAnalysisResult();
        result.setJarName("large-class-lib.jar");

        ClassMetadata largeClass = new ClassMetadata();
        largeClass.setPackageName("com.example");
        largeClass.setClassName("GodClass");
        largeClass.setFullyQualifiedName("com.example.GodClass");
        largeClass.setClassType(ClassMetadata.ClassType.CLASS);

        // Add 35 methods to trigger the warning
        for (int i = 0; i < 35; i++) {
            MethodMetadata method = new MethodMetadata();
            method.setMethodName("method" + i);
            method.setAccessModifiers(Set.of("public"));
            largeClass.getMethods().add(method);
        }

        result.setClasses(List.of(largeClass));
        return result;
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
        controller.getMethods().add(getUsers);
        classes.add(controller);

        // Service
        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("UserService");
        service.setFullyQualifiedName("com.example.service.UserService");
        service.setClassType(ClassMetadata.ClassType.CLASS);
        service.getAnnotations().add("org.springframework.stereotype.Service");
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
}

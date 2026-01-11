package com.codeexplainer.web;

import com.codeexplainer.analysis.DataFlowAnalyzer;
import com.codeexplainer.analysis.InputOutputAnalyzer;
import com.codeexplainer.analyzer.BytecodeAnalysisService;
import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.JarAnalysisResult;
import com.codeexplainer.detector.ComponentDetector;
import com.codeexplainer.detector.EndpointDetector;
import com.codeexplainer.diagram.FlowDiagramGenerator;
import com.codeexplainer.docs.DocumentationGenerator;
import com.codeexplainer.docs.ReportGenerator;
import com.codeexplainer.docs.ReportGenerator.AnalysisReport;
import com.codeexplainer.docs.ReportGenerator.ArchitectureOverview;
import com.codeexplainer.docs.ReportGenerator.ClassMetrics;
import com.codeexplainer.docs.ReportGenerator.DependencyMetrics;
import com.codeexplainer.docs.ReportGenerator.MethodMetrics;
import com.codeexplainer.export.ExportService;
import com.codeexplainer.export.ReportExporter;
import com.codeexplainer.parser.JarContent;
import com.codeexplainer.parser.JarParserService;
import com.codeexplainer.parser.JarParserService.ParseResult;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeExplainerControllerTest {

    @Mock
    private JarParserService jarParser;
    @Mock
    private BytecodeAnalysisService bytecodeAnalyzer;
    @Mock
    private DocumentationGenerator docGenerator;
    @Mock
    private ReportGenerator reportGenerator;
    @Mock
    private EndpointDetector endpointDetector;
    @Mock
    private ComponentDetector componentDetector;
    @Mock
    private InputOutputAnalyzer ioAnalyzer;
    @Mock
    private DataFlowAnalyzer flowAnalyzer;
    @Mock
    private FlowDiagramGenerator diagramGenerator;
    @Mock
    private ExportService exportService;
    @Mock
    private MultipartFile multipartFile;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ReportExporter exporter;
    @Mock
    private ServletOutputStream outputStream;

    private CodeExplainerController controller;

    @BeforeEach
    void setUp() {
        controller = new CodeExplainerController(
                jarParser, bytecodeAnalyzer, docGenerator, reportGenerator,
                endpointDetector, componentDetector, ioAnalyzer, flowAnalyzer,
                diagramGenerator, exportService);
    }

    @Test
    @DisplayName("Should analyze uploaded JAR file")
    void shouldAnalyzeJar() throws Exception {
        // Setup mocks
        when(multipartFile.getOriginalFilename()).thenReturn("test.jar");

        JarContent jarContent = new JarContent();
        jarContent.setExtractedPath("temp");
        jarContent.setClassFiles(List.of("path/to/Test.class"));

        ParseResult parseResult = new ParseResult();
        parseResult.setJarContent(jarContent);

        when(jarParser.parse(any(File.class))).thenReturn(parseResult);

        ClassMetadata classMetadata = new ClassMetadata();
        classMetadata.setClassName("Test");
        classMetadata.setPackageName("com.test");
        classMetadata.setClassType(ClassMetadata.ClassType.CLASS);
        when(bytecodeAnalyzer.analyzeClassFile(any(Path.class))).thenReturn(classMetadata);

        ArchitectureOverview architecture = new ArchitectureOverview(new ArrayList<>(), new ArrayList<>());

        AnalysisReport report = new AnalysisReport(
                "test.jar",
                new ClassMetrics(1, 0, 0, 0, 0, 1),
                new MethodMetrics(0, 0, 0, 0.0),
                new DependencyMetrics(0, 0.0, 0),
                new ArrayList<>(),
                architecture);
        when(reportGenerator.generateReport(any(JarAnalysisResult.class))).thenReturn(report);

        // Execute
        ResponseEntity<CodeExplainerController.AnalysisResponse> response = controller.analyzeJar(multipartFile);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().sessionId());
        assertEquals(1, response.getBody().classCount());

        verify(jarParser).parse(any(File.class));
        verify(bytecodeAnalyzer).analyzeClassFile(any(Path.class));
    }

    @Test
    @DisplayName("Should export report")
    void shouldExportReport() throws Exception {
        // Mock successful analysis (reuse mocking logic or assume session state)
        // Since controller stores sessions in memory, we first need to analyze a jar to
        // create a session

        // .... actually controller.analyzeJar populates the session.
        // So we call analyzeJar first
        shouldAnalyzeJar();

        // Get the session ID from the response (we can't easily capture it from the
        // void method unless we verify the analyze call)
        // But the previous test verified analyzing works.
        // We need to capture the sessionId.
        // Or we can mock `exportService` and calling export endpoint with ANY session
        // ID might fail if session checks map.

        // To properly test this, I'd need to extract the session ID from previous call.
        // For simplicity, let's assume we can mock the internal state or just rely on
        // the flow.
        // Wait, Controller stores sessions in a HashMap private field. I can't inject
        // it.
        // So I MUST call analyzeJar first to put something in the map.

        // Re-execute analyzeJar to get session ID
        ResponseEntity<CodeExplainerController.AnalysisResponse> analysisResp = controller.analyzeJar(multipartFile);
        String sessionId = analysisResp.getBody().sessionId();

        // Setup export mocks
        when(exportService.getExporter("pdf")).thenReturn(exporter);
        when(exporter.getContentType()).thenReturn("application/pdf");
        when(exporter.getFileExtension()).thenReturn("pdf");
        when(response.getOutputStream()).thenReturn(outputStream);

        // Execute export
        controller.exportReport(sessionId, "pdf", response);

        // Verify
        verify(exportService).getExporter("pdf");
        verify(exporter).export(any(AnalysisReport.class), eq(outputStream));
        verify(response).setContentType("application/pdf");
    }

    @Test
    @DisplayName("Should return 400 on analysis failure")
    void shouldReturn400OnFailure() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("test.jar");
        when(jarParser.parse(any(File.class))).thenThrow(new RuntimeException("Parse error"));

        ResponseEntity<CodeExplainerController.AnalysisResponse> response = controller.analyzeJar(multipartFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("Parse error"));
    }
}

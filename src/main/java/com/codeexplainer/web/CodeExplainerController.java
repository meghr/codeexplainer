package com.codeexplainer.web;

import com.codeexplainer.analysis.DataFlowAnalyzer;
import com.codeexplainer.analysis.DataFlowAnalyzer.*;
import com.codeexplainer.analysis.InputOutputAnalyzer;
import com.codeexplainer.analysis.InputOutputAnalyzer.*;
import com.codeexplainer.analyzer.BytecodeAnalysisService;
import com.codeexplainer.core.model.*;
import com.codeexplainer.detector.ComponentDetector;
import com.codeexplainer.detector.ComponentDetector.DetectedComponent;
import com.codeexplainer.detector.EndpointDetector;
import com.codeexplainer.diagram.FlowDiagramGenerator;
import com.codeexplainer.docs.DocumentationGenerator;
import com.codeexplainer.docs.DocumentationGenerator.*;
import com.codeexplainer.docs.ReportGenerator;
import com.codeexplainer.docs.ReportGenerator.AnalysisReport;
import com.codeexplainer.docs.ReportGenerator.ClassMetrics;
import com.codeexplainer.docs.ReportGenerator.MethodMetrics;
import com.codeexplainer.export.ExportService;
import com.codeexplainer.export.ReportExporter;
import com.codeexplainer.parser.JarParserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * REST API controller for Code Explainer operations.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CodeExplainerController {

    private static final Logger log = LoggerFactory.getLogger(CodeExplainerController.class);

    private final JarParserService jarParser;
    private final BytecodeAnalysisService bytecodeAnalyzer;
    private final DocumentationGenerator docGenerator;
    private final ReportGenerator reportGenerator;
    private final EndpointDetector endpointDetector;
    private final ComponentDetector componentDetector;
    private final InputOutputAnalyzer ioAnalyzer;
    private final DataFlowAnalyzer flowAnalyzer;
    private final FlowDiagramGenerator diagramGenerator;
    private final ExportService exportService;

    // Store analysis results for session
    private final Map<String, AnalysisSession> sessions = new HashMap<>();

    public CodeExplainerController(JarParserService jarParser,
            BytecodeAnalysisService bytecodeAnalyzer,
            DocumentationGenerator docGenerator,
            ReportGenerator reportGenerator,
            EndpointDetector endpointDetector,
            ComponentDetector componentDetector,
            InputOutputAnalyzer ioAnalyzer,
            DataFlowAnalyzer flowAnalyzer,
            FlowDiagramGenerator diagramGenerator,
            ExportService exportService) {
        this.jarParser = jarParser;
        this.bytecodeAnalyzer = bytecodeAnalyzer;
        this.docGenerator = docGenerator;
        this.reportGenerator = reportGenerator;
        this.endpointDetector = endpointDetector;
        this.componentDetector = componentDetector;
        this.ioAnalyzer = ioAnalyzer;
        this.flowAnalyzer = flowAnalyzer;
        this.diagramGenerator = diagramGenerator;
        this.exportService = exportService;
    }

    /**
     * Upload and analyze a JAR file.
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyzeJar(@RequestParam("file") MultipartFile file) {
        log.info("Received JAR for analysis: {}", file.getOriginalFilename());

        try {
            // Save uploaded file temporarily
            Path tempFile = Files.createTempFile("jar-", ".jar");
            file.transferTo(tempFile.toFile());

            // Parse the JAR
            JarParserService.ParseResult parseResult = jarParser.parse(tempFile.toFile());

            // Read class metadata from class files
            List<ClassMetadata> classes = new ArrayList<>();
            for (String classFilePath : parseResult.getJarContent().getClassFiles()) {
                try {
                    ClassMetadata metadata = bytecodeAnalyzer.analyzeClassFile(
                            java.nio.file.Paths.get(classFilePath));
                    classes.add(metadata);
                } catch (Exception e) {
                    log.debug("Could not read class file: {}", classFilePath);
                }
            }

            // Create analysis result
            JarAnalysisResult analysisResult = new JarAnalysisResult();
            analysisResult.setJarName(file.getOriginalFilename());
            analysisResult.setClasses(classes);

            // Create session
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, new AnalysisSession(sessionId, analysisResult));

            // Generate summary
            AnalysisReport report = reportGenerator.generateReport(analysisResult);

            // Clean up
            jarParser.cleanup(parseResult);
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(new AnalysisResponse(
                    sessionId,
                    file.getOriginalFilename(),
                    classes.size(),
                    report.classMetrics(),
                    report.methodMetrics(),
                    "Analysis complete"));

        } catch (Exception e) {
            log.error("Error analyzing JAR", e);
            return ResponseEntity.badRequest().body(
                    new AnalysisResponse(null, file.getOriginalFilename(), 0, null, null,
                            "Error: " + e.getMessage()));
        }
    }

    /**
     * Get classes for a session.
     */
    @GetMapping("/sessions/{sessionId}/classes")
    public ResponseEntity<List<ClassSummary>> getClasses(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        List<ClassSummary> summaries = session.result().getClasses().stream()
                .map(c -> new ClassSummary(
                        c.getClassName(),
                        c.getPackageName(),
                        c.getClassType().toString(),
                        c.getMethods().size(),
                        c.getFields().size()))
                .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * Get endpoints for a session.
     */
    @GetMapping("/sessions/{sessionId}/endpoints")
    public ResponseEntity<List<EndpointInfo>> getEndpoints(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        List<EndpointInfo> endpoints = endpointDetector.detectEndpoints(session.result().getClasses());
        return ResponseEntity.ok(endpoints);
    }

    /**
     * Get components for a session.
     */
    @GetMapping("/sessions/{sessionId}/components")
    public ResponseEntity<List<DetectedComponent>> getComponents(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        List<DetectedComponent> components = componentDetector.detectComponents(session.result().getClasses());
        return ResponseEntity.ok(components);
    }

    /**
     * Get documentation for a session.
     */
    @GetMapping("/sessions/{sessionId}/docs")
    public ResponseEntity<GeneratedDocumentation> getDocumentation(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        GeneratedDocumentation docs = docGenerator.generate(session.result(), DocOptions.full());
        return ResponseEntity.ok(docs);
    }

    /**
     * Get I/O analysis for a session.
     */
    @GetMapping("/sessions/{sessionId}/io-analysis")
    public ResponseEntity<IOAnalysisResult> getIOAnalysis(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        IOAnalysisResult result = ioAnalyzer.analyze(session.result().getClasses());
        return ResponseEntity.ok(result);
    }

    /**
     * Get class diagram.
     */
    @GetMapping("/sessions/{sessionId}/diagram")
    public ResponseEntity<String> getDiagram(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        String plantUml = diagramGenerator.generateClassDiagram(session.result().getClasses());
        // For demonstration, we simply return the PlantUML source.
        // In a real app with Graphviz, we'd render to SVG.
        // But since we can't guarantee Graphviz, we'll return text/plain or try a
        // web-renderer url.
        // Let's assume frontend can render PlantUML or we return basic text.
        // Actually, let's verify if we can include a JS renderer (plantuml-encoder) in
        // frontend.
        return ResponseEntity.ok(plantUml);
    }

    /**
     * Get data flow analysis.
     */
    @GetMapping("/sessions/{sessionId}/data-flow")
    public ResponseEntity<DataFlowResult> getDataFlow(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        DataFlowResult result = flowAnalyzer.analyzeFlow(session.result().getClasses());
        return ResponseEntity.ok(result);
    }

    /**
     * Get analysis report.
     */
    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<String> getReport(@PathVariable String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        AnalysisReport report = reportGenerator.generateReport(session.result());
        String markdown = reportGenerator.generateMarkdownReport(report);
        return ResponseEntity.ok(markdown);
    }

    @GetMapping("/sessions/{sessionId}/export")
    public void exportReport(@PathVariable String sessionId,
            @RequestParam(defaultValue = "json") String format,
            HttpServletResponse response) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        AnalysisReport report = reportGenerator.generateReport(session.result());

        ReportExporter exporter = exportService.getExporter(format);

        response.setContentType(exporter.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"report." + exporter.getFileExtension() + "\"");

        try {
            exporter.export(report, response.getOutputStream());
        } catch (IOException e) {
            log.error("Failed to export report for session {}", sessionId, e);
            throw new RuntimeException("Failed to export report", e);
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Code Explainer",
                "sessions", String.valueOf(sessions.size())));
    }

    // ============= Records =============

    public record AnalysisResponse(
            String sessionId,
            String fileName,
            int classCount,
            ClassMetrics classMetrics,
            MethodMetrics methodMetrics,
            String message) {
    }

    public record ClassSummary(
            String className,
            String packageName,
            String type,
            int methodCount,
            int fieldCount) {
    }

    public record AnalysisSession(
            String id,
            JarAnalysisResult result) {
    }
}

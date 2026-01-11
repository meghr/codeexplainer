package com.codeexplainer.analyzer;

import com.codeexplainer.core.exception.BytecodeAnalysisException;
import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.parser.ClassFileReader;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service that orchestrates bytecode analysis operations.
 * Combines BytecodeVisitor, MethodAnalyzer, AnnotationExtractor, and
 * InstructionParser
 * to provide comprehensive class analysis.
 */
@Service
public class BytecodeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(BytecodeAnalysisService.class);

    private final ClassFileReader classFileReader;

    public BytecodeAnalysisService(ClassFileReader classFileReader) {
        this.classFileReader = classFileReader;
    }

    /**
     * Analyzes a single class file and extracts its metadata.
     *
     * @param classFilePath Path to the .class file
     * @return ClassMetadata containing all extracted information
     * @throws BytecodeAnalysisException if analysis fails
     */
    public ClassMetadata analyzeClassFile(Path classFilePath) {
        Objects.requireNonNull(classFilePath, "Class file path cannot be null");
        log.debug("Analyzing class file: {}", classFilePath);

        try {
            byte[] classBytes = Files.readAllBytes(classFilePath);
            return analyzeClassBytes(classBytes);
        } catch (IOException e) {
            throw new BytecodeAnalysisException(
                    "Failed to read class file: " + classFilePath, e);
        }
    }

    /**
     * Analyzes class bytecode from raw bytes.
     *
     * @param classBytes The bytecode as a byte array
     * @return ClassMetadata containing all extracted information
     * @throws BytecodeAnalysisException if analysis fails
     */
    public ClassMetadata analyzeClassBytes(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "Class bytes cannot be null");

        try {
            ClassReader classReader = new ClassReader(classBytes);
            return analyzeWithClassReader(classReader);
        } catch (IllegalArgumentException e) {
            throw new BytecodeAnalysisException("Invalid class bytecode", e);
        }
    }

    /**
     * Analyzes class from an input stream.
     *
     * @param inputStream Input stream containing class bytecode
     * @return ClassMetadata containing all extracted information
     * @throws BytecodeAnalysisException if analysis fails
     */
    public ClassMetadata analyzeClassStream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Input stream cannot be null");

        try {
            ClassReader classReader = new ClassReader(inputStream);
            return analyzeWithClassReader(classReader);
        } catch (IOException e) {
            throw new BytecodeAnalysisException("Failed to read class from stream", e);
        }
    }

    /**
     * Analyzes a class with options.
     *
     * @param classBytes Raw bytecode
     * @param options    Analysis options
     * @return ClassMetadata with analysis results
     */
    public ClassMetadata analyzeClassBytes(byte[] classBytes, AnalysisOptions options) {
        Objects.requireNonNull(classBytes, "Class bytes cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");

        try {
            ClassReader classReader = new ClassReader(classBytes);
            BytecodeVisitor visitor = new BytecodeVisitor(options.includePrivateMethods());
            classReader.accept(visitor, options.parsingFlags());
            return visitor.getClassMetadata();
        } catch (IllegalArgumentException e) {
            throw new BytecodeAnalysisException("Invalid class bytecode", e);
        }
    }

    /**
     * Batch analyzes multiple class files.
     *
     * @param classPaths List of paths to class files
     * @return List of ClassMetadata for each successfully analyzed class
     */
    public List<ClassMetadata> analyzeClassFiles(List<Path> classPaths) {
        Objects.requireNonNull(classPaths, "Class paths cannot be null");

        List<ClassMetadata> results = new ArrayList<>();
        int errorCount = 0;

        for (Path classPath : classPaths) {
            try {
                ClassMetadata metadata = analyzeClassFile(classPath);
                results.add(metadata);
            } catch (BytecodeAnalysisException e) {
                log.warn("Failed to analyze {}: {}", classPath, e.getMessage());
                errorCount++;
            }
        }

        log.info("Analyzed {} classes, {} errors", results.size(), errorCount);
        return results;
    }

    /**
     * Gets detailed instruction analysis for a class.
     * This performs a deeper analysis including instruction-level details.
     *
     * @param classBytes Raw bytecode
     * @return AnalysisResult containing class metadata and instruction stats
     */
    public DetailedAnalysisResult analyzeDetailedInstructions(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "Class bytes cannot be null");

        ClassReader classReader = new ClassReader(classBytes);
        BytecodeVisitor visitor = new BytecodeVisitor(true);
        classReader.accept(visitor, ClassReader.EXPAND_FRAMES);

        ClassMetadata metadata = visitor.getClassMetadata();

        // Collect instruction stats for each method
        List<MethodInstructionStats> methodStats = new ArrayList<>();
        for (var method : metadata.getMethods()) {
            // Note: In a real implementation, we'd need to re-parse with InstructionParser
            // For now, we'll estimate stats from method invocations count
            methodStats.add(new MethodInstructionStats(
                    method.getMethodName(),
                    method.getDescriptor(),
                    method.getInvocations() != null ? method.getInvocations().size() : 0));
        }

        return new DetailedAnalysisResult(metadata, methodStats);
    }

    /**
     * Validates if bytecode is a valid Java class.
     *
     * @param classBytes Raw bytecode
     * @return true if valid, false otherwise
     */
    public boolean isValidClass(byte[] classBytes) {
        if (classBytes == null || classBytes.length < 4) {
            return false;
        }
        // Check magic number (CAFEBABE)
        return ((classBytes[0] & 0xFF) == 0xCA &&
                (classBytes[1] & 0xFF) == 0xFE &&
                (classBytes[2] & 0xFF) == 0xBA &&
                (classBytes[3] & 0xFF) == 0xBE);
    }

    /**
     * Gets the Java version from class bytes.
     *
     * @param classBytes Raw bytecode
     * @return Java version string (e.g., "Java 17")
     */
    public String getJavaVersion(byte[] classBytes) {
        if (!isValidClass(classBytes)) {
            return "Unknown";
        }
        int majorVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
        return classFileReader.getJavaVersion(majorVersion);
    }

    private ClassMetadata analyzeWithClassReader(ClassReader classReader) {
        BytecodeVisitor visitor = new BytecodeVisitor();
        classReader.accept(visitor, ClassReader.SKIP_DEBUG);
        return visitor.getClassMetadata();
    }

    // ============= Records for Results =============

    /**
     * Options for class analysis.
     */
    public record AnalysisOptions(
            boolean includePrivateMethods,
            int parsingFlags) {
        public static AnalysisOptions defaults() {
            return new AnalysisOptions(false, ClassReader.SKIP_DEBUG);
        }

        public static AnalysisOptions full() {
            return new AnalysisOptions(true, ClassReader.EXPAND_FRAMES);
        }
    }

    /**
     * Detailed analysis result including instruction statistics.
     */
    public record DetailedAnalysisResult(
            ClassMetadata classMetadata,
            List<MethodInstructionStats> methodStats) {
    }

    /**
     * Instruction statistics for a single method.
     */
    public record MethodInstructionStats(
            String methodName,
            String descriptor,
            int invocationCount) {
    }
}

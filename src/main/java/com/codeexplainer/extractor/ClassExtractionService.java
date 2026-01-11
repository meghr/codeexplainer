package com.codeexplainer.extractor;

import com.codeexplainer.analyzer.BytecodeAnalysisService;
import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.FieldMetadata;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.parser.JarContent;
import com.codeexplainer.parser.JarParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting structured class metadata from JAR files.
 * Provides hierarchical views of packages, classes, methods, and fields.
 */
@Service
public class ClassExtractionService {

        private static final Logger log = LoggerFactory.getLogger(ClassExtractionService.class);

        private final JarParserService jarParserService;
        private final BytecodeAnalysisService bytecodeAnalysisService;

        public ClassExtractionService(JarParserService jarParserService,
                        BytecodeAnalysisService bytecodeAnalysisService) {
                this.jarParserService = jarParserService;
                this.bytecodeAnalysisService = bytecodeAnalysisService;
        }

        /**
         * Extracts all class metadata from a JAR file.
         *
         * @param jarPath Path to the JAR file
         * @return ExtractionResult containing all extracted metadata
         */
        public ExtractionResult extractFromJar(Path jarPath) {
                log.info("Starting class extraction from JAR: {}", jarPath);

                JarParserService.ParseResult parseResult = jarParserService.parse(jarPath.toFile());
                JarContent jarContent = parseResult.getJarContent();
                List<ClassMetadata> allClasses = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                for (String classFilePath : jarContent.getClassFiles()) {
                        try {
                                Path classFile = Paths.get(classFilePath);
                                ClassMetadata metadata = bytecodeAnalysisService.analyzeClassFile(classFile);
                                allClasses.add(metadata);
                        } catch (Exception e) {
                                log.warn("Failed to analyze class: {}", classFilePath, e);
                                errors.add(classFilePath + ": " + e.getMessage());
                        }
                }

                log.info("Extracted {} classes from JAR, {} errors", allClasses.size(), errors.size());

                return new ExtractionResult(
                                jarPath.getFileName().toString(),
                                allClasses,
                                buildPackageHierarchy(allClasses),
                                errors);
        }

        /**
         * Extracts class metadata from raw bytecode.
         *
         * @param classBytes Raw class bytecode
         * @return ClassMetadata for the class
         */
        public ClassMetadata extractFromBytes(byte[] classBytes) {
                return bytecodeAnalysisService.analyzeClassBytes(classBytes);
        }

        /**
         * Filters classes by various criteria.
         *
         * @param classes List of class metadata
         * @param filter  Filter criteria
         * @return Filtered list of classes
         */
        public List<ClassMetadata> filterClasses(List<ClassMetadata> classes, ClassFilter filter) {
                return classes.stream()
                                .filter(c -> filter.packagePrefix() == null ||
                                                c.getPackageName().startsWith(filter.packagePrefix()))
                                .filter(c -> filter.classTypes() == null || filter.classTypes().isEmpty() ||
                                                filter.classTypes().contains(c.getClassType()))
                                .filter(c -> !filter.excludeInnerClasses() || !c.getClassName().contains("$"))
                                .filter(c -> filter.annotations() == null || filter.annotations().isEmpty() ||
                                                c.getAnnotations().stream()
                                                                .anyMatch(a -> filter.annotations().stream()
                                                                                .anyMatch(a::contains)))
                                .collect(Collectors.toList());
        }

        /**
         * Builds a package hierarchy from a list of classes.
         */
        public Map<String, PackageDetails> buildPackageHierarchy(List<ClassMetadata> classes) {
                Map<String, PackageDetails> packages = new LinkedHashMap<>();

                for (ClassMetadata classMetadata : classes) {
                        String packageName = classMetadata.getPackageName();
                        if (packageName == null || packageName.isEmpty()) {
                                packageName = "(default)";
                        }

                        packages.computeIfAbsent(packageName, name -> new PackageDetails(
                                        name,
                                        new ArrayList<>())).classes().add(classMetadata);
                }

                return packages;
        }

        /**
         * Creates a summary of the extraction results.
         */
        public ExtractionSummary createSummary(ExtractionResult result) {
                List<ClassMetadata> classes = result.classes();

                int totalClasses = classes.size();
                int interfaces = (int) classes.stream()
                                .filter(c -> c.getClassType() == ClassMetadata.ClassType.INTERFACE).count();
                int enums = (int) classes.stream()
                                .filter(c -> c.getClassType() == ClassMetadata.ClassType.ENUM).count();
                int annotations = (int) classes.stream()
                                .filter(c -> c.getClassType() == ClassMetadata.ClassType.ANNOTATION).count();
                int abstractClasses = (int) classes.stream()
                                .filter(c -> c.getClassType() == ClassMetadata.ClassType.ABSTRACT_CLASS).count();

                int totalMethods = classes.stream()
                                .mapToInt(c -> c.getMethods().size()).sum();
                int totalFields = classes.stream()
                                .mapToInt(c -> c.getFields().size()).sum();
                int publicMethods = classes.stream()
                                .flatMap(c -> c.getMethods().stream())
                                .filter(m -> m.getAccessModifiers().contains("public"))
                                .mapToInt(m -> 1).sum();

                Set<String> uniqueAnnotations = classes.stream()
                                .flatMap(c -> c.getAnnotations().stream())
                                .collect(Collectors.toSet());

                return new ExtractionSummary(
                                result.jarName(),
                                totalClasses,
                                interfaces,
                                enums,
                                annotations,
                                abstractClasses,
                                result.packages().size(),
                                totalMethods,
                                totalFields,
                                publicMethods,
                                uniqueAnnotations,
                                result.errors().size());
        }

        /**
         * Finds classes that extend a specific class.
         */
        public List<ClassMetadata> findSubclasses(List<ClassMetadata> classes, String superClassName) {
                return classes.stream()
                                .filter(c -> superClassName.equals(c.getSuperClassName()) ||
                                                (c.getSuperClassName() != null && c.getSuperClassName()
                                                                .endsWith("." + superClassName)))
                                .collect(Collectors.toList());
        }

        /**
         * Finds classes that implement a specific interface.
         */
        public List<ClassMetadata> findImplementors(List<ClassMetadata> classes, String interfaceName) {
                return classes.stream()
                                .filter(c -> c.getInterfaces().stream()
                                                .anyMatch(i -> i.equals(interfaceName)
                                                                || i.endsWith("." + interfaceName)))
                                .collect(Collectors.toList());
        }

        /**
         * Finds classes with a specific annotation.
         */
        public List<ClassMetadata> findByAnnotation(List<ClassMetadata> classes, String annotationName) {
                return classes.stream()
                                .filter(c -> c.getAnnotations().stream()
                                                .anyMatch(a -> a.equals(annotationName) ||
                                                                a.endsWith("." + annotationName) ||
                                                                a.contains(annotationName)))
                                .collect(Collectors.toList());
        }

        /**
         * Gets statistics about methods across all classes.
         */
        public MethodStatistics getMethodStatistics(List<ClassMetadata> classes) {
                List<MethodMetadata> allMethods = classes.stream()
                                .flatMap(c -> c.getMethods().stream())
                                .toList();

                int total = allMethods.size();
                int staticMethods = (int) allMethods.stream().filter(MethodMetadata::isStatic).count();
                int abstractMethods = (int) allMethods.stream().filter(MethodMetadata::isAbstract).count();
                int publicMethods = (int) allMethods.stream()
                                .filter(m -> m.getAccessModifiers().contains("public")).count();
                int privateMethods = (int) allMethods.stream()
                                .filter(m -> m.getAccessModifiers().contains("private")).count();

                // Count methods with specific annotations
                int deprecatedMethods = (int) allMethods.stream()
                                .filter(m -> m.getAnnotations().stream().anyMatch(a -> a.contains("Deprecated")))
                                .count();
                int overrideMethods = (int) allMethods.stream()
                                .filter(m -> m.getAnnotations().stream().anyMatch(a -> a.contains("Override")))
                                .count();

                double avgParameters = allMethods.stream()
                                .mapToInt(m -> m.getParameters().size())
                                .average().orElse(0.0);

                return new MethodStatistics(
                                total, staticMethods, abstractMethods,
                                publicMethods, privateMethods,
                                deprecatedMethods, overrideMethods, avgParameters);
        }

        /**
         * Gets statistics about fields across all classes.
         */
        public FieldStatistics getFieldStatistics(List<ClassMetadata> classes) {
                List<FieldMetadata> allFields = classes.stream()
                                .flatMap(c -> c.getFields().stream())
                                .toList();

                int total = allFields.size();
                int staticFields = (int) allFields.stream().filter(FieldMetadata::isStatic).count();
                int finalFields = (int) allFields.stream().filter(FieldMetadata::isFinal).count();
                int volatileFields = (int) allFields.stream().filter(FieldMetadata::isVolatile).count();

                // Group by type
                Map<String, Long> typeDistribution = allFields.stream()
                                .collect(Collectors.groupingBy(
                                                f -> simplifyTypeName(f.getType()),
                                                Collectors.counting()));

                return new FieldStatistics(total, staticFields, finalFields, volatileFields, typeDistribution);
        }

        private String simplifyTypeName(String typeName) {
                if (typeName == null)
                        return "unknown";
                if (typeName.startsWith("java.lang.")) {
                        return typeName.substring("java.lang.".length());
                }
                if (typeName.contains(".")) {
                        return typeName.substring(typeName.lastIndexOf('.') + 1);
                }
                return typeName;
        }

        // ============= Result Records =============

        /**
         * Package details with list of classes.
         */
        public record PackageDetails(
                        String name,
                        List<ClassMetadata> classes) {
                public int classCount() {
                        return classes.size();
                }

                public int interfaceCount() {
                        return (int) classes.stream()
                                        .filter(c -> c.getClassType() == ClassMetadata.ClassType.INTERFACE).count();
                }

                public int enumCount() {
                        return (int) classes.stream()
                                        .filter(c -> c.getClassType() == ClassMetadata.ClassType.ENUM).count();
                }
        }

        /**
         * Result of class extraction from a JAR.
         */
        public record ExtractionResult(
                        String jarName,
                        List<ClassMetadata> classes,
                        Map<String, PackageDetails> packages,
                        List<String> errors) {
        }

        /**
         * Summary of extraction results.
         */
        public record ExtractionSummary(
                        String jarName,
                        int totalClasses,
                        int interfaces,
                        int enums,
                        int annotations,
                        int abstractClasses,
                        int packages,
                        int totalMethods,
                        int totalFields,
                        int publicMethods,
                        Set<String> uniqueAnnotations,
                        int errorCount) {
        }

        /**
         * Filter criteria for classes.
         */
        public record ClassFilter(
                        String packagePrefix,
                        Set<ClassMetadata.ClassType> classTypes,
                        boolean excludeInnerClasses,
                        Set<String> annotations) {
                public static ClassFilter all() {
                        return new ClassFilter(null, null, false, null);
                }

                public static ClassFilter forPackage(String prefix) {
                        return new ClassFilter(prefix, null, false, null);
                }

                public static ClassFilter excludingInner() {
                        return new ClassFilter(null, null, true, null);
                }
        }

        /**
         * Statistics about methods.
         */
        public record MethodStatistics(
                        int total,
                        int staticMethods,
                        int abstractMethods,
                        int publicMethods,
                        int privateMethods,
                        int deprecatedMethods,
                        int overrideMethods,
                        double averageParameterCount) {
        }

        /**
         * Statistics about fields.
         */
        public record FieldStatistics(
                        int total,
                        int staticFields,
                        int finalFields,
                        int volatileFields,
                        Map<String, Long> typeDistribution) {
        }
}

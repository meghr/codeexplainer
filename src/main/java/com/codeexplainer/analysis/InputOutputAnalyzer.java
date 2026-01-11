package com.codeexplainer.analysis;

import com.codeexplainer.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing method inputs (parameters) and outputs (return types).
 * Provides insights into data flow, type usage, and API contracts.
 */
@Service
public class InputOutputAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(InputOutputAnalyzer.class);

    /**
     * Analyzes all methods and extracts input/output information.
     */
    public IOAnalysisResult analyze(List<ClassMetadata> classes) {
        log.info("Analyzing I/O for {} classes", classes.size());

        List<MethodIOInfo> methodIOs = new ArrayList<>();
        Map<String, Integer> inputTypes = new HashMap<>();
        Map<String, Integer> outputTypes = new HashMap<>();

        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getMethodName().startsWith("<"))
                    continue;

                MethodIOInfo ioInfo = analyzeMethod(clazz, method);
                methodIOs.add(ioInfo);

                // Count input types
                for (String type : ioInfo.inputTypes()) {
                    inputTypes.merge(simplifyType(type), 1, Integer::sum);
                }

                // Count output type
                if (ioInfo.outputType() != null) {
                    outputTypes.merge(simplifyType(ioInfo.outputType()), 1, Integer::sum);
                }
            }
        }

        return new IOAnalysisResult(
                methodIOs,
                inputTypes,
                outputTypes,
                findDataTransferObjects(classes),
                findRequestResponsePairs(methodIOs));
    }

    /**
     * Analyzes a single method's I/O.
     */
    public MethodIOInfo analyzeMethod(ClassMetadata clazz, MethodMetadata method) {
        List<String> inputTypes = method.getParameters().stream()
                .map(ParameterInfo::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<String> inputNames = method.getParameters().stream()
                .map(ParameterInfo::getName)
                .collect(Collectors.toList());

        String outputType = method.getReturnType();

        boolean isVoid = outputType == null || "void".equals(outputType);
        boolean hasComplexInput = inputTypes.stream()
                .anyMatch(this::isComplexType);
        boolean hasComplexOutput = !isVoid && isComplexType(outputType);

        IOCategory category = categorizeMethod(method, inputTypes, outputType);

        return new MethodIOInfo(
                clazz.getFullyQualifiedName(),
                clazz.getClassName(),
                method.getMethodName(),
                inputTypes,
                inputNames,
                outputType,
                isVoid,
                hasComplexInput,
                hasComplexOutput,
                category);
    }

    /**
     * Finds potential Data Transfer Objects (DTOs).
     */
    public List<DTOInfo> findDataTransferObjects(List<ClassMetadata> classes) {
        List<DTOInfo> dtos = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            if (isDtoClass(clazz)) {
                List<String> fields = clazz.getFields().stream()
                        .map(f -> f.getFieldName() + ": " + simplifyType(f.getType()))
                        .collect(Collectors.toList());

                dtos.add(new DTOInfo(
                        clazz.getClassName(),
                        clazz.getFullyQualifiedName(),
                        fields,
                        detectDtoType(clazz)));
            }
        }

        return dtos;
    }

    /**
     * Finds request/response pairs in API methods.
     */
    public List<RequestResponsePair> findRequestResponsePairs(List<MethodIOInfo> methodIOs) {
        List<RequestResponsePair> pairs = new ArrayList<>();

        for (MethodIOInfo io : methodIOs) {
            if (io.category() == IOCategory.REQUEST_HANDLER) {
                String requestType = io.inputTypes().stream()
                        .filter(t -> t.contains("Request") || t.contains("DTO") || isComplexType(t))
                        .findFirst()
                        .orElse(null);

                String responseType = io.isVoid() ? null : io.outputType();

                if (requestType != null || responseType != null) {
                    pairs.add(new RequestResponsePair(
                            io.className() + "." + io.methodName(),
                            requestType,
                            responseType));
                }
            }
        }

        return pairs;
    }

    /**
     * Gets type usage statistics.
     */
    public TypeUsageStats getTypeUsageStats(IOAnalysisResult result) {
        List<Map.Entry<String, Integer>> topInputTypes = result.inputTypeFrequency().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<Map.Entry<String, Integer>> topOutputTypes = result.outputTypeFrequency().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        long voidMethods = result.methodIOs().stream()
                .filter(MethodIOInfo::isVoid)
                .count();

        long complexInputMethods = result.methodIOs().stream()
                .filter(MethodIOInfo::hasComplexInput)
                .count();

        long complexOutputMethods = result.methodIOs().stream()
                .filter(MethodIOInfo::hasComplexOutput)
                .count();

        return new TypeUsageStats(
                topInputTypes,
                topOutputTypes,
                result.methodIOs().size(),
                (int) voidMethods,
                (int) complexInputMethods,
                (int) complexOutputMethods);
    }

    /**
     * Generates I/O analysis report.
     */
    public String generateReport(IOAnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Input/Output Analysis Report\n\n");

        TypeUsageStats stats = getTypeUsageStats(result);

        // Summary
        sb.append("## Summary\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Total Methods | ").append(stats.totalMethods()).append(" |\n");
        sb.append("| Void Methods | ").append(stats.voidMethods()).append(" |\n");
        sb.append("| Complex Input Methods | ").append(stats.complexInputMethods()).append(" |\n");
        sb.append("| Complex Output Methods | ").append(stats.complexOutputMethods()).append(" |\n");
        sb.append("| DTOs Detected | ").append(result.dtos().size()).append(" |\n\n");

        // Top input types
        sb.append("## Most Used Input Types\n\n");
        sb.append("| Type | Usage Count |\n");
        sb.append("|------|------------|\n");
        for (var entry : stats.topInputTypes()) {
            sb.append("| `").append(entry.getKey()).append("` | ")
                    .append(entry.getValue()).append(" |\n");
        }
        sb.append("\n");

        // Top output types
        sb.append("## Most Used Output Types\n\n");
        sb.append("| Type | Usage Count |\n");
        sb.append("|------|------------|\n");
        for (var entry : stats.topOutputTypes()) {
            sb.append("| `").append(entry.getKey()).append("` | ")
                    .append(entry.getValue()).append(" |\n");
        }
        sb.append("\n");

        // DTOs
        if (!result.dtos().isEmpty()) {
            sb.append("## Data Transfer Objects\n\n");
            for (DTOInfo dto : result.dtos()) {
                sb.append("### ").append(dto.className()).append("\n\n");
                sb.append("**Type:** ").append(dto.type()).append("\n\n");
                if (!dto.fields().isEmpty()) {
                    sb.append("**Fields:**\n");
                    for (String field : dto.fields()) {
                        sb.append("- `").append(field).append("`\n");
                    }
                    sb.append("\n");
                }
            }
        }

        // Request/Response pairs
        if (!result.requestResponsePairs().isEmpty()) {
            sb.append("## Request/Response Pairs\n\n");
            sb.append("| Endpoint | Request Type | Response Type |\n");
            sb.append("|----------|--------------|---------------|\n");
            for (RequestResponsePair pair : result.requestResponsePairs()) {
                sb.append("| `").append(pair.methodName()).append("` | `")
                        .append(pair.requestType() != null ? simplifyType(pair.requestType()) : "N/A")
                        .append("` | `")
                        .append(pair.responseType() != null ? simplifyType(pair.responseType()) : "void")
                        .append("` |\n");
            }
        }

        return sb.toString();
    }

    // ============= Private Methods =============

    private IOCategory categorizeMethod(MethodMetadata method, List<String> inputs, String output) {
        // Check for REST annotations
        boolean isRest = method.getAnnotations().stream()
                .anyMatch(a -> a.contains("Mapping") || a.contains("Path"));
        if (isRest)
            return IOCategory.REQUEST_HANDLER;

        // Check for service layer patterns
        String name = method.getMethodName().toLowerCase();
        if (name.startsWith("get") || name.startsWith("find") || name.startsWith("load")) {
            return IOCategory.QUERY;
        }
        if (name.startsWith("save") || name.startsWith("create") ||
                name.startsWith("update") || name.startsWith("delete")) {
            return IOCategory.COMMAND;
        }
        if (name.startsWith("process") || name.startsWith("handle")) {
            return IOCategory.PROCESSOR;
        }
        if (name.startsWith("validate") || name.startsWith("check")) {
            return IOCategory.VALIDATOR;
        }
        if (name.startsWith("convert") || name.startsWith("map") || name.startsWith("transform")) {
            return IOCategory.TRANSFORMER;
        }

        return IOCategory.GENERAL;
    }

    private boolean isDtoClass(ClassMetadata clazz) {
        String name = clazz.getClassName();
        // Check naming conventions
        if (name.endsWith("DTO") || name.endsWith("Dto") ||
                name.endsWith("Request") || name.endsWith("Response") ||
                name.endsWith("Model") || name.endsWith("Vo")) {
            return true;
        }
        // Check if mostly fields with getters/setters
        if (clazz.getClassType() == ClassMetadata.ClassType.CLASS ||
                clazz.getClassType() == ClassMetadata.ClassType.RECORD) {
            int fieldCount = clazz.getFields().size();
            long getterSetterCount = clazz.getMethods().stream()
                    .filter(m -> m.getMethodName().startsWith("get") ||
                            m.getMethodName().startsWith("set") ||
                            m.getMethodName().startsWith("is"))
                    .count();
            return fieldCount > 0 && getterSetterCount >= fieldCount;
        }
        return false;
    }

    private DTOType detectDtoType(ClassMetadata clazz) {
        String name = clazz.getClassName();
        if (name.endsWith("Request"))
            return DTOType.REQUEST;
        if (name.endsWith("Response"))
            return DTOType.RESPONSE;
        if (hasAnnotation(clazz, "Entity", "Table"))
            return DTOType.ENTITY;
        return DTOType.DATA;
    }

    private boolean hasAnnotation(ClassMetadata clazz, String... names) {
        for (String name : names) {
            if (clazz.getAnnotations().stream().anyMatch(a -> a.contains(name))) {
                return true;
            }
        }
        return false;
    }

    private boolean isComplexType(String type) {
        if (type == null)
            return false;
        String simple = simplifyType(type);
        // Primitive and common types are not complex
        return !Set.of("int", "long", "double", "float", "boolean", "byte", "char", "short",
                "Integer", "Long", "Double", "Float", "Boolean", "Byte", "Character",
                "Short", "String", "Object", "void", "List", "Map", "Set", "Optional")
                .contains(simple);
    }

    private String simplifyType(String type) {
        if (type == null)
            return "void";
        // Remove generics
        int genericIdx = type.indexOf('<');
        if (genericIdx > 0) {
            type = type.substring(0, genericIdx);
        }
        // Get simple name
        if (type.contains(".")) {
            type = type.substring(type.lastIndexOf('.') + 1);
        }
        return type;
    }

    // ============= Records and Enums =============

    public record IOAnalysisResult(
            List<MethodIOInfo> methodIOs,
            Map<String, Integer> inputTypeFrequency,
            Map<String, Integer> outputTypeFrequency,
            List<DTOInfo> dtos,
            List<RequestResponsePair> requestResponsePairs) {
    }

    public record MethodIOInfo(
            String className,
            String simpleClassName,
            String methodName,
            List<String> inputTypes,
            List<String> inputNames,
            String outputType,
            boolean isVoid,
            boolean hasComplexInput,
            boolean hasComplexOutput,
            IOCategory category) {
    }

    public record DTOInfo(
            String className,
            String fullyQualifiedName,
            List<String> fields,
            DTOType type) {
    }

    public record RequestResponsePair(
            String methodName,
            String requestType,
            String responseType) {
    }

    public record TypeUsageStats(
            List<Map.Entry<String, Integer>> topInputTypes,
            List<Map.Entry<String, Integer>> topOutputTypes,
            int totalMethods,
            int voidMethods,
            int complexInputMethods,
            int complexOutputMethods) {
    }

    public enum IOCategory {
        REQUEST_HANDLER, QUERY, COMMAND, PROCESSOR, VALIDATOR, TRANSFORMER, GENERAL
    }

    public enum DTOType {
        REQUEST, RESPONSE, ENTITY, DATA
    }
}

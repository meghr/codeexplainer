package com.codeexplainer.analysis;

import com.codeexplainer.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing data flow between methods and classes.
 * Tracks how data moves through the application.
 */
@Service
public class DataFlowAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DataFlowAnalyzer.class);

    /**
     * Analyzes data flow across methods.
     */
    public DataFlowResult analyzeFlow(List<ClassMetadata> classes) {
        log.info("Analyzing data flow for {} classes", classes.size());

        List<DataFlowPath> paths = new ArrayList<>();
        Map<String, Set<String>> typeProducers = new HashMap<>();
        Map<String, Set<String>> typeConsumers = new HashMap<>();

        // Build producer/consumer maps
        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getMethodName().startsWith("<"))
                    continue;

                String methodFqn = clazz.getFullyQualifiedName() + "." + method.getMethodName();

                // Method produces its return type
                if (method.getReturnType() != null && !"void".equals(method.getReturnType())) {
                    String returnType = simplifyType(method.getReturnType());
                    typeProducers.computeIfAbsent(returnType, k -> new HashSet<>()).add(methodFqn);
                }

                // Method consumes its parameter types
                for (ParameterInfo param : method.getParameters()) {
                    if (param.getType() != null) {
                        String paramType = simplifyType(param.getType());
                        typeConsumers.computeIfAbsent(paramType, k -> new HashSet<>()).add(methodFqn);
                    }
                }
            }
        }

        // Find data flow paths (producer -> consumer)
        for (String type : typeProducers.keySet()) {
            if (typeConsumers.containsKey(type)) {
                Set<String> producers = typeProducers.get(type);
                Set<String> consumers = typeConsumers.get(type);

                for (String producer : producers) {
                    for (String consumer : consumers) {
                        if (!producer.equals(consumer)) {
                            paths.add(new DataFlowPath(producer, consumer, type, FlowType.DATA_PASSING));
                        }
                    }
                }
            }
        }

        // Detect data transformation chains
        List<TransformationChain> chains = detectTransformationChains(classes);

        return new DataFlowResult(paths, typeProducers, typeConsumers, chains);
    }

    /**
     * Finds all methods that produce a given type.
     */
    public List<String> findProducersOf(List<ClassMetadata> classes, String typeName) {
        List<String> producers = new ArrayList<>();
        String simpleType = simplifyType(typeName);

        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getReturnType() != null) {
                    if (simplifyType(method.getReturnType()).equals(simpleType)) {
                        producers.add(clazz.getClassName() + "." + method.getMethodName());
                    }
                }
            }
        }

        return producers;
    }

    /**
     * Finds all methods that consume a given type.
     */
    public List<String> findConsumersOf(List<ClassMetadata> classes, String typeName) {
        List<String> consumers = new ArrayList<>();
        String simpleType = simplifyType(typeName);

        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                boolean consumes = method.getParameters().stream()
                        .anyMatch(p -> p.getType() != null &&
                                simplifyType(p.getType()).equals(simpleType));
                if (consumes) {
                    consumers.add(clazz.getClassName() + "." + method.getMethodName());
                }
            }
        }

        return consumers;
    }

    /**
     * Analyzes method call chains for data flow.
     */
    public List<CallChain> analyzeCallChains(List<ClassMetadata> classes, int maxDepth) {
        List<CallChain> chains = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getMethodName().startsWith("<"))
                    continue;
                if (!method.getAccessModifiers().contains("public"))
                    continue;

                List<String> chain = new ArrayList<>();
                chain.add(clazz.getClassName() + "." + method.getMethodName());

                // Track method invocations
                for (MethodCall inv : method.getInvocations()) {
                    if (chain.size() >= maxDepth)
                        break;
                    chain.add(inv.getOwnerClass() + "." + inv.getMethodName());
                }

                if (chain.size() > 1) {
                    chains.add(new CallChain(chain, detectChainPurpose(chain)));
                }
            }
        }

        return chains;
    }

    /**
     * Generates a data flow diagram in PlantUML format.
     */
    public String generateFlowDiagram(DataFlowResult result, int maxPaths) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam ArrowColor #2563eb\n");
        sb.append("skinparam ActivityBackgroundColor #f8fafc\n\n");

        int count = 0;
        Set<String> addedNodes = new HashSet<>();

        for (DataFlowPath path : result.paths()) {
            if (count++ >= maxPaths)
                break;

            String source = simplifyMethodName(path.source());
            String target = simplifyMethodName(path.target());

            if (!addedNodes.contains(source)) {
                sb.append("rectangle \"").append(source).append("\" as ")
                        .append(sanitize(source)).append("\n");
                addedNodes.add(source);
            }
            if (!addedNodes.contains(target)) {
                sb.append("rectangle \"").append(target).append("\" as ")
                        .append(sanitize(target)).append("\n");
                addedNodes.add(target);
            }

            sb.append(sanitize(source)).append(" --> ").append(sanitize(target))
                    .append(" : ").append(path.dataType()).append("\n");
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    /**
     * Gets flow statistics.
     */
    public FlowStatistics getStatistics(DataFlowResult result) {
        int totalPaths = result.paths().size();
        int uniqueTypes = (int) result.paths().stream()
                .map(DataFlowPath::dataType)
                .distinct()
                .count();

        Map<String, Long> byType = result.paths().stream()
                .collect(Collectors.groupingBy(DataFlowPath::dataType, Collectors.counting()));

        List<Map.Entry<String, Long>> topFlowTypes = byType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new FlowStatistics(totalPaths, uniqueTypes, topFlowTypes);
    }

    // ============= Private Methods =============

    private List<TransformationChain> detectTransformationChains(List<ClassMetadata> classes) {
        List<TransformationChain> chains = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            for (MethodMetadata method : clazz.getMethods()) {
                String name = method.getMethodName().toLowerCase();

                // Look for transformation methods
                if (name.startsWith("convert") || name.startsWith("map") ||
                        name.startsWith("transform") || name.startsWith("to")) {

                    List<String> inputTypes = method.getParameters().stream()
                            .map(p -> simplifyType(p.getType()))
                            .filter(t -> !isPrimitive(t))
                            .collect(Collectors.toList());

                    String outputType = simplifyType(method.getReturnType());

                    if (!inputTypes.isEmpty() && outputType != null && !isPrimitive(outputType)) {
                        chains.add(new TransformationChain(
                                clazz.getClassName() + "." + method.getMethodName(),
                                inputTypes,
                                outputType));
                    }
                }
            }
        }

        return chains;
    }

    private ChainPurpose detectChainPurpose(List<String> chain) {
        if (chain.isEmpty())
            return ChainPurpose.UNKNOWN;

        String first = chain.get(0).toLowerCase();
        if (first.contains("controller"))
            return ChainPurpose.REQUEST_HANDLING;
        if (first.contains("service"))
            return ChainPurpose.BUSINESS_LOGIC;
        if (first.contains("repository") || first.contains("dao"))
            return ChainPurpose.DATA_ACCESS;

        return ChainPurpose.GENERAL;
    }

    private String simplifyType(String type) {
        if (type == null)
            return "void";
        int genericIdx = type.indexOf('<');
        if (genericIdx > 0)
            type = type.substring(0, genericIdx);
        if (type.contains("."))
            type = type.substring(type.lastIndexOf('.') + 1);
        return type;
    }

    private String simplifyMethodName(String fqn) {
        if (fqn == null)
            return "";
        // Extract ClassName.methodName from fully qualified name
        String[] parts = fqn.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return fqn;
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private boolean isPrimitive(String type) {
        if (type == null)
            return false;
        return Set.of("int", "long", "double", "float", "boolean", "byte", "char", "short",
                "Integer", "Long", "Double", "Float", "Boolean", "String", "void")
                .contains(type);
    }

    // ============= Records and Enums =============

    public record DataFlowResult(
            List<DataFlowPath> paths,
            Map<String, Set<String>> typeProducers,
            Map<String, Set<String>> typeConsumers,
            List<TransformationChain> transformationChains) {
    }

    public record DataFlowPath(
            String source,
            String target,
            String dataType,
            FlowType flowType) {
    }

    public record TransformationChain(
            String methodName,
            List<String> inputTypes,
            String outputType) {
    }

    public record CallChain(
            List<String> methods,
            ChainPurpose purpose) {
    }

    public record FlowStatistics(
            int totalPaths,
            int uniqueDataTypes,
            List<Map.Entry<String, Long>> topFlowTypes) {
    }

    public enum FlowType {
        DATA_PASSING, TRANSFORMATION, AGGREGATION
    }

    public enum ChainPurpose {
        REQUEST_HANDLING, BUSINESS_LOGIC, DATA_ACCESS, GENERAL, UNKNOWN
    }
}

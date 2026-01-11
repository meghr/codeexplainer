package com.codeexplainer.extractor;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting and analyzing method-level details.
 * Provides call graph analysis, signature parsing, and method categorization.
 */
@Service
public class MethodExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MethodExtractionService.class);

    /**
     * Extracts all methods from a list of classes with enriched metadata.
     */
    public List<EnrichedMethod> extractAllMethods(List<ClassMetadata> classes) {
        List<EnrichedMethod> methods = new ArrayList<>();

        for (ClassMetadata classMetadata : classes) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                methods.add(enrichMethod(method, classMetadata));
            }
        }

        return methods;
    }

    /**
     * Enriches a method with additional computed metadata.
     */
    public EnrichedMethod enrichMethod(MethodMetadata method, ClassMetadata ownerClass) {
        return new EnrichedMethod(
                ownerClass.getFullyQualifiedName(),
                ownerClass.getClassName(),
                method.getMethodName(),
                method.getReturnType(),
                method.getParameters(),
                method.getAnnotations(),
                method.getExceptions(),
                method.getAccessModifiers(),
                method.isStatic(),
                method.isAbstract(),
                method.getInvocations() != null ? method.getInvocations().size() : 0,
                categorizeMethod(method, ownerClass),
                generateSignature(method),
                generateFullyQualifiedName(method, ownerClass));
    }

    /**
     * Builds a call graph from method invocation data.
     */
    public CallGraph buildCallGraph(List<ClassMetadata> classes) {
        Map<String, Set<String>> outgoing = new HashMap<>();
        Map<String, Set<String>> incoming = new HashMap<>();
        Set<String> allMethods = new HashSet<>();

        for (ClassMetadata classMetadata : classes) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                String callerFqn = classMetadata.getFullyQualifiedName() + "." + method.getMethodName();
                allMethods.add(callerFqn);

                if (method.getInvocations() != null) {
                    for (MethodCall call : method.getInvocations()) {
                        String calleeFqn = call.getOwnerClass() + "." + call.getMethodName();

                        // Add outgoing edge
                        outgoing.computeIfAbsent(callerFqn, k -> new HashSet<>()).add(calleeFqn);

                        // Add incoming edge
                        incoming.computeIfAbsent(calleeFqn, k -> new HashSet<>()).add(callerFqn);
                    }
                }
            }
        }

        return new CallGraph(outgoing, incoming, allMethods);
    }

    /**
     * Finds methods that match specific criteria.
     */
    public List<EnrichedMethod> findMethods(List<ClassMetadata> classes, MethodFilter filter) {
        return extractAllMethods(classes).stream()
                .filter(m -> filter.namePattern() == null ||
                        m.methodName().matches(filter.namePattern()))
                .filter(m -> filter.returnType() == null ||
                        filter.returnType().equals(m.returnType()))
                .filter(m -> filter.annotations() == null || filter.annotations().isEmpty() ||
                        m.annotations().stream().anyMatch(a -> filter.annotations().stream().anyMatch(a::contains)))
                .filter(m -> filter.categories() == null || filter.categories().isEmpty() ||
                        filter.categories().contains(m.category()))
                .filter(m -> !filter.publicOnly() || m.accessModifiers().contains("public"))
                .filter(m -> !filter.staticOnly() || m.isStatic())
                .collect(Collectors.toList());
    }

    /**
     * Gets entry point methods (main methods, REST endpoints, etc.).
     */
    public List<EnrichedMethod> findEntryPoints(List<ClassMetadata> classes) {
        return extractAllMethods(classes).stream()
                .filter(m -> isEntryPoint(m))
                .collect(Collectors.toList());
    }

    /**
     * Gets the most called methods (highest incoming edges).
     */
    public List<MethodCallCount> getMostCalledMethods(CallGraph callGraph, int limit) {
        return callGraph.incoming().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .map(e -> new MethodCallCount(e.getKey(), e.getValue().size()))
                .collect(Collectors.toList());
    }

    /**
     * Gets methods with the most outgoing calls (highest complexity indicator).
     */
    public List<MethodCallCount> getMostComplexMethods(CallGraph callGraph, int limit) {
        return callGraph.outgoing().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .map(e -> new MethodCallCount(e.getKey(), e.getValue().size()))
                .collect(Collectors.toList());
    }

    /**
     * Finds unused methods (no incoming calls from within the codebase).
     */
    public List<String> findUnusedMethods(CallGraph callGraph) {
        return callGraph.allMethods().stream()
                .filter(m -> !callGraph.incoming().containsKey(m) ||
                        callGraph.incoming().get(m).isEmpty())
                .filter(m -> !isSpecialMethod(m))
                .collect(Collectors.toList());
    }

    /**
     * Groups methods by their category.
     */
    public Map<MethodCategory, List<EnrichedMethod>> groupByCategory(List<ClassMetadata> classes) {
        return extractAllMethods(classes).stream()
                .collect(Collectors.groupingBy(EnrichedMethod::category));
    }

    /**
     * Gets overloaded methods (same name, different signatures).
     */
    public Map<String, List<EnrichedMethod>> findOverloadedMethods(List<ClassMetadata> classes) {
        Map<String, List<EnrichedMethod>> byClassAndName = extractAllMethods(classes).stream()
                .collect(Collectors.groupingBy(
                        m -> m.ownerClass() + "." + m.methodName()));

        // Keep only entries with multiple methods (overloaded)
        return byClassAndName.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Analyzes method parameter patterns.
     */
    public ParameterAnalysis analyzeParameters(List<ClassMetadata> classes) {
        List<MethodMetadata> allMethods = classes.stream()
                .flatMap(c -> c.getMethods().stream())
                .toList();

        Map<Integer, Long> paramCountDistribution = allMethods.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getParameters().size(),
                        Collectors.counting()));

        Map<String, Long> typeFrequency = allMethods.stream()
                .flatMap(m -> m.getParameters().stream())
                .collect(Collectors.groupingBy(
                        p -> simplifyTypeName(p.getType()),
                        Collectors.counting()));

        int maxParams = allMethods.stream()
                .mapToInt(m -> m.getParameters().size())
                .max().orElse(0);

        double avgParams = allMethods.stream()
                .mapToInt(m -> m.getParameters().size())
                .average().orElse(0.0);

        return new ParameterAnalysis(paramCountDistribution, typeFrequency, maxParams, avgParams);
    }

    /**
     * Analyzes return type patterns.
     */
    public Map<String, Long> analyzeReturnTypes(List<ClassMetadata> classes) {
        return classes.stream()
                .flatMap(c -> c.getMethods().stream())
                .collect(Collectors.groupingBy(
                        m -> simplifyTypeName(m.getReturnType()),
                        Collectors.counting()));
    }

    // ============= Helper Methods =============

    private MethodCategory categorizeMethod(MethodMetadata method, ClassMetadata ownerClass) {
        String name = method.getMethodName();
        List<String> annotations = method.getAnnotations();

        // Check for special methods
        if (name.equals("<init>"))
            return MethodCategory.CONSTRUCTOR;
        if (name.equals("<clinit>"))
            return MethodCategory.STATIC_INITIALIZER;
        if (name.equals("main") && method.isStatic())
            return MethodCategory.MAIN_METHOD;

        // Check annotations for categorization
        if (hasAnnotation(annotations, "GetMapping", "PostMapping", "PutMapping",
                "DeleteMapping", "RequestMapping", "PatchMapping")) {
            return MethodCategory.REST_ENDPOINT;
        }
        if (hasAnnotation(annotations, "EventListener"))
            return MethodCategory.EVENT_HANDLER;
        if (hasAnnotation(annotations, "Scheduled"))
            return MethodCategory.SCHEDULED_TASK;
        if (hasAnnotation(annotations, "Test", "ParameterizedTest"))
            return MethodCategory.TEST_METHOD;
        if (hasAnnotation(annotations, "Bean"))
            return MethodCategory.BEAN_FACTORY;
        if (hasAnnotation(annotations, "PostConstruct"))
            return MethodCategory.LIFECYCLE;
        if (hasAnnotation(annotations, "PreDestroy"))
            return MethodCategory.LIFECYCLE;

        // Check naming patterns
        if (name.startsWith("get") && !method.isStatic() &&
                method.getParameters().isEmpty() && !"void".equals(method.getReturnType())) {
            return MethodCategory.GETTER;
        }
        if (name.startsWith("set") && !method.isStatic() &&
                method.getParameters().size() == 1 && "void".equals(method.getReturnType())) {
            return MethodCategory.SETTER;
        }
        if (name.startsWith("is") && !method.isStatic() &&
                method.getParameters().isEmpty() && "boolean".equals(method.getReturnType())) {
            return MethodCategory.GETTER;
        }
        if (name.equals("equals") || name.equals("hashCode") || name.equals("toString")) {
            return MethodCategory.OBJECT_METHOD;
        }
        if (name.equals("compareTo"))
            return MethodCategory.COMPARABLE;

        return MethodCategory.REGULAR;
    }

    private boolean hasAnnotation(List<String> annotations, String... names) {
        for (String name : names) {
            for (String annotation : annotations) {
                if (annotation.contains(name))
                    return true;
            }
        }
        return false;
    }

    private String generateSignature(MethodMetadata method) {
        StringBuilder sb = new StringBuilder();

        // Access modifiers
        if (method.getAccessModifiers().contains("public"))
            sb.append("public ");
        else if (method.getAccessModifiers().contains("protected"))
            sb.append("protected ");
        else if (method.getAccessModifiers().contains("private"))
            sb.append("private ");

        if (method.isStatic())
            sb.append("static ");
        if (method.isAbstract())
            sb.append("abstract ");

        // Return type
        sb.append(simplifyTypeName(method.getReturnType())).append(" ");

        // Method name
        sb.append(method.getMethodName());

        // Parameters
        sb.append("(");
        List<ParameterInfo> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(simplifyTypeName(params.get(i).getType()));
            if (params.get(i).getName() != null && !params.get(i).getName().startsWith("arg")) {
                sb.append(" ").append(params.get(i).getName());
            }
        }
        sb.append(")");

        // Exceptions
        if (method.getExceptions() != null && !method.getExceptions().isEmpty()) {
            sb.append(" throws ");
            sb.append(method.getExceptions().stream()
                    .map(this::simplifyTypeName)
                    .collect(Collectors.joining(", ")));
        }

        return sb.toString();
    }

    private String generateFullyQualifiedName(MethodMetadata method, ClassMetadata ownerClass) {
        return ownerClass.getFullyQualifiedName() + "." + method.getMethodName() +
                "(" + method.getParameters().stream()
                        .map(p -> p.getType())
                        .collect(Collectors.joining(","))
                + ")";
    }

    private boolean isEntryPoint(EnrichedMethod method) {
        return method.category() == MethodCategory.MAIN_METHOD ||
                method.category() == MethodCategory.REST_ENDPOINT ||
                method.category() == MethodCategory.EVENT_HANDLER ||
                method.category() == MethodCategory.SCHEDULED_TASK;
    }

    private boolean isSpecialMethod(String methodName) {
        return methodName.contains("<init>") ||
                methodName.contains("<clinit>") ||
                methodName.contains("$") ||
                methodName.contains("lambda$");
    }

    private String simplifyTypeName(String typeName) {
        if (typeName == null)
            return "void";
        if (typeName.startsWith("java.lang.")) {
            return typeName.substring("java.lang.".length());
        }
        if (typeName.contains(".")) {
            return typeName.substring(typeName.lastIndexOf('.') + 1);
        }
        return typeName;
    }

    // ============= Records and Enums =============

    /**
     * Enriched method with computed metadata.
     */
    public record EnrichedMethod(
            String ownerClass,
            String className,
            String methodName,
            String returnType,
            List<ParameterInfo> parameters,
            List<String> annotations,
            List<String> exceptions,
            Set<String> accessModifiers,
            boolean isStatic,
            boolean isAbstract,
            int invocationCount,
            MethodCategory category,
            String signature,
            String fullyQualifiedName) {
    }

    /**
     * Call graph representation.
     */
    public record CallGraph(
            Map<String, Set<String>> outgoing,
            Map<String, Set<String>> incoming,
            Set<String> allMethods) {
        /**
         * Gets all methods that the given method calls.
         */
        public Set<String> getCallees(String methodFqn) {
            return outgoing.getOrDefault(methodFqn, Set.of());
        }

        /**
         * Gets all methods that call the given method.
         */
        public Set<String> getCallers(String methodFqn) {
            return incoming.getOrDefault(methodFqn, Set.of());
        }
    }

    /**
     * Method call count for ranking.
     */
    public record MethodCallCount(String methodName, int count) {
    }

    /**
     * Filter for finding methods.
     */
    public record MethodFilter(
            String namePattern,
            String returnType,
            Set<String> annotations,
            Set<MethodCategory> categories,
            boolean publicOnly,
            boolean staticOnly) {
        public static MethodFilter all() {
            return new MethodFilter(null, null, null, null, false, false);
        }

        public static MethodFilter publicMethods() {
            return new MethodFilter(null, null, null, null, true, false);
        }

        public static MethodFilter byCategory(MethodCategory... categories) {
            return new MethodFilter(null, null, null, Set.of(categories), false, false);
        }
    }

    /**
     * Parameter analysis results.
     */
    public record ParameterAnalysis(
            Map<Integer, Long> parameterCountDistribution,
            Map<String, Long> typeFrequency,
            int maxParameters,
            double averageParameters) {
    }

    /**
     * Categories for methods.
     */
    public enum MethodCategory {
        CONSTRUCTOR,
        STATIC_INITIALIZER,
        MAIN_METHOD,
        GETTER,
        SETTER,
        REST_ENDPOINT,
        EVENT_HANDLER,
        SCHEDULED_TASK,
        TEST_METHOD,
        BEAN_FACTORY,
        LIFECYCLE,
        OBJECT_METHOD,
        COMPARABLE,
        REGULAR
    }
}

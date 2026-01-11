package com.codeexplainer.diagram;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating flow diagrams showing method execution paths.
 * Creates activity diagrams and flowcharts from method analysis.
 */
@Service
public class FlowDiagramGenerator {

    /**
     * Generates an activity diagram for a method's control flow.
     */
    public String generateActivityDiagram(ClassMetadata classMetadata, String methodName) {
        MethodMetadata method = classMetadata.getMethods().stream()
                .filter(m -> m.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        if (method == null) {
            return generateEmptyDiagram("Method not found: " + methodName);
        }

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Activity: ").append(classMetadata.getClassName())
                .append(".").append(methodName).append("()\n\n");

        puml.append("start\n");

        // Generate activity nodes from method invocations
        if (method.getInvocations() != null && !method.getInvocations().isEmpty()) {
            for (MethodCall call : method.getInvocations()) {
                String activity = getSimpleClassName(call.getOwnerClass()) + "." + call.getMethodName() + "()";
                puml.append(":").append(activity).append(";\n");
            }
        } else {
            puml.append(":Execute ").append(methodName).append(";\n");
        }

        puml.append("stop\n");
        puml.append("@enduml\n");

        return puml.toString();
    }

    /**
     * Generates a request flow diagram for a REST endpoint.
     */
    public String generateRequestFlowDiagram(ClassMetadata controllerClass,
            String endpointMethod,
            List<ClassMetadata> allClasses,
            int maxDepth) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Request Flow: ").append(endpointMethod).append("\n\n");

        puml.append("actor Client\n");
        puml.append("participant \"").append(controllerClass.getClassName())
                .append("\" as Controller\n");

        MethodMetadata method = controllerClass.getMethods().stream()
                .filter(m -> m.getMethodName().equals(endpointMethod))
                .findFirst()
                .orElse(null);

        if (method == null) {
            puml.append("note over Client: Endpoint not found\n");
        } else {
            // Collect all participants
            Set<String> participants = new LinkedHashSet<>();
            collectServiceParticipants(method, allClasses, participants, 0, maxDepth);

            for (String participant : participants) {
                puml.append("participant \"").append(participant)
                        .append("\" as ").append(sanitizeName(participant)).append("\n");
            }

            puml.append("\n");
            puml.append("Client -> Controller: ").append(endpointMethod).append("()\n");
            puml.append("activate Controller\n");

            generateRequestFlow(puml, "Controller", method, allClasses, 0, maxDepth);

            puml.append("Controller --> Client: response\n");
            puml.append("deactivate Controller\n");
        }

        puml.append("@enduml\n");

        return puml.toString();
    }

    /**
     * Generates a swimlane diagram showing responsibilities by layer.
     */
    public String generateSwimlaneDiagram(List<ClassMetadata> classes) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Application Layers\n\n");

        // Group by layer based on annotations/naming
        Map<String, List<String>> layers = new LinkedHashMap<>();
        layers.put("Controllers", new ArrayList<>());
        layers.put("Services", new ArrayList<>());
        layers.put("Repositories", new ArrayList<>());
        layers.put("Entities", new ArrayList<>());
        layers.put("Other", new ArrayList<>());

        for (ClassMetadata clazz : classes) {
            String layer = classifyLayer(clazz);
            layers.get(layer).add(clazz.getClassName());
        }

        // Generate swimlanes
        puml.append("|Controllers|\n");
        for (String name : layers.get("Controllers")) {
            puml.append(":").append(name).append(";\n");
        }

        puml.append("|Services|\n");
        for (String name : layers.get("Services")) {
            puml.append(":").append(name).append(";\n");
        }

        puml.append("|Repositories|\n");
        for (String name : layers.get("Repositories")) {
            puml.append(":").append(name).append(";\n");
        }

        puml.append("|Entities|\n");
        for (String name : layers.get("Entities")) {
            puml.append(":").append(name).append(";\n");
        }

        puml.append("@enduml\n");

        return puml.toString();
    }

    /**
     * Generates a use case diagram from REST endpoints.
     */
    public String generateUseCaseDiagram(List<ClassMetadata> controllerClasses) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("left to right direction\n");
        puml.append("title Use Cases\n\n");

        puml.append("actor User\n\n");
        puml.append("rectangle Application {\n");

        for (ClassMetadata controller : controllerClasses) {
            for (MethodMetadata method : controller.getMethods()) {
                if (hasRestAnnotation(method)) {
                    String useCaseName = methodToUseCase(method.getMethodName());
                    puml.append("  usecase \"").append(useCaseName)
                            .append("\" as UC_").append(sanitizeName(method.getMethodName())).append("\n");
                }
            }
        }

        puml.append("}\n\n");

        // Link user to use cases
        for (ClassMetadata controller : controllerClasses) {
            for (MethodMetadata method : controller.getMethods()) {
                if (hasRestAnnotation(method)) {
                    puml.append("User --> UC_").append(sanitizeName(method.getMethodName())).append("\n");
                }
            }
        }

        puml.append("@enduml\n");

        return puml.toString();
    }

    /**
     * Generates a state diagram from an enum or state-based class.
     */
    public String generateStateDiagram(ClassMetadata enumOrStateClass) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title State: ").append(enumOrStateClass.getClassName()).append("\n\n");

        if (enumOrStateClass.getClassType() == ClassMetadata.ClassType.ENUM) {
            // Use enum constants as states
            puml.append("[*] --> State1\n");

            for (var field : enumOrStateClass.getFields()) {
                if (field.isStatic() && field.isFinal()) {
                    puml.append("state ").append(field.getFieldName()).append("\n");
                }
            }
        } else {
            puml.append("note: Not an enum or state class\n");
        }

        puml.append("@enduml\n");

        return puml.toString();
    }

    /**
     * Generates a deployment view diagram.
     */
    public String generateDeploymentDiagram(String applicationName, List<String> components) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("title Deployment: ").append(applicationName).append("\n\n");

        puml.append("node \"Application Server\" {\n");
        puml.append("  [").append(applicationName).append("] as app\n");
        for (String component : components) {
            puml.append("  [").append(component).append("]\n");
        }
        puml.append("}\n\n");

        puml.append("database \"Database\" as db\n");
        puml.append("app --> db\n");

        puml.append("@enduml\n");

        return puml.toString();
    }

    /**
     * Generates a basic class diagram.
     */
    public String generateClassDiagram(List<ClassMetadata> classes) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("hide empty members\n");

        // Add classes
        for (ClassMetadata clazz : classes) {
            String cleanName = getSimpleClassName(clazz.getClassName());
            String type = clazz.getClassType() != null ? clazz.getClassType().toString().toLowerCase() : "class";
            if ("enum".equals(type) || "record".equals(type) || "interface".equals(type)) {
                // native support
            } else {
                type = "class";
            }

            puml.append(type).append(" ").append(cleanName).append(" {\n");
            // Add fields (limit to public/protected or just first few to avoid bloat)
            clazz.getFields().stream().limit(10).forEach(f -> {
                puml.append("  ").append(f.getFieldName()).append(": ").append(getSimpleClassName(f.getType()))
                        .append("\n");
            });
            puml.append("}\n");
        }

        // Add relationships
        for (ClassMetadata clazz : classes) {
            String source = getSimpleClassName(clazz.getClassName());

            // Inheritance
            if (clazz.getSuperClassName() != null && !clazz.getSuperClassName().equals("java.lang.Object")) {
                String target = getSimpleClassName(clazz.getSuperClassName());
                if (classes.stream().anyMatch(c -> getSimpleClassName(c.getClassName()).equals(target))) {
                    puml.append(target).append(" <|-- ").append(source).append("\n");
                }
            }

            // Interfaces
            for (String iface : clazz.getInterfaces()) {
                String target = getSimpleClassName(iface);
                if (classes.stream().anyMatch(c -> getSimpleClassName(c.getClassName()).equals(target))) {
                    puml.append(target).append(" <|.. ").append(source).append("\n");
                }
            }
        }

        puml.append("@enduml\n");
        return puml.toString();
    }

    // ============= Helper Methods =============

    private void collectServiceParticipants(MethodMetadata method, List<ClassMetadata> allClasses,
            Set<String> participants, int depth, int maxDepth) {
        if (depth >= maxDepth || method.getInvocations() == null)
            return;

        for (MethodCall call : method.getInvocations()) {
            String targetClass = getSimpleClassName(call.getOwnerClass());
            if (!targetClass.equals("Unknown")) {
                participants.add(targetClass);

                ClassMetadata target = findClass(allClasses, call.getOwnerClass());
                if (target != null) {
                    MethodMetadata targetMethod = target.getMethods().stream()
                            .filter(m -> m.getMethodName().equals(call.getMethodName()))
                            .findFirst().orElse(null);
                    if (targetMethod != null) {
                        collectServiceParticipants(targetMethod, allClasses, participants, depth + 1, maxDepth);
                    }
                }
            }
        }
    }

    private void generateRequestFlow(StringBuilder puml, String caller, MethodMetadata method,
            List<ClassMetadata> allClasses, int depth, int maxDepth) {
        if (depth >= maxDepth || method.getInvocations() == null)
            return;

        for (MethodCall call : method.getInvocations()) {
            String targetClass = getSimpleClassName(call.getOwnerClass());
            String target = sanitizeName(targetClass);

            if (!targetClass.equals("Unknown")) {
                puml.append(caller).append(" -> ").append(target)
                        .append(": ").append(call.getMethodName()).append("()\n");

                ClassMetadata targetMetadata = findClass(allClasses, call.getOwnerClass());
                if (targetMetadata != null && depth < maxDepth - 1) {
                    MethodMetadata targetMethod = targetMetadata.getMethods().stream()
                            .filter(m -> m.getMethodName().equals(call.getMethodName()))
                            .findFirst().orElse(null);

                    if (targetMethod != null) {
                        puml.append("activate ").append(target).append("\n");
                        generateRequestFlow(puml, target, targetMethod, allClasses, depth + 1, maxDepth);
                        puml.append(target).append(" --> ").append(caller).append("\n");
                        puml.append("deactivate ").append(target).append("\n");
                    } else {
                        puml.append(target).append(" --> ").append(caller).append("\n");
                    }
                } else {
                    puml.append(target).append(" --> ").append(caller).append("\n");
                }
            }
        }
    }

    private String classifyLayer(ClassMetadata clazz) {
        if (hasAnnotation(clazz, "Controller", "RestController"))
            return "Controllers";
        if (hasAnnotation(clazz, "Service"))
            return "Services";
        if (hasAnnotation(clazz, "Repository"))
            return "Repositories";
        if (hasAnnotation(clazz, "Entity", "Table"))
            return "Entities";
        if (clazz.getClassName().endsWith("Controller"))
            return "Controllers";
        if (clazz.getClassName().endsWith("Service"))
            return "Services";
        if (clazz.getClassName().endsWith("Repository") || clazz.getClassName().endsWith("Dao"))
            return "Repositories";
        return "Other";
    }

    private boolean hasAnnotation(ClassMetadata clazz, String... names) {
        for (String name : names) {
            if (clazz.getAnnotations().stream().anyMatch(a -> a.contains(name))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRestAnnotation(MethodMetadata method) {
        return method.getAnnotations().stream().anyMatch(a -> a.contains("Mapping") || a.contains("RequestMapping") ||
                a.contains("GetMapping") || a.contains("PostMapping") ||
                a.contains("PutMapping") || a.contains("DeleteMapping"));
    }

    private String methodToUseCase(String methodName) {
        // Convert camelCase to Title Case with spaces
        StringBuilder result = new StringBuilder();
        for (char c : methodName.toCharArray()) {
            if (Character.isUpperCase(c) && result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toLowerCase(c));
        }
        String s = result.toString();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private ClassMetadata findClass(List<ClassMetadata> classes, String fqn) {
        return classes.stream()
                .filter(c -> c.getFullyQualifiedName().equals(fqn))
                .findFirst().orElse(null);
    }

    private String getSimpleClassName(String fqn) {
        if (fqn == null)
            return "Unknown";
        if (fqn.contains(".")) {
            return fqn.substring(fqn.lastIndexOf('.') + 1);
        }
        return fqn;
    }

    private String sanitizeName(String name) {
        if (name == null)
            return "Unknown";
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private String generateEmptyDiagram(String message) {
        return "@startuml\n!theme plain\nnote \"" + message + "\" as N\n@enduml\n";
    }
}

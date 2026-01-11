package com.codeexplainer.detector;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.FieldMetadata;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting Spring components, services, repositories, and other
 * beans.
 */
@Service
public class ComponentDetector {

    private static final Logger log = LoggerFactory.getLogger(ComponentDetector.class);

    // Spring annotations
    private static final Map<String, ComponentType> SPRING_ANNOTATIONS = Map.of(
            "Service", ComponentType.SERVICE,
            "Repository", ComponentType.REPOSITORY,
            "Component", ComponentType.COMPONENT,
            "Controller", ComponentType.CONTROLLER,
            "RestController", ComponentType.CONTROLLER,
            "Configuration", ComponentType.CONFIGURATION);

    // Other framework annotations
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "Entity", "Table", "Document", "MappedSuperclass");

    private static final Set<String> BEAN_ANNOTATIONS = Set.of(
            "Bean", "Primary", "Qualifier", "Lazy");

    /**
     * Detects all Spring components from class metadata.
     */
    public List<DetectedComponent> detectComponents(List<ClassMetadata> classes) {
        log.info("Detecting Spring components from {} classes", classes.size());

        List<DetectedComponent> components = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            ComponentType type = detectComponentType(clazz);
            if (type != null) {
                components.add(createComponent(clazz, type));
            }
        }

        log.info("Detected {} components", components.size());
        return components;
    }

    /**
     * Detects services specifically and creates ServiceInfo objects.
     */
    public List<ServiceInfo> detectServices(List<ClassMetadata> classes) {
        return classes.stream()
                .filter(c -> hasAnnotation(c, "Service"))
                .map(this::createServiceInfo)
                .collect(Collectors.toList());
    }

    /**
     * Detects repository classes.
     */
    public List<DetectedComponent> detectRepositories(List<ClassMetadata> classes) {
        return classes.stream()
                .filter(c -> hasAnnotation(c, "Repository") ||
                        isRepositoryInterface(c))
                .map(c -> createComponent(c, ComponentType.REPOSITORY))
                .collect(Collectors.toList());
    }

    /**
     * Detects entity/model classes.
     */
    public List<DetectedComponent> detectEntities(List<ClassMetadata> classes) {
        return classes.stream()
                .filter(c -> ENTITY_ANNOTATIONS.stream()
                        .anyMatch(a -> hasAnnotation(c, a)))
                .map(c -> createComponent(c, ComponentType.ENTITY))
                .collect(Collectors.toList());
    }

    /**
     * Detects configuration classes.
     */
    public List<DetectedComponent> detectConfigurations(List<ClassMetadata> classes) {
        return classes.stream()
                .filter(c -> hasAnnotation(c, "Configuration"))
                .map(c -> createComponent(c, ComponentType.CONFIGURATION))
                .collect(Collectors.toList());
    }

    /**
     * Detects bean factory methods in configuration classes.
     */
    public List<BeanDefinition> detectBeans(List<ClassMetadata> classes) {
        List<BeanDefinition> beans = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            if (hasAnnotation(clazz, "Configuration")) {
                for (MethodMetadata method : clazz.getMethods()) {
                    if (method.getAnnotations().stream().anyMatch(a -> a.contains("Bean"))) {
                        beans.add(new BeanDefinition(
                                method.getMethodName(),
                                method.getReturnType(),
                                clazz.getFullyQualifiedName(),
                                method.getAnnotations().stream().anyMatch(a -> a.contains("Primary")),
                                method.getAnnotations().stream().anyMatch(a -> a.contains("Lazy"))));
                    }
                }
            }
        }

        return beans;
    }

    /**
     * Groups components by type.
     */
    public Map<ComponentType, List<DetectedComponent>> groupByType(List<DetectedComponent> components) {
        return components.stream()
                .collect(Collectors.groupingBy(DetectedComponent::type));
    }

    /**
     * Groups components by package.
     */
    public Map<String, List<DetectedComponent>> groupByPackage(List<DetectedComponent> components) {
        return components.stream()
                .collect(Collectors.groupingBy(DetectedComponent::packageName));
    }

    /**
     * Detects component dependencies (injected fields).
     */
    public Map<String, List<String>> detectDependencies(List<ClassMetadata> classes) {
        Map<String, List<String>> dependencies = new HashMap<>();

        for (ClassMetadata clazz : classes) {
            List<String> deps = new ArrayList<>();

            // Check injected fields
            for (FieldMetadata field : clazz.getFields()) {
                if (isInjectedField(field)) {
                    deps.add(field.getType());
                }
            }

            // Check constructor injection (final fields are often injected)
            for (MethodMetadata method : clazz.getMethods()) {
                if (method.getMethodName().equals("<init>") &&
                        !method.getParameters().isEmpty()) {
                    for (var param : method.getParameters()) {
                        if (!isPrimitive(param.getType())) {
                            deps.add(param.getType());
                        }
                    }
                }
            }

            if (!deps.isEmpty()) {
                dependencies.put(clazz.getFullyQualifiedName(), deps);
            }
        }

        return dependencies;
    }

    /**
     * Gets component statistics.
     */
    public ComponentStatistics getStatistics(List<DetectedComponent> components) {
        Map<ComponentType, Long> byType = components.stream()
                .collect(Collectors.groupingBy(DetectedComponent::type, Collectors.counting()));

        Map<String, Long> byPackage = components.stream()
                .collect(Collectors.groupingBy(DetectedComponent::packageName, Collectors.counting()));

        int totalBeans = components.stream()
                .mapToInt(c -> c.beanMethods().size())
                .sum();

        return new ComponentStatistics(
                components.size(),
                byType,
                byPackage,
                totalBeans);
    }

    /**
     * Generates a component summary report.
     */
    public String generateComponentReport(List<DetectedComponent> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Component Summary\n\n");

        Map<ComponentType, List<DetectedComponent>> byType = groupByType(components);

        for (Map.Entry<ComponentType, List<DetectedComponent>> entry : byType.entrySet()) {
            sb.append("## ").append(entry.getKey()).append(" (")
                    .append(entry.getValue().size()).append(")\n\n");

            for (DetectedComponent comp : entry.getValue()) {
                sb.append("- **").append(comp.className()).append("**");
                if (!comp.injectedDependencies().isEmpty()) {
                    sb.append(" - injects ");
                    sb.append(comp.injectedDependencies().size()).append(" dependencies");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ============= Helper Methods =============

    private ComponentType detectComponentType(ClassMetadata clazz) {
        for (String annotation : clazz.getAnnotations()) {
            for (Map.Entry<String, ComponentType> entry : SPRING_ANNOTATIONS.entrySet()) {
                if (annotation.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        // Check for entity annotations
        for (String annotation : clazz.getAnnotations()) {
            if (ENTITY_ANNOTATIONS.stream().anyMatch(annotation::contains)) {
                return ComponentType.ENTITY;
            }
        }

        // Check by naming convention
        String className = clazz.getClassName();
        if (className.endsWith("Service") || className.endsWith("ServiceImpl")) {
            return ComponentType.SERVICE;
        }
        if (className.endsWith("Repository") || className.endsWith("Dao")) {
            return ComponentType.REPOSITORY;
        }
        if (className.endsWith("Controller")) {
            return ComponentType.CONTROLLER;
        }
        if (className.endsWith("Config") || className.endsWith("Configuration")) {
            return ComponentType.CONFIGURATION;
        }

        return null;
    }

    private DetectedComponent createComponent(ClassMetadata clazz, ComponentType type) {
        List<String> injectedDeps = new ArrayList<>();
        for (FieldMetadata field : clazz.getFields()) {
            if (isInjectedField(field)) {
                injectedDeps.add(field.getType());
            }
        }

        List<String> beanMethods = new ArrayList<>();
        for (MethodMetadata method : clazz.getMethods()) {
            if (method.getAnnotations().stream().anyMatch(a -> a.contains("Bean"))) {
                beanMethods.add(method.getMethodName());
            }
        }

        List<String> exposedMethods = new ArrayList<>();
        for (MethodMetadata method : clazz.getMethods()) {
            if (method.getAccessModifiers().contains("public") &&
                    !method.getMethodName().startsWith("<")) {
                exposedMethods.add(method.getMethodName());
            }
        }

        return new DetectedComponent(
                clazz.getFullyQualifiedName(),
                clazz.getClassName(),
                clazz.getPackageName() != null ? clazz.getPackageName() : "(default)",
                type,
                injectedDeps,
                beanMethods,
                exposedMethods,
                clazz.getAnnotations());
    }

    private ServiceInfo createServiceInfo(ClassMetadata clazz) {
        ServiceInfo info = new ServiceInfo();
        info.setServiceName(clazz.getClassName());
        info.setClassName(clazz.getFullyQualifiedName());
        info.setServiceType(ServiceInfo.ServiceType.SERVICE);

        // Extract public methods as service methods
        List<String> methods = clazz.getMethods().stream()
                .filter(m -> m.getAccessModifiers().contains("public"))
                .filter(m -> !m.getMethodName().startsWith("<"))
                .map(MethodMetadata::getMethodName)
                .collect(Collectors.toList());
        info.setPublicMethods(methods);

        // Extract dependencies
        List<String> deps = clazz.getFields().stream()
                .filter(this::isInjectedField)
                .map(FieldMetadata::getType)
                .collect(Collectors.toList());
        info.setDependencies(deps);

        return info;
    }

    private boolean hasAnnotation(ClassMetadata clazz, String annotationName) {
        return clazz.getAnnotations().stream()
                .anyMatch(a -> a.contains(annotationName));
    }

    private boolean isRepositoryInterface(ClassMetadata clazz) {
        // Check if extends Spring Data repository interfaces
        return clazz.getInterfaces().stream()
                .anyMatch(i -> i.contains("Repository") ||
                        i.contains("CrudRepository") ||
                        i.contains("JpaRepository") ||
                        i.contains("MongoRepository"));
    }

    private boolean isInjectedField(FieldMetadata field) {
        // Check for injection annotations in field name pattern
        // (actual annotation info would be in field annotations if available)
        return field.isFinal() && !field.isStatic() && !isPrimitive(field.getType());
    }

    private boolean isPrimitive(String type) {
        if (type == null)
            return false;
        return Set.of("int", "long", "double", "float", "boolean", "byte", "char", "short",
                "Integer", "Long", "Double", "Float", "Boolean", "Byte", "Character",
                "Short", "String").contains(type);
    }

    // ============= Records and Enums =============

    public record DetectedComponent(
            String fullyQualifiedName,
            String className,
            String packageName,
            ComponentType type,
            List<String> injectedDependencies,
            List<String> beanMethods,
            List<String> exposedMethods,
            List<String> annotations) {
    }

    public record BeanDefinition(
            String name,
            String type,
            String configurationClass,
            boolean isPrimary,
            boolean isLazy) {
    }

    public record ComponentStatistics(
            int totalComponents,
            Map<ComponentType, Long> byType,
            Map<String, Long> byPackage,
            int totalBeans) {
    }

    public enum ComponentType {
        SERVICE, REPOSITORY, CONTROLLER, COMPONENT, CONFIGURATION, ENTITY
    }
}

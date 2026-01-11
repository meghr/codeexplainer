package com.codeexplainer.extractor;

import com.codeexplainer.core.model.ClassMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting and analyzing type hierarchies.
 * Provides inheritance tree building, interface analysis, and type relationship
 * mapping.
 */
@Service
public class TypeHierarchyService {

    private static final Logger log = LoggerFactory.getLogger(TypeHierarchyService.class);

    /**
     * Builds the complete inheritance hierarchy for all classes.
     */
    public InheritanceHierarchy buildInheritanceHierarchy(List<ClassMetadata> classes) {
        Map<String, ClassMetadata> classMap = classes.stream()
                .collect(Collectors.toMap(
                        ClassMetadata::getFullyQualifiedName,
                        c -> c,
                        (a, b) -> a));

        Map<String, List<String>> childrenMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        Set<String> rootClasses = new HashSet<>();

        for (ClassMetadata classMetadata : classes) {
            String className = classMetadata.getFullyQualifiedName();
            String superClass = classMetadata.getSuperClassName();

            if (superClass != null && !superClass.equals("java.lang.Object")) {
                parentMap.put(className, superClass);
                childrenMap.computeIfAbsent(superClass, k -> new ArrayList<>()).add(className);
            } else {
                rootClasses.add(className);
            }
        }

        return new InheritanceHierarchy(classMap, childrenMap, parentMap, rootClasses);
    }

    /**
     * Builds interface implementation relationships.
     */
    public InterfaceHierarchy buildInterfaceHierarchy(List<ClassMetadata> classes) {
        Map<String, List<String>> implementors = new HashMap<>();
        Map<String, List<String>> implementations = new HashMap<>();
        Set<String> allInterfaces = new HashSet<>();

        for (ClassMetadata classMetadata : classes) {
            String className = classMetadata.getFullyQualifiedName();

            if (classMetadata.getClassType() == ClassMetadata.ClassType.INTERFACE) {
                allInterfaces.add(className);
            }

            for (String iface : classMetadata.getInterfaces()) {
                allInterfaces.add(iface);
                implementors.computeIfAbsent(iface, k -> new ArrayList<>()).add(className);
                implementations.computeIfAbsent(className, k -> new ArrayList<>()).add(iface);
            }
        }

        return new InterfaceHierarchy(implementors, implementations, allInterfaces);
    }

    /**
     * Gets the complete type tree for visualization.
     */
    public List<TypeNode> buildTypeTree(List<ClassMetadata> classes) {
        InheritanceHierarchy hierarchy = buildInheritanceHierarchy(classes);
        List<TypeNode> roots = new ArrayList<>();

        for (String rootClass : hierarchy.rootClasses()) {
            TypeNode node = buildTypeNode(rootClass, hierarchy, 0);
            if (node != null) {
                roots.add(node);
            }
        }

        // Sort by name
        roots.sort(Comparator.comparing(TypeNode::name));

        return roots;
    }

    private TypeNode buildTypeNode(String className, InheritanceHierarchy hierarchy, int depth) {
        ClassMetadata metadata = hierarchy.classMap().get(className);
        List<String> childNames = hierarchy.childrenMap().getOrDefault(className, List.of());

        List<TypeNode> children = childNames.stream()
                .map(child -> buildTypeNode(child, hierarchy, depth + 1))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TypeNode::name))
                .toList();

        String displayName = className.contains(".")
                ? className.substring(className.lastIndexOf('.') + 1)
                : className;

        ClassMetadata.ClassType type = metadata != null
                ? metadata.getClassType()
                : ClassMetadata.ClassType.CLASS;

        return new TypeNode(className, displayName, type, depth, children);
    }

    /**
     * Finds the inheritance path from a class to its root.
     */
    public List<String> getInheritancePath(String className, InheritanceHierarchy hierarchy) {
        List<String> path = new ArrayList<>();
        String current = className;

        while (current != null) {
            path.add(current);
            current = hierarchy.parentMap().get(current);
        }

        // Add java.lang.Object if not already there
        if (!path.isEmpty() && !path.get(path.size() - 1).equals("java.lang.Object")) {
            path.add("java.lang.Object");
        }

        return path;
    }

    /**
     * Calculates inheritance depth for each class.
     */
    public Map<String, Integer> calculateInheritanceDepths(List<ClassMetadata> classes) {
        InheritanceHierarchy hierarchy = buildInheritanceHierarchy(classes);
        Map<String, Integer> depths = new HashMap<>();

        for (ClassMetadata classMetadata : classes) {
            List<String> path = getInheritancePath(classMetadata.getFullyQualifiedName(), hierarchy);
            depths.put(classMetadata.getFullyQualifiedName(), path.size() - 1);
        }

        return depths;
    }

    /**
     * Finds classes with deep inheritance hierarchies.
     */
    public List<String> findDeepHierarchies(List<ClassMetadata> classes, int minDepth) {
        Map<String, Integer> depths = calculateInheritanceDepths(classes);

        return depths.entrySet().stream()
                .filter(e -> e.getValue() >= minDepth)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Finds classes that implement multiple interfaces.
     */
    public List<MultiInterfaceClass> findMultiInterfaceClasses(List<ClassMetadata> classes) {
        return classes.stream()
                .filter(c -> c.getInterfaces().size() > 1)
                .map(c -> new MultiInterfaceClass(
                        c.getFullyQualifiedName(),
                        c.getInterfaces()))
                .sorted((a, b) -> Integer.compare(b.interfaces().size(), a.interfaces().size()))
                .collect(Collectors.toList());
    }

    /**
     * Finds marker interfaces (interfaces with no methods).
     */
    public List<String> findMarkerInterfaces(List<ClassMetadata> classes) {
        return classes.stream()
                .filter(c -> c.getClassType() == ClassMetadata.ClassType.INTERFACE)
                .filter(c -> c.getMethods().isEmpty())
                .map(ClassMetadata::getFullyQualifiedName)
                .collect(Collectors.toList());
    }

    /**
     * Gets type statistics.
     */
    public TypeStatistics getTypeStatistics(List<ClassMetadata> classes) {
        InheritanceHierarchy inheritance = buildInheritanceHierarchy(classes);
        InterfaceHierarchy interfaces = buildInterfaceHierarchy(classes);

        int maxInheritanceDepth = calculateInheritanceDepths(classes).values().stream()
                .mapToInt(Integer::intValue).max().orElse(0);

        int maxChildren = inheritance.childrenMap().values().stream()
                .mapToInt(List::size).max().orElse(0);

        int maxImplementors = interfaces.implementors().values().stream()
                .mapToInt(List::size).max().orElse(0);

        double avgInterfaces = classes.stream()
                .mapToInt(c -> c.getInterfaces().size())
                .average().orElse(0.0);

        return new TypeStatistics(
                classes.size(),
                inheritance.rootClasses().size(),
                interfaces.allInterfaces().size(),
                maxInheritanceDepth,
                maxChildren,
                maxImplementors,
                avgInterfaces);
    }

    /**
     * Finds all subtypes (direct and indirect) of a given class.
     */
    public Set<String> findAllSubtypes(String className, InheritanceHierarchy hierarchy) {
        Set<String> subtypes = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        List<String> directChildren = hierarchy.childrenMap().get(className);
        if (directChildren != null) {
            queue.addAll(directChildren);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (subtypes.add(current)) {
                List<String> children = hierarchy.childrenMap().get(current);
                if (children != null) {
                    queue.addAll(children);
                }
            }
        }

        return subtypes;
    }

    /**
     * Finds common ancestors of two classes.
     */
    public List<String> findCommonAncestors(String class1, String class2, InheritanceHierarchy hierarchy) {
        List<String> path1 = getInheritancePath(class1, hierarchy);
        List<String> path2 = getInheritancePath(class2, hierarchy);

        Set<String> ancestors1 = new HashSet<>(path1);

        return path2.stream()
                .filter(ancestors1::contains)
                .collect(Collectors.toList());
    }

    // ============= Records =============

    /**
     * Inheritance hierarchy representation.
     */
    public record InheritanceHierarchy(
            Map<String, ClassMetadata> classMap,
            Map<String, List<String>> childrenMap,
            Map<String, String> parentMap,
            Set<String> rootClasses) {
        /**
         * Gets direct children of a class.
         */
        public List<String> getChildren(String className) {
            return childrenMap.getOrDefault(className, List.of());
        }

        /**
         * Gets the parent of a class.
         */
        public String getParent(String className) {
            return parentMap.get(className);
        }
    }

    /**
     * Interface hierarchy representation.
     */
    public record InterfaceHierarchy(
            Map<String, List<String>> implementors,
            Map<String, List<String>> implementations,
            Set<String> allInterfaces) {
        /**
         * Gets all classes implementing an interface.
         */
        public List<String> getImplementors(String interfaceName) {
            return implementors.getOrDefault(interfaceName, List.of());
        }

        /**
         * Gets all interfaces implemented by a class.
         */
        public List<String> getImplementations(String className) {
            return implementations.getOrDefault(className, List.of());
        }
    }

    /**
     * Type node for tree visualization.
     */
    public record TypeNode(
            String fullyQualifiedName,
            String name,
            ClassMetadata.ClassType type,
            int depth,
            List<TypeNode> children) {
        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }
    }

    /**
     * Class implementing multiple interfaces.
     */
    public record MultiInterfaceClass(
            String className,
            List<String> interfaces) {
    }

    /**
     * Type hierarchy statistics.
     */
    public record TypeStatistics(
            int totalTypes,
            int rootClasses,
            int interfaces,
            int maxInheritanceDepth,
            int maxChildren,
            int maxImplementors,
            double averageInterfacesPerClass) {
    }
}

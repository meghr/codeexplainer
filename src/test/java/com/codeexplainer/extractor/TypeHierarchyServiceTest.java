package com.codeexplainer.extractor;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.extractor.TypeHierarchyService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeHierarchyService.
 */
@DisplayName("TypeHierarchyService Tests")
class TypeHierarchyServiceTest {

    private TypeHierarchyService service;

    @BeforeEach
    void setUp() {
        service = new TypeHierarchyService();
    }

    @Nested
    @DisplayName("Inheritance Hierarchy")
    class InheritanceHierarchyTests {

        @Test
        @DisplayName("Should build inheritance hierarchy")
        void shouldBuildInheritanceHierarchy() {
            List<ClassMetadata> classes = createInheritanceChain();

            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            assertNotNull(hierarchy);
            assertFalse(hierarchy.rootClasses().isEmpty());
        }

        @Test
        @DisplayName("Should track parent-child relationships")
        void shouldTrackParentChildRelationships() {
            List<ClassMetadata> classes = createInheritanceChain();

            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            String childClass = "com.example.ConcreteService";
            assertEquals("com.example.AbstractService", hierarchy.getParent(childClass));
        }

        @Test
        @DisplayName("Should track children of parent class")
        void shouldTrackChildren() {
            List<ClassMetadata> classes = createInheritanceChain();

            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            List<String> children = hierarchy.getChildren("com.example.AbstractService");
            assertTrue(children.contains("com.example.ConcreteService"));
        }

        @Test
        @DisplayName("Should identify root classes")
        void shouldIdentifyRootClasses() {
            List<ClassMetadata> classes = createInheritanceChain();

            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            assertTrue(hierarchy.rootClasses().contains("com.example.BaseClass"));
        }
    }

    @Nested
    @DisplayName("Interface Hierarchy")
    class InterfaceHierarchyTests {

        @Test
        @DisplayName("Should build interface hierarchy")
        void shouldBuildInterfaceHierarchy() {
            List<ClassMetadata> classes = createClassesWithInterfaces();

            InterfaceHierarchy hierarchy = service.buildInterfaceHierarchy(classes);

            assertNotNull(hierarchy);
            assertFalse(hierarchy.allInterfaces().isEmpty());
        }

        @Test
        @DisplayName("Should track interface implementors")
        void shouldTrackImplementors() {
            List<ClassMetadata> classes = createClassesWithInterfaces();

            InterfaceHierarchy hierarchy = service.buildInterfaceHierarchy(classes);

            List<String> implementors = hierarchy.getImplementors("com.example.Repository");
            assertFalse(implementors.isEmpty());
        }

        @Test
        @DisplayName("Should track class implementations")
        void shouldTrackImplementations() {
            List<ClassMetadata> classes = createClassesWithInterfaces();

            InterfaceHierarchy hierarchy = service.buildInterfaceHierarchy(classes);

            List<String> interfaces = hierarchy.getImplementations("com.example.UserService");
            assertFalse(interfaces.isEmpty());
        }
    }

    @Nested
    @DisplayName("Type Tree")
    class TypeTreeTests {

        @Test
        @DisplayName("Should build type tree")
        void shouldBuildTypeTree() {
            List<ClassMetadata> classes = createInheritanceChain();

            List<TypeNode> tree = service.buildTypeTree(classes);

            assertNotNull(tree);
            assertFalse(tree.isEmpty());
        }

        @Test
        @DisplayName("Type nodes should have correct depth")
        void typeNodesShouldHaveCorrectDepth() {
            List<ClassMetadata> classes = createInheritanceChain();

            List<TypeNode> tree = service.buildTypeTree(classes);

            // Root should be depth 0
            assertTrue(tree.stream().allMatch(n -> n.depth() == 0));
        }

        @Test
        @DisplayName("Type nodes should have children")
        void typeNodesShouldHaveChildren() {
            List<ClassMetadata> classes = createInheritanceChain();

            List<TypeNode> tree = service.buildTypeTree(classes);

            // At least one root should have children
            assertTrue(tree.stream().anyMatch(TypeNode::hasChildren));
        }
    }

    @Nested
    @DisplayName("Inheritance Path")
    class InheritancePathTests {

        @Test
        @DisplayName("Should get inheritance path to root")
        void shouldGetInheritancePath() {
            List<ClassMetadata> classes = createInheritanceChain();
            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            List<String> path = service.getInheritancePath(
                    "com.example.ConcreteService", hierarchy);

            assertFalse(path.isEmpty());
            assertEquals("com.example.ConcreteService", path.get(0));
        }

        @Test
        @DisplayName("Should calculate inheritance depths")
        void shouldCalculateInheritanceDepths() {
            List<ClassMetadata> classes = createInheritanceChain();

            Map<String, Integer> depths = service.calculateInheritanceDepths(classes);

            assertNotNull(depths);
            assertFalse(depths.isEmpty());
        }

        @Test
        @DisplayName("Should find deep hierarchies")
        void shouldFindDeepHierarchies() {
            List<ClassMetadata> classes = createDeepHierarchy();

            List<String> deep = service.findDeepHierarchies(classes, 2);

            assertFalse(deep.isEmpty());
        }
    }

    @Nested
    @DisplayName("Class Queries")
    class ClassQueriesTests {

        @Test
        @DisplayName("Should find multi-interface classes")
        void shouldFindMultiInterfaceClasses() {
            List<ClassMetadata> classes = createMultiInterfaceClasses();

            List<MultiInterfaceClass> result = service.findMultiInterfaceClasses(classes);

            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(c -> c.interfaces().size() > 1));
        }

        @Test
        @DisplayName("Should find all subtypes")
        void shouldFindAllSubtypes() {
            List<ClassMetadata> classes = createInheritanceChain();
            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            Set<String> subtypes = service.findAllSubtypes("com.example.BaseClass", hierarchy);

            // Should include AbstractService and ConcreteService
            assertTrue(subtypes.contains("com.example.AbstractService"));
        }

        @Test
        @DisplayName("Should find common ancestors")
        void shouldFindCommonAncestors() {
            List<ClassMetadata> classes = createBranchingHierarchy();
            InheritanceHierarchy hierarchy = service.buildInheritanceHierarchy(classes);

            List<String> ancestors = service.findCommonAncestors(
                    "com.example.ServiceA", "com.example.ServiceB", hierarchy);

            assertTrue(ancestors.contains("com.example.BaseClass"));
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should get type statistics")
        void shouldGetTypeStatistics() {
            List<ClassMetadata> classes = createMixedClasses();

            TypeStatistics stats = service.getTypeStatistics(classes);

            assertNotNull(stats);
            assertTrue(stats.totalTypes() > 0);
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createInheritanceChain() {
        List<ClassMetadata> classes = new ArrayList<>();

        // BaseClass -> AbstractService -> ConcreteService
        ClassMetadata base = createClass("com.example", "BaseClass", null);
        classes.add(base);

        ClassMetadata abstractService = createClass("com.example", "AbstractService",
                "com.example.BaseClass");
        abstractService.setClassType(ClassMetadata.ClassType.ABSTRACT_CLASS);
        classes.add(abstractService);

        ClassMetadata concrete = createClass("com.example", "ConcreteService",
                "com.example.AbstractService");
        classes.add(concrete);

        return classes;
    }

    private List<ClassMetadata> createDeepHierarchy() {
        List<ClassMetadata> classes = new ArrayList<>();

        // Level 0
        ClassMetadata l0 = createClass("com.example", "Level0", null);
        classes.add(l0);

        // Level 1
        ClassMetadata l1 = createClass("com.example", "Level1", "com.example.Level0");
        classes.add(l1);

        // Level 2
        ClassMetadata l2 = createClass("com.example", "Level2", "com.example.Level1");
        classes.add(l2);

        // Level 3
        ClassMetadata l3 = createClass("com.example", "Level3", "com.example.Level2");
        classes.add(l3);

        return classes;
    }

    private List<ClassMetadata> createBranchingHierarchy() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata base = createClass("com.example", "BaseClass", null);
        classes.add(base);

        ClassMetadata serviceA = createClass("com.example", "ServiceA", "com.example.BaseClass");
        classes.add(serviceA);

        ClassMetadata serviceB = createClass("com.example", "ServiceB", "com.example.BaseClass");
        classes.add(serviceB);

        return classes;
    }

    private List<ClassMetadata> createClassesWithInterfaces() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata iface = createClass("com.example", "Repository", null);
        iface.setClassType(ClassMetadata.ClassType.INTERFACE);
        classes.add(iface);

        ClassMetadata impl = createClass("com.example", "UserService", null);
        impl.getInterfaces().add("com.example.Repository");
        classes.add(impl);

        return classes;
    }

    private List<ClassMetadata> createMultiInterfaceClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata multiImpl = createClass("com.example", "MultiService", null);
        multiImpl.getInterfaces().add("com.example.Repository");
        multiImpl.getInterfaces().add("com.example.Auditable");
        multiImpl.getInterfaces().add("java.io.Serializable");
        classes.add(multiImpl);

        return classes;
    }

    private List<ClassMetadata> createMixedClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata clazz = createClass("com.example", "MyClass", null);
        classes.add(clazz);

        ClassMetadata iface = createClass("com.example", "MyInterface", null);
        iface.setClassType(ClassMetadata.ClassType.INTERFACE);
        classes.add(iface);

        ClassMetadata abstractClass = createClass("com.example", "AbstractClass", null);
        abstractClass.setClassType(ClassMetadata.ClassType.ABSTRACT_CLASS);
        classes.add(abstractClass);

        return classes;
    }

    private ClassMetadata createClass(String packageName, String className, String superClass) {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName(packageName);
        metadata.setClassName(className);
        metadata.setFullyQualifiedName(packageName + "." + className);
        metadata.setClassType(ClassMetadata.ClassType.CLASS);
        metadata.setSuperClassName(superClass);
        return metadata;
    }
}

package com.codeexplainer.extractor;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.FieldMetadata;
import com.codeexplainer.core.model.MethodMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClassExtractionService.
 */
@DisplayName("ClassExtractionService Tests")
class ClassExtractionServiceTest {

    @Nested
    @DisplayName("Package Hierarchy")
    class PackageHierarchyTests {

        @Test
        @DisplayName("Should build package hierarchy from classes")
        void shouldBuildPackageHierarchy() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            Map<String, ClassExtractionService.PackageDetails> packages = service.buildPackageHierarchy(classes);

            assertEquals(3, packages.size());
            assertTrue(packages.containsKey("com.example.service"));
            assertTrue(packages.containsKey("com.example.model"));
            assertTrue(packages.containsKey("com.example.controller"));
        }

        @Test
        @DisplayName("Should count classes per package")
        void shouldCountClassesPerPackage() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            Map<String, ClassExtractionService.PackageDetails> packages = service.buildPackageHierarchy(classes);

            assertEquals(2, packages.get("com.example.service").classCount());
            assertEquals(2, packages.get("com.example.model").classCount());
        }

        @Test
        @DisplayName("Should handle default package")
        void shouldHandleDefaultPackage() {
            ClassExtractionService service = createService();

            ClassMetadata defaultPkgClass = createClassMetadata("", "DefaultClass");
            Map<String, ClassExtractionService.PackageDetails> packages = service
                    .buildPackageHierarchy(List.of(defaultPkgClass));

            assertTrue(packages.containsKey("(default)"));
        }
    }

    @Nested
    @DisplayName("Class Filtering")
    class ClassFilteringTests {

        @Test
        @DisplayName("Should filter by package prefix")
        void shouldFilterByPackagePrefix() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            ClassExtractionService.ClassFilter filter = new ClassExtractionService.ClassFilter("com.example.service",
                    null, false, null);

            List<ClassMetadata> filtered = service.filterClasses(classes, filter);

            assertEquals(2, filtered.size());
            assertTrue(filtered.stream().allMatch(
                    c -> c.getPackageName().startsWith("com.example.service")));
        }

        @Test
        @DisplayName("Should filter by class type")
        void shouldFilterByClassType() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            ClassExtractionService.ClassFilter filter = new ClassExtractionService.ClassFilter(null,
                    Set.of(ClassMetadata.ClassType.INTERFACE), false, null);

            List<ClassMetadata> filtered = service.filterClasses(classes, filter);

            assertTrue(filtered.stream().allMatch(
                    c -> c.getClassType() == ClassMetadata.ClassType.INTERFACE));
        }

        @Test
        @DisplayName("Should exclude inner classes")
        void shouldExcludeInnerClasses() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = new ArrayList<>(createSampleClasses());

            ClassMetadata innerClass = createClassMetadata("com.example", "Outer$Inner");
            classes.add(innerClass);

            ClassExtractionService.ClassFilter filter = ClassExtractionService.ClassFilter.excludingInner();

            List<ClassMetadata> filtered = service.filterClasses(classes, filter);

            assertFalse(filtered.stream().anyMatch(c -> c.getClassName().contains("$")));
        }
    }

    @Nested
    @DisplayName("Class Finding")
    class ClassFindingTests {

        @Test
        @DisplayName("Should find subclasses")
        void shouldFindSubclasses() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            List<ClassMetadata> subclasses = service.findSubclasses(classes, "AbstractBaseService");

            assertEquals(2, subclasses.size());
        }

        @Test
        @DisplayName("Should find implementors")
        void shouldFindImplementors() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            List<ClassMetadata> implementors = service.findImplementors(classes, "Serializable");

            assertFalse(implementors.isEmpty());
        }

        @Test
        @DisplayName("Should find by annotation")
        void shouldFindByAnnotation() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            List<ClassMetadata> annotated = service.findByAnnotation(classes, "Service");

            assertTrue(annotated.stream().allMatch(
                    c -> c.getAnnotations().stream().anyMatch(a -> a.contains("Service"))));
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate method statistics")
        void shouldCalculateMethodStatistics() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            ClassExtractionService.MethodStatistics stats = service.getMethodStatistics(classes);

            assertTrue(stats.total() > 0);
            assertTrue(stats.publicMethods() >= 0);
            assertTrue(stats.averageParameterCount() >= 0);
        }

        @Test
        @DisplayName("Should calculate field statistics")
        void shouldCalculateFieldStatistics() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            ClassExtractionService.FieldStatistics stats = service.getFieldStatistics(classes);

            assertTrue(stats.total() > 0);
            assertNotNull(stats.typeDistribution());
        }

        @Test
        @DisplayName("Should create extraction summary")
        void shouldCreateExtractionSummary() {
            ClassExtractionService service = createService();
            List<ClassMetadata> classes = createSampleClasses();

            ClassExtractionService.ExtractionResult result = new ClassExtractionService.ExtractionResult(
                    "test.jar",
                    classes,
                    service.buildPackageHierarchy(classes),
                    List.of());

            ClassExtractionService.ExtractionSummary summary = service.createSummary(result);

            assertEquals("test.jar", summary.jarName());
            assertEquals(classes.size(), summary.totalClasses());
            assertTrue(summary.interfaces() >= 0);
            assertTrue(summary.packages() > 0);
        }
    }

    // ============= Helper Methods =============

    private ClassExtractionService createService() {
        // Create with null dependencies - only testing pure methods
        return new ClassExtractionService(null, null);
    }

    private List<ClassMetadata> createSampleClasses() {
        List<ClassMetadata> classes = new ArrayList<>();

        // Service classes
        ClassMetadata userService = createClassMetadata("com.example.service", "UserService");
        userService.setSuperClassName("AbstractBaseService");
        userService.getAnnotations().add("org.springframework.stereotype.Service");
        userService.getMethods().add(createMethod("findUser", "public", false));
        userService.getMethods().add(createMethod("saveUser", "public", false));
        userService.getFields().add(createField("userRepository", "UserRepository", false, false));
        classes.add(userService);

        ClassMetadata orderService = createClassMetadata("com.example.service", "OrderService");
        orderService.setSuperClassName("AbstractBaseService");
        orderService.getAnnotations().add("org.springframework.stereotype.Service");
        orderService.getMethods().add(createMethod("createOrder", "public", false));
        classes.add(orderService);

        // Model classes
        ClassMetadata user = createClassMetadata("com.example.model", "User");
        user.getInterfaces().add("java.io.Serializable");
        user.getFields().add(createField("id", "Long", false, false));
        user.getFields().add(createField("name", "String", false, false));
        user.getMethods().add(createMethod("getId", "public", false));
        user.getMethods().add(createMethod("getName", "public", false));
        classes.add(user);

        ClassMetadata order = createClassMetadata("com.example.model", "Order");
        order.getInterfaces().add("java.io.Serializable");
        order.getFields().add(createField("id", "Long", false, false));
        classes.add(order);

        // Interface
        ClassMetadata repository = createClassMetadata("com.example.controller", "UserRepository");
        repository.setClassType(ClassMetadata.ClassType.INTERFACE);
        classes.add(repository);

        // Controller
        ClassMetadata controller = createClassMetadata("com.example.controller", "UserController");
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RestController");
        controller.getMethods().add(createMethod("getUsers", "public", false));
        classes.add(controller);

        return classes;
    }

    private ClassMetadata createClassMetadata(String packageName, String className) {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName(packageName);
        metadata.setClassName(className);
        metadata.setFullyQualifiedName(packageName.isEmpty() ? className : packageName + "." + className);
        metadata.setClassType(ClassMetadata.ClassType.CLASS);
        return metadata;
    }

    private MethodMetadata createMethod(String name, String access, boolean isStatic) {
        MethodMetadata method = new MethodMetadata();
        method.setMethodName(name);
        method.setAccessModifiers(Set.of(access));
        method.setStatic(isStatic);
        method.setReturnType("void");
        return method;
    }

    private FieldMetadata createField(String name, String type, boolean isStatic, boolean isFinal) {
        FieldMetadata field = new FieldMetadata();
        field.setFieldName(name);
        field.setType(type);
        field.setStatic(isStatic);
        field.setFinal(isFinal);
        return field;
    }
}

package com.codeexplainer.analyzer;

import com.codeexplainer.core.model.ClassMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BytecodeVisitor - the core ASM ClassVisitor
 * that extracts metadata from Java bytecode.
 */
@DisplayName("BytecodeVisitor Tests")
class BytecodeVisitorTest {

    /**
     * Test analyzing the test class itself (this class).
     */
    @Test
    @DisplayName("Should extract class metadata from this test class")
    void shouldExtractClassMetadata() throws IOException {
        ClassMetadata metadata = analyzeClass(BytecodeVisitorTest.class);

        assertNotNull(metadata);
        assertEquals("BytecodeVisitorTest", metadata.getClassName());
        assertEquals("com.codeexplainer.analyzer", metadata.getPackageName());
        assertEquals("com.codeexplainer.analyzer.BytecodeVisitorTest", metadata.getFullyQualifiedName());
    }

    @Nested
    @DisplayName("Class Type Detection")
    class ClassTypeDetection {

        @Test
        @DisplayName("Should detect regular class")
        void shouldDetectRegularClass() throws IOException {
            ClassMetadata metadata = analyzeClass(BytecodeVisitorTest.class);
            assertEquals(ClassMetadata.ClassType.CLASS, metadata.getClassType());
        }

        @Test
        @DisplayName("Should detect interface")
        void shouldDetectInterface() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleInterface.class);
            assertEquals(ClassMetadata.ClassType.INTERFACE, metadata.getClassType());
        }

        @Test
        @DisplayName("Should detect enum")
        void shouldDetectEnum() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleEnum.class);
            assertEquals(ClassMetadata.ClassType.ENUM, metadata.getClassType());
        }

        @Test
        @DisplayName("Should detect annotation")
        void shouldDetectAnnotation() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleAnnotation.class);
            assertEquals(ClassMetadata.ClassType.ANNOTATION, metadata.getClassType());
        }

        @Test
        @DisplayName("Should detect abstract class")
        void shouldDetectAbstractClass() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleAbstractClass.class);
            assertEquals(ClassMetadata.ClassType.ABSTRACT_CLASS, metadata.getClassType());
        }
    }

    @Nested
    @DisplayName("Method Extraction")
    class MethodExtraction {

        @Test
        @DisplayName("Should extract public methods")
        void shouldExtractPublicMethods() throws IOException {
            ClassMetadata metadata = analyzeClass(BytecodeVisitorTest.class);

            assertFalse(metadata.getMethods().isEmpty());
            assertTrue(metadata.getMethods().stream()
                    .anyMatch(m -> m.getMethodName().equals("shouldExtractClassMetadata")));
        }

        @Test
        @DisplayName("Should include private methods when configured")
        void shouldIncludePrivateMethodsWhenConfigured() throws IOException {
            byte[] bytecode = loadClassBytecode(SampleClassWithPrivateMethods.class);
            ClassReader reader = new ClassReader(bytecode);
            BytecodeVisitor visitor = new BytecodeVisitor(true); // Include private
            reader.accept(visitor, 0);
            ClassMetadata metadata = visitor.getClassMetadata();

            assertTrue(metadata.getMethods().stream()
                    .anyMatch(m -> m.getMethodName().equals("privateMethod")));
        }

        @Test
        @DisplayName("Should exclude private methods by default")
        void shouldExcludePrivateMethodsByDefault() throws IOException {
            byte[] bytecode = loadClassBytecode(SampleClassWithPrivateMethods.class);
            ClassReader reader = new ClassReader(bytecode);
            BytecodeVisitor visitor = new BytecodeVisitor(false); // Exclude private
            reader.accept(visitor, 0);
            ClassMetadata metadata = visitor.getClassMetadata();

            assertFalse(metadata.getMethods().stream()
                    .anyMatch(m -> m.getMethodName().equals("privateMethod")));
        }
    }

    @Nested
    @DisplayName("Annotation Extraction")
    class AnnotationExtraction {

        @Test
        @DisplayName("Should extract class annotations")
        void shouldExtractClassAnnotations() throws IOException {
            ClassMetadata metadata = analyzeClass(BytecodeVisitorTest.class);

            assertTrue(metadata.getAnnotations().stream()
                    .anyMatch(a -> a.contains("DisplayName")));
        }

        @Test
        @DisplayName("Should extract method annotations")
        void shouldExtractMethodAnnotations() throws IOException {
            ClassMetadata metadata = analyzeClass(BytecodeVisitorTest.class);

            assertTrue(metadata.getMethods().stream()
                    .filter(m -> m.getMethodName().equals("shouldExtractClassMetadata"))
                    .flatMap(m -> m.getAnnotations().stream())
                    .anyMatch(a -> a.contains("Test")));
        }
    }

    @Nested
    @DisplayName("Field Extraction")
    class FieldExtraction {

        @Test
        @DisplayName("Should extract fields with types")
        void shouldExtractFieldsWithTypes() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleClassWithFields.class);

            assertTrue(metadata.getFields().stream()
                    .anyMatch(f -> f.getFieldName().equals("stringField") &&
                            f.getType().equals("java.lang.String")));
        }

        @Test
        @DisplayName("Should detect static fields")
        void shouldDetectStaticFields() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleClassWithFields.class);

            assertTrue(metadata.getFields().stream()
                    .anyMatch(f -> f.getFieldName().equals("staticField") && f.isStatic()));
        }

        @Test
        @DisplayName("Should detect final fields")
        void shouldDetectFinalFields() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleClassWithFields.class);

            assertTrue(metadata.getFields().stream()
                    .anyMatch(f -> f.getFieldName().equals("finalField") && f.isFinal()));
        }
    }

    @Nested
    @DisplayName("Inheritance Detection")
    class InheritanceDetection {

        @Test
        @DisplayName("Should extract superclass")
        void shouldExtractSuperclass() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleChildClass.class);
            assertEquals("com.codeexplainer.analyzer.BytecodeVisitorTest$SampleParentClass",
                    metadata.getSuperClassName());
        }

        @Test
        @DisplayName("Should extract interfaces")
        void shouldExtractInterfaces() throws IOException {
            ClassMetadata metadata = analyzeClass(SampleClassWithInterfaces.class);
            assertTrue(metadata.getInterfaces().contains("java.io.Serializable"));
            assertTrue(metadata.getInterfaces().contains("java.lang.Comparable"));
        }
    }

    @Nested
    @DisplayName("Java Version Detection")
    class JavaVersionDetection {

        @Test
        @DisplayName("Should detect Java version from class file")
        void shouldDetectJavaVersion() throws IOException {
            ClassMetadata metadata = analyzeClass(BytecodeVisitorTest.class);

            assertTrue(metadata.getMajorVersion() >= 52, // Java 8+
                    "Class should be compiled for Java 8 or higher");
        }
    }

    // ============= Helper Methods =============

    private ClassMetadata analyzeClass(Class<?> clazz) throws IOException {
        byte[] bytecode = loadClassBytecode(clazz);
        ClassReader reader = new ClassReader(bytecode);
        BytecodeVisitor visitor = new BytecodeVisitor();
        reader.accept(visitor, 0);
        return visitor.getClassMetadata();
    }

    private byte[] loadClassBytecode(Class<?> clazz) throws IOException {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Could not find class resource: " + resourceName);
            return is.readAllBytes();
        }
    }

    // ============= Sample Test Classes =============

    /** Sample interface for testing interface detection. */
    interface SampleInterface {
        void doSomething();
    }

    /** Sample enum for testing enum detection. */
    enum SampleEnum {
        VALUE_ONE, VALUE_TWO, VALUE_THREE
    }

    /** Sample annotation for testing annotation detection. */
    @interface SampleAnnotation {
        String value() default "";
    }

    /** Sample abstract class for testing abstract class detection. */
    @SuppressWarnings("unused")
    static abstract class SampleAbstractClass {
        public abstract void abstractMethod();

        public void concreteMethod() {
        }
    }

    /** Sample class for testing private method detection. */
    @SuppressWarnings("unused")
    static class SampleClassWithPrivateMethods {
        public void publicMethod() {
        }

        protected void protectedMethod() {
        }

        void packageMethod() {
        }

        private void privateMethod() {
        }
    }

    /** Sample class for testing field extraction. */
    @SuppressWarnings("unused")
    static class SampleClassWithFields {
        private String stringField;
        public int intField;
        private static String staticField;
        private final String finalField = "constant";
        private volatile int volatileField;
    }

    /** Sample parent class for testing inheritance. */
    @SuppressWarnings("unused")
    static class SampleParentClass {
        public void parentMethod() {
        }
    }

    /** Sample child class for testing inheritance. */
    @SuppressWarnings("unused")
    static class SampleChildClass extends SampleParentClass {
        public void childMethod() {
        }
    }

    /** Sample class implementing interfaces for testing interface extraction. */
    @SuppressWarnings("unused")
    static class SampleClassWithInterfaces implements Serializable, Comparable<SampleClassWithInterfaces> {
        @Override
        public int compareTo(SampleClassWithInterfaces other) {
            return 0;
        }
    }
}

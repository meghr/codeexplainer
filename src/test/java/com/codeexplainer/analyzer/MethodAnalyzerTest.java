package com.codeexplainer.analyzer;

import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MethodAnalyzer - the ASM MethodVisitor
 * that analyzes method bodies to extract invocations and details.
 */
@DisplayName("MethodAnalyzer Tests")
class MethodAnalyzerTest {

    @Nested
    @DisplayName("Method Invocation Detection")
    class MethodInvocationDetection {

        @Test
        @DisplayName("Should detect method invocations")
        void shouldDetectMethodInvocations() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethods(SampleClassWithInvocations.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("methodWithInvocations"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            assertNotNull(testMethod.getInvocations());
            assertTrue(testMethod.getInvocations().size() >= 2,
                    "Should detect at least 2 invocations");

            // Check for System.out.println call
            assertTrue(testMethod.getInvocations().stream()
                    .anyMatch(call -> call.getOwnerClass().contains("PrintStream") &&
                            call.getMethodName().equals("println")),
                    "Should detect println invocation");
        }

        @Test
        @DisplayName("Should detect static method calls")
        void shouldDetectStaticMethodCalls() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethods(SampleClassWithStaticCalls.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("methodWithStaticCall"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            assertTrue(testMethod.getInvocations().stream()
                    .anyMatch(call -> call.getOwnerClass().equals("java.lang.Integer") &&
                            call.getMethodName().equals("parseInt")),
                    "Should detect Integer.parseInt static call");
        }

        @Test
        @DisplayName("Should track line numbers for invocations")
        void shouldTrackLineNumbers() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethods(SampleClassWithInvocations.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("methodWithInvocations"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            // At least some invocations should have line numbers
            boolean hasLineNumbers = testMethod.getInvocations().stream()
                    .anyMatch(call -> call.getLineNumber() > 0);
            assertTrue(hasLineNumbers, "Should have line number information");
        }
    }

    @Nested
    @DisplayName("Method Annotation Detection")
    class MethodAnnotationDetection {

        @Test
        @DisplayName("Should detect method annotations")
        void shouldDetectMethodAnnotations() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethods(SampleClassWithAnnotatedMethods.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("deprecatedMethod"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            assertTrue(testMethod.getAnnotations().stream()
                    .anyMatch(a -> a.contains("Deprecated")),
                    "Should detect @Deprecated annotation");
        }

        @Test
        @DisplayName("Should detect multiple runtime-visible annotations on a method")
        void shouldDetectMultipleAnnotations() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethods(SampleClassWithAnnotatedMethods.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("multiAnnotatedMethod"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            // Should have at least @Deprecated and @CustomRuntimeAnnotation
            assertTrue(testMethod.getAnnotations().size() >= 2,
                    "Should have at least 2 runtime annotations");
            assertTrue(testMethod.getAnnotations().stream()
                    .anyMatch(a -> a.contains("Deprecated")));
            assertTrue(testMethod.getAnnotations().stream()
                    .anyMatch(a -> a.contains("CustomRuntimeAnnotation")));
        }
    }

    @Nested
    @DisplayName("Parameter Extraction")
    class ParameterExtraction {

        @Test
        @DisplayName("Should extract parameters from method descriptor")
        void shouldExtractParameters() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethodsWithParams(SampleClassWithParameters.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("methodWithParams"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            assertEquals(2, testMethod.getParameters().size(), "Should have 2 parameters");
        }

        @Test
        @DisplayName("Should detect parameter types")
        void shouldDetectParameterTypes() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethodsWithParams(SampleClassWithParameters.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("methodWithParams"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            assertEquals("java.lang.String", testMethod.getParameters().get(0).getType());
            assertEquals("int", testMethod.getParameters().get(1).getType());
        }
    }

    @Nested
    @DisplayName("Lambda and Method Reference Detection")
    class LambdaDetection {

        @Test
        @DisplayName("Should detect lambda expressions")
        void shouldDetectLambdaExpressions() throws IOException {
            List<MethodMetadata> methods = analyzeClassMethods(SampleClassWithLambdas.class);

            MethodMetadata testMethod = methods.stream()
                    .filter(m -> m.getMethodName().equals("methodWithLambda"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Method not found"));

            // Lambda creates an INVOKEDYNAMIC instruction
            assertTrue(testMethod.getInvocations().stream()
                    .anyMatch(call -> call.getOwnerClass().contains("LambdaMetafactory")),
                    "Should detect lambda via invokedynamic");
        }
    }

    // ============= Helper Methods =============

    private List<MethodMetadata> analyzeClassMethods(Class<?> clazz) throws IOException {
        byte[] bytecode = loadClassBytecode(clazz);
        ClassReader reader = new ClassReader(bytecode);

        List<MethodMetadata> methods = new ArrayList<>();

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodMetadata method = new MethodMetadata();
                method.setMethodName(name);
                method.setDescriptor(descriptor);
                method.setInvocations(new ArrayList<>());
                methods.add(method);
                return new MethodAnalyzer(Opcodes.ASM9, method);
            }
        }, 0);

        return methods;
    }

    /**
     * Analyzes methods with parameter extraction from descriptor.
     */
    private List<MethodMetadata> analyzeClassMethodsWithParams(Class<?> clazz) throws IOException {
        byte[] bytecode = loadClassBytecode(clazz);
        ClassReader reader = new ClassReader(bytecode);

        List<MethodMetadata> methods = new ArrayList<>();

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodMetadata method = new MethodMetadata();
                method.setMethodName(name);
                method.setDescriptor(descriptor);
                method.setInvocations(new ArrayList<>());

                // Parse parameters from descriptor
                Type[] argTypes = Type.getArgumentTypes(descriptor);
                for (int i = 0; i < argTypes.length; i++) {
                    ParameterInfo param = new ParameterInfo();
                    param.setIndex(i);
                    param.setType(argTypes[i].getClassName());
                    param.setName("arg" + i);
                    method.getParameters().add(param);
                }

                methods.add(method);
                return new MethodAnalyzer(Opcodes.ASM9, method);
            }
        }, 0);

        return methods;
    }

    private byte[] loadClassBytecode(Class<?> clazz) throws IOException {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Could not find class resource: " + resourceName);
            return is.readAllBytes();
        }
    }

    // ============= Sample Test Classes =============

    /** Custom runtime annotation for testing. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface CustomRuntimeAnnotation {
        String value() default "";
    }

    @SuppressWarnings("unused")
    static class SampleClassWithInvocations {
        public void methodWithInvocations() {
            String value = "test";
            System.out.println(value);
            value.length();
        }
    }

    @SuppressWarnings("unused")
    static class SampleClassWithStaticCalls {
        public void methodWithStaticCall() {
            int number = Integer.parseInt("123");
            String.valueOf(number);
        }
    }

    @SuppressWarnings("unused")
    static class SampleClassWithAnnotatedMethods {
        @Deprecated
        public void deprecatedMethod() {
        }

        @Deprecated
        @CustomRuntimeAnnotation("test")
        public void multiAnnotatedMethod() {
        }
    }

    @SuppressWarnings("unused")
    static class SampleClassWithParameters {
        public String methodWithParams(String text, int count) {
            return text.repeat(count);
        }
    }

    @SuppressWarnings("unused")
    static class SampleClassWithLambdas {
        public void methodWithLambda() {
            Runnable r = () -> System.out.println("Hello");
            r.run();
        }
    }
}

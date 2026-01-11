package com.codeexplainer.analyzer;

import com.codeexplainer.analyzer.InstructionParser.ControlFlowEdge;
import com.codeexplainer.analyzer.InstructionParser.InstructionInfo;
import com.codeexplainer.analyzer.InstructionParser.InstructionStats;
import com.codeexplainer.analyzer.InstructionParser.InstructionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InstructionParser - the detailed bytecode instruction
 * analyzer.
 */
@DisplayName("InstructionParser Tests")
class InstructionParserTest {

    @Nested
    @DisplayName("Instruction Extraction")
    class InstructionExtraction {

        @Test
        @DisplayName("Should extract instructions from method")
        void shouldExtractInstructions() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("simpleMethod");
            assertNotNull(parser, "Parser for simpleMethod should exist");

            List<InstructionInfo> instructions = parser.getInstructions();
            assertFalse(instructions.isEmpty(), "Should have instructions");
        }

        @Test
        @DisplayName("Should categorize arithmetic instructions")
        void shouldCategorizeArithmeticInstructions() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("arithmeticMethod");
            assertNotNull(parser, "Parser for arithmeticMethod should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.arithmeticOps() > 0, "Should have arithmetic operations");
        }

        @Test
        @DisplayName("Should categorize method invocations")
        void shouldCategorizeMethodInvocations() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("methodWithCalls");
            assertNotNull(parser, "Parser for methodWithCalls should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.invocations() > 0, "Should have invocations");
        }
    }

    @Nested
    @DisplayName("Control Flow Detection")
    class ControlFlowDetection {

        @Test
        @DisplayName("Should detect jumps in conditional code")
        void shouldDetectJumps() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("conditionalMethod");
            assertNotNull(parser, "Parser for conditionalMethod should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.jumps() > 0, "Should have jump instructions");
        }

        @Test
        @DisplayName("Should build control flow edges")
        void shouldBuildControlFlowEdges() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("conditionalMethod");
            assertNotNull(parser, "Parser for conditionalMethod should exist");

            List<ControlFlowEdge> edges = parser.getControlFlow();
            assertFalse(edges.isEmpty(), "Should have control flow edges");
        }

        @Test
        @DisplayName("Should detect loops")
        void shouldDetectLoops() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("loopMethod");
            assertNotNull(parser, "Parser for loopMethod should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.jumps() >= 2, "Loop should have at least 2 jumps (condition + back edge)");
        }
    }

    @Nested
    @DisplayName("Instruction Statistics")
    class InstructionStatistics {

        @Test
        @DisplayName("Should count total instructions")
        void shouldCountTotalInstructions() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("simpleMethod");
            assertNotNull(parser, "Parser for simpleMethod should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.totalInstructions() > 0, "Should have total instruction count");
        }

        @Test
        @DisplayName("Should calculate complexity estimate")
        void shouldCalculateComplexityEstimate() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parserSimple = parsers.get("simpleMethod");
            InstructionParser parserConditional = parsers.get("conditionalMethod");

            assertNotNull(parserSimple);
            assertNotNull(parserConditional);

            int simpleComplexity = parserSimple.getStats().estimatedComplexity();
            int conditionalComplexity = parserConditional.getStats().estimatedComplexity();

            // Conditional method should have higher complexity
            assertTrue(conditionalComplexity > simpleComplexity,
                    "Conditional method should have higher complexity");
        }

        @Test
        @DisplayName("Should count field accesses")
        void shouldCountFieldAccesses() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("fieldAccessMethod");
            assertNotNull(parser, "Parser for fieldAccessMethod should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.fieldAccesses() > 0, "Should have field accesses");
        }
    }

    @Nested
    @DisplayName("Instruction Type Detection")
    class InstructionTypeDetection {

        @Test
        @DisplayName("Should detect RETURN instructions")
        void shouldDetectReturnInstructions() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("simpleMethod");
            assertNotNull(parser, "Parser should exist");

            boolean hasReturn = parser.getInstructions().stream()
                    .anyMatch(i -> i.type() == InstructionType.RETURN);
            assertTrue(hasReturn, "Should have RETURN instruction");
        }

        @Test
        @DisplayName("Should detect INVOKE instructions")
        void shouldDetectInvokeInstructions() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("methodWithCalls");
            assertNotNull(parser, "Parser should exist");

            boolean hasInvoke = parser.getInstructions().stream()
                    .anyMatch(i -> i.type() == InstructionType.INVOKE);
            assertTrue(hasInvoke, "Should have INVOKE instruction");
        }

        @Test
        @DisplayName("Should detect LOAD and STORE instructions")
        void shouldDetectLoadStoreInstructions() throws IOException {
            Map<String, InstructionParser> parsers = analyzeClassInstructions(SampleClassForInstructions.class);

            InstructionParser parser = parsers.get("arithmeticMethod");
            assertNotNull(parser, "Parser should exist");

            InstructionStats stats = parser.getStats();
            assertTrue(stats.loadStoreOps() > 0, "Should have load/store operations");
        }
    }

    // ============= Helper Methods =============

    private Map<String, InstructionParser> analyzeClassInstructions(Class<?> clazz) throws IOException {
        byte[] bytecode = loadClassBytecode(clazz);
        ClassReader reader = new ClassReader(bytecode);

        Map<String, InstructionParser> parsers = new HashMap<>();

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                InstructionParser parser = new InstructionParser(Opcodes.ASM9, name);
                parsers.put(name, parser);
                return parser;
            }
        }, 0);

        return parsers;
    }

    private byte[] loadClassBytecode(Class<?> clazz) throws IOException {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Could not find class resource: " + resourceName);
            return is.readAllBytes();
        }
    }

    // ============= Sample Test Class =============

    @SuppressWarnings("unused")
    static class SampleClassForInstructions {
        private int field = 0;

        public void simpleMethod() {
            // Just returns
        }

        public int arithmeticMethod() {
            int a = 5;
            int b = 10;
            int c = a + b;
            int d = c * 2;
            return d - a;
        }

        public void methodWithCalls() {
            String text = "Hello";
            System.out.println(text);
            text.length();
            String.valueOf(42);
        }

        public int conditionalMethod(boolean condition) {
            if (condition) {
                return 1;
            } else {
                return 0;
            }
        }

        public void loopMethod() {
            for (int i = 0; i < 10; i++) {
                System.out.println(i);
            }
        }

        public int fieldAccessMethod() {
            int value = this.field;
            this.field = value + 1;
            return this.field;
        }
    }
}

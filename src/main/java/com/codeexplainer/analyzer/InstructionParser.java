package com.codeexplainer.analyzer;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses and categorizes bytecode instructions within methods.
 * Provides detailed analysis of bytecode operations, control flow,
 * and instruction patterns for deeper method understanding.
 */
public class InstructionParser extends MethodVisitor {

    private static final Logger log = LoggerFactory.getLogger(InstructionParser.class);

    private final String methodName;
    private final List<InstructionInfo> instructions = new ArrayList<>();
    private final List<ControlFlowEdge> controlFlow = new ArrayList<>();
    private int currentLineNumber = -1;
    private int instructionIndex = 0;
    private Label currentLabel;

    // Statistics
    private int localVarCount = 0;
    private int jumpCount = 0;
    private int invokeCount = 0;
    private int fieldAccessCount = 0;
    private int arithmeticCount = 0;
    private int comparisonCount = 0;
    private int loadStoreCount = 0;
    private int stackManipulationCount = 0;

    public InstructionParser(int api, String methodName) {
        super(api);
        this.methodName = methodName;
    }

    /**
     * Gets the parsed instructions.
     */
    public List<InstructionInfo> getInstructions() {
        return instructions;
    }

    /**
     * Gets the control flow edges.
     */
    public List<ControlFlowEdge> getControlFlow() {
        return controlFlow;
    }

    /**
     * Gets instruction statistics summary.
     */
    public InstructionStats getStats() {
        return new InstructionStats(
                instructions.size(),
                localVarCount,
                jumpCount,
                invokeCount,
                fieldAccessCount,
                arithmeticCount,
                comparisonCount,
                loadStoreCount,
                stackManipulationCount);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.currentLineNumber = line;
    }

    @Override
    public void visitLabel(Label label) {
        this.currentLabel = label;
        addInstruction(InstructionType.LABEL, "L" + label.hashCode());
    }

    @Override
    public void visitInsn(int opcode) {
        InstructionType type = categorizeInsn(opcode);
        addInstruction(type, getOpcodeName(opcode));
        updateStats(type);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        addInstruction(InstructionType.PUSH, getOpcodeName(opcode) + " " + operand);
        loadStoreCount++;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        InstructionType type = isLoad(opcode) ? InstructionType.LOAD : InstructionType.STORE;
        addInstruction(type, getOpcodeName(opcode) + " var" + var);
        loadStoreCount++;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        String typeName = type.replace('/', '.');
        addInstruction(InstructionType.TYPE_OP, getOpcodeName(opcode) + " " + typeName);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        String ownerClass = owner.replace('/', '.');
        String fieldType = Type.getType(descriptor).getClassName();
        addInstruction(InstructionType.FIELD,
                getOpcodeName(opcode) + " " + ownerClass + "." + name + ":" + fieldType);
        fieldAccessCount++;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String ownerClass = owner.replace('/', '.');
        addInstruction(InstructionType.INVOKE,
                getOpcodeName(opcode) + " " + ownerClass + "." + name + descriptor);
        invokeCount++;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments) {
        addInstruction(InstructionType.INVOKE_DYNAMIC,
                "INVOKEDYNAMIC " + name + descriptor);
        invokeCount++;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        addInstruction(InstructionType.JUMP,
                getOpcodeName(opcode) + " -> L" + label.hashCode());

        // Record control flow edge
        controlFlow.add(new ControlFlowEdge(
                currentLabel != null ? "L" + currentLabel.hashCode() : "start",
                "L" + label.hashCode(),
                getOpcodeName(opcode)));
        jumpCount++;
    }

    @Override
    public void visitLdcInsn(Object value) {
        String valueStr = value instanceof String ? "\"" + value + "\"" : String.valueOf(value);
        addInstruction(InstructionType.LOAD_CONSTANT, "LDC " + valueStr);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        addInstruction(InstructionType.INCREMENT, "IINC var" + var + " " + increment);
        arithmeticCount++;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        addInstruction(InstructionType.SWITCH,
                "TABLESWITCH " + min + "-" + max + " (" + labels.length + " cases)");
        jumpCount += labels.length + 1;
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        addInstruction(InstructionType.SWITCH,
                "LOOKUPSWITCH (" + labels.length + " cases)");
        jumpCount += labels.length + 1;
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        String typeName = Type.getType(descriptor).getClassName();
        addInstruction(InstructionType.ARRAY,
                "MULTIANEWARRAY " + typeName + " " + numDimensions + "d");
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        String exceptionType = type != null ? type.replace('/', '.') : "finally";
        addInstruction(InstructionType.EXCEPTION,
                "TRY-CATCH " + exceptionType + " L" + start.hashCode() + "-L" + end.hashCode());
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature,
            Label start, Label end, int index) {
        localVarCount++;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        log.trace("Method {} requires max stack={}, max locals={}", methodName, maxStack, maxLocals);
    }

    private void addInstruction(InstructionType type, String description) {
        instructions.add(new InstructionInfo(
                instructionIndex++,
                currentLineNumber,
                type,
                description));
    }

    private InstructionType categorizeInsn(int opcode) {
        if (opcode >= Opcodes.IADD && opcode <= Opcodes.DREM) {
            return InstructionType.ARITHMETIC;
        }
        if (opcode >= Opcodes.INEG && opcode <= Opcodes.DNEG) {
            return InstructionType.ARITHMETIC;
        }
        if (opcode >= Opcodes.ISHL && opcode <= Opcodes.LXOR) {
            return InstructionType.BITWISE;
        }
        if (opcode >= Opcodes.LCMP && opcode <= Opcodes.DCMPG) {
            return InstructionType.COMPARISON;
        }
        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            return InstructionType.RETURN;
        }
        if (opcode == Opcodes.ATHROW) {
            return InstructionType.THROW;
        }
        if (opcode >= Opcodes.POP && opcode <= Opcodes.SWAP) {
            return InstructionType.STACK;
        }
        if (opcode >= Opcodes.DUP && opcode <= Opcodes.DUP2_X2) {
            return InstructionType.STACK;
        }
        if (opcode >= Opcodes.I2L && opcode <= Opcodes.I2S) {
            return InstructionType.CONVERSION;
        }
        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
            return InstructionType.PUSH;
        }
        if (opcode == Opcodes.ACONST_NULL) {
            return InstructionType.PUSH;
        }
        if (opcode == Opcodes.NOP) {
            return InstructionType.NOP;
        }
        if (opcode == Opcodes.ARRAYLENGTH) {
            return InstructionType.ARRAY;
        }
        if (opcode == Opcodes.MONITORENTER || opcode == Opcodes.MONITOREXIT) {
            return InstructionType.MONITOR;
        }
        return InstructionType.OTHER;
    }

    private void updateStats(InstructionType type) {
        switch (type) {
            case ARITHMETIC, BITWISE -> arithmeticCount++;
            case COMPARISON -> comparisonCount++;
            case STACK -> stackManipulationCount++;
            case LOAD, STORE, PUSH -> loadStoreCount++;
            default -> {
            } // No stats for other types
        }
    }

    private boolean isLoad(int opcode) {
        return opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD;
    }

    private String getOpcodeName(int opcode) {
        // Map common opcodes to readable names
        return switch (opcode) {
            // Constants
            case Opcodes.ICONST_M1 -> "ICONST_M1";
            case Opcodes.ICONST_0 -> "ICONST_0";
            case Opcodes.ICONST_1 -> "ICONST_1";
            case Opcodes.ICONST_2 -> "ICONST_2";
            case Opcodes.ICONST_3 -> "ICONST_3";
            case Opcodes.ICONST_4 -> "ICONST_4";
            case Opcodes.ICONST_5 -> "ICONST_5";
            case Opcodes.LCONST_0 -> "LCONST_0";
            case Opcodes.LCONST_1 -> "LCONST_1";
            case Opcodes.FCONST_0 -> "FCONST_0";
            case Opcodes.FCONST_1 -> "FCONST_1";
            case Opcodes.FCONST_2 -> "FCONST_2";
            case Opcodes.DCONST_0 -> "DCONST_0";
            case Opcodes.DCONST_1 -> "DCONST_1";
            case Opcodes.ACONST_NULL -> "ACONST_NULL";

            // Loads
            case Opcodes.ILOAD -> "ILOAD";
            case Opcodes.LLOAD -> "LLOAD";
            case Opcodes.FLOAD -> "FLOAD";
            case Opcodes.DLOAD -> "DLOAD";
            case Opcodes.ALOAD -> "ALOAD";

            // Stores
            case Opcodes.ISTORE -> "ISTORE";
            case Opcodes.LSTORE -> "LSTORE";
            case Opcodes.FSTORE -> "FSTORE";
            case Opcodes.DSTORE -> "DSTORE";
            case Opcodes.ASTORE -> "ASTORE";

            // Arithmetic
            case Opcodes.IADD -> "IADD";
            case Opcodes.LADD -> "LADD";
            case Opcodes.FADD -> "FADD";
            case Opcodes.DADD -> "DADD";
            case Opcodes.ISUB -> "ISUB";
            case Opcodes.LSUB -> "LSUB";
            case Opcodes.FSUB -> "FSUB";
            case Opcodes.DSUB -> "DSUB";
            case Opcodes.IMUL -> "IMUL";
            case Opcodes.LMUL -> "LMUL";
            case Opcodes.FMUL -> "FMUL";
            case Opcodes.DMUL -> "DMUL";
            case Opcodes.IDIV -> "IDIV";
            case Opcodes.LDIV -> "LDIV";
            case Opcodes.FDIV -> "FDIV";
            case Opcodes.DDIV -> "DDIV";
            case Opcodes.IREM -> "IREM";
            case Opcodes.LREM -> "LREM";

            // Comparison & Control
            case Opcodes.LCMP -> "LCMP";
            case Opcodes.IF_ICMPEQ -> "IF_ICMPEQ";
            case Opcodes.IF_ICMPNE -> "IF_ICMPNE";
            case Opcodes.IF_ICMPLT -> "IF_ICMPLT";
            case Opcodes.IF_ICMPGE -> "IF_ICMPGE";
            case Opcodes.IF_ICMPGT -> "IF_ICMPGT";
            case Opcodes.IF_ICMPLE -> "IF_ICMPLE";
            case Opcodes.IF_ACMPEQ -> "IF_ACMPEQ";
            case Opcodes.IF_ACMPNE -> "IF_ACMPNE";
            case Opcodes.IFEQ -> "IFEQ";
            case Opcodes.IFNE -> "IFNE";
            case Opcodes.IFLT -> "IFLT";
            case Opcodes.IFGE -> "IFGE";
            case Opcodes.IFGT -> "IFGT";
            case Opcodes.IFLE -> "IFLE";
            case Opcodes.IFNULL -> "IFNULL";
            case Opcodes.IFNONNULL -> "IFNONNULL";
            case Opcodes.GOTO -> "GOTO";

            // Return
            case Opcodes.IRETURN -> "IRETURN";
            case Opcodes.LRETURN -> "LRETURN";
            case Opcodes.FRETURN -> "FRETURN";
            case Opcodes.DRETURN -> "DRETURN";
            case Opcodes.ARETURN -> "ARETURN";
            case Opcodes.RETURN -> "RETURN";

            // Invocations
            case Opcodes.INVOKEVIRTUAL -> "INVOKEVIRTUAL";
            case Opcodes.INVOKESPECIAL -> "INVOKESPECIAL";
            case Opcodes.INVOKESTATIC -> "INVOKESTATIC";
            case Opcodes.INVOKEINTERFACE -> "INVOKEINTERFACE";

            // Fields
            case Opcodes.GETFIELD -> "GETFIELD";
            case Opcodes.PUTFIELD -> "PUTFIELD";
            case Opcodes.GETSTATIC -> "GETSTATIC";
            case Opcodes.PUTSTATIC -> "PUTSTATIC";

            // Type operations
            case Opcodes.NEW -> "NEW";
            case Opcodes.NEWARRAY -> "NEWARRAY";
            case Opcodes.ANEWARRAY -> "ANEWARRAY";
            case Opcodes.CHECKCAST -> "CHECKCAST";
            case Opcodes.INSTANCEOF -> "INSTANCEOF";

            // Stack manipulation
            case Opcodes.POP -> "POP";
            case Opcodes.POP2 -> "POP2";
            case Opcodes.DUP -> "DUP";
            case Opcodes.DUP_X1 -> "DUP_X1";
            case Opcodes.DUP_X2 -> "DUP_X2";
            case Opcodes.DUP2 -> "DUP2";
            case Opcodes.SWAP -> "SWAP";

            // Exception
            case Opcodes.ATHROW -> "ATHROW";

            // Monitor
            case Opcodes.MONITORENTER -> "MONITORENTER";
            case Opcodes.MONITOREXIT -> "MONITOREXIT";

            // Other
            case Opcodes.NOP -> "NOP";
            case Opcodes.BIPUSH -> "BIPUSH";
            case Opcodes.SIPUSH -> "SIPUSH";
            case Opcodes.ARRAYLENGTH -> "ARRAYLENGTH";

            default -> "OPCODE_" + opcode;
        };
    }

    // ============= Inner Classes =============

    /**
     * Represents a single bytecode instruction.
     */
    public record InstructionInfo(
            int index,
            int lineNumber,
            InstructionType type,
            String description) {
    }

    /**
     * Represents a control flow edge between two points.
     */
    public record ControlFlowEdge(
            String from,
            String to,
            String condition) {
    }

    /**
     * Statistics about instructions in a method.
     */
    public record InstructionStats(
            int totalInstructions,
            int localVariables,
            int jumps,
            int invocations,
            int fieldAccesses,
            int arithmeticOps,
            int comparisons,
            int loadStoreOps,
            int stackManipulations) {
        /**
         * Calculates cyclomatic complexity estimate based on jumps.
         */
        public int estimatedComplexity() {
            return jumps + 1;
        }
    }

    /**
     * Categories of bytecode instructions.
     */
    public enum InstructionType {
        LABEL,
        PUSH,
        LOAD,
        STORE,
        LOAD_CONSTANT,
        INCREMENT,
        ARITHMETIC,
        BITWISE,
        COMPARISON,
        CONVERSION,
        STACK,
        ARRAY,
        FIELD,
        INVOKE,
        INVOKE_DYNAMIC,
        JUMP,
        SWITCH,
        RETURN,
        THROW,
        EXCEPTION,
        TYPE_OP,
        MONITOR,
        NOP,
        OTHER
    }
}

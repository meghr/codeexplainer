package com.codeexplainer.analyzer;

import com.codeexplainer.core.model.MethodCall;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import org.objectweb.asm.*;

import java.util.ArrayList;

/**
 * ASM MethodVisitor that analyzes method bodies to extract:
 * - Method invocations (call graph data)
 * - Annotations
 * - Parameter names (from debug info)
 * - Line numbers
 */
public class MethodAnalyzer extends MethodVisitor {

    private final MethodMetadata methodMetadata;
    private int currentLineNumber = -1;

    public MethodAnalyzer(int api, MethodMetadata methodMetadata) {
        super(api);
        this.methodMetadata = methodMetadata;
        if (methodMetadata.getInvocations() == null) {
            methodMetadata.setInvocations(new ArrayList<>());
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        String annotationName = Type.getType(descriptor).getClassName();
        methodMetadata.getAnnotations().add(annotationName);
        return new AnnotationExtractor(annotationName);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        // Store parameter annotations
        String annotationName = Type.getType(descriptor).getClassName();
        if (parameter < methodMetadata.getParameters().size()) {
            ParameterInfo param = methodMetadata.getParameters().get(parameter);
            // For now, we'll just note that the parameter has annotations
        }
        return new AnnotationExtractor(annotationName);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.currentLineNumber = line;
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature,
            Label start, Label end, int index) {
        // Update parameter names from local variable table
        // Index 0 is 'this' for instance methods, so we adjust
        int paramIndex = methodMetadata.isStatic() ? index : index - 1;

        if (paramIndex >= 0 && paramIndex < methodMetadata.getParameters().size()) {
            methodMetadata.getParameters().get(paramIndex).setName(name);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String descriptor, boolean isInterface) {
        // Record method invocations
        MethodCall call = new MethodCall();
        call.setOwnerClass(owner.replace('/', '.'));
        call.setMethodName(name);
        call.setDescriptor(descriptor);
        call.setLineNumber(currentLineNumber);

        methodMetadata.getInvocations().add(call);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        // Could track field access here if needed
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // Could track type usage (new, instanceof, checkcast) here
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments) {
        // Record dynamic invocations (lambdas, method references)
        MethodCall call = new MethodCall();
        call.setOwnerClass("java.lang.invoke.LambdaMetafactory");
        call.setMethodName(name);
        call.setDescriptor(descriptor);
        call.setLineNumber(currentLineNumber);

        methodMetadata.getInvocations().add(call);
    }
}

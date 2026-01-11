package com.codeexplainer.core.model;

/**
 * Represents a method call/invocation for building call graphs.
 */
public class MethodCall {

    private String ownerClass;
    private String methodName;
    private String descriptor;
    private int lineNumber;

    public MethodCall() {
    }

    public MethodCall(String ownerClass, String methodName, String descriptor) {
        this.ownerClass = ownerClass;
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    // Getters and Setters
    public String getOwnerClass() {
        return ownerClass;
    }

    public void setOwnerClass(String ownerClass) {
        this.ownerClass = ownerClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFullyQualifiedName() {
        return ownerClass + "." + methodName;
    }
}

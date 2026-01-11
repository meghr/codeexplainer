package com.codeexplainer.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents metadata extracted from a Java method.
 */
public class MethodMetadata {

    private String methodName;
    private String returnType;
    private List<ParameterInfo> parameters = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private List<String> exceptions = new ArrayList<>();
    private Set<String> accessModifiers = new HashSet<>();
    private boolean isStatic;
    private boolean isAbstract;
    private boolean isSynchronized;
    private boolean isNative;
    private String descriptor;
    private String signature;

    // Call graph data
    private List<MethodCall> invocations = new ArrayList<>(); // Methods this method calls
    private List<MethodCall> calledBy = new ArrayList<>(); // Methods that call this

    // Getters and Setters
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public Set<String> getAccessModifiers() {
        return accessModifiers;
    }

    public void setAccessModifiers(Set<String> accessModifiers) {
        this.accessModifiers = accessModifiers;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean isSynchronized() {
        return isSynchronized;
    }

    public void setSynchronized(boolean aSynchronized) {
        isSynchronized = aSynchronized;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setNative(boolean aNative) {
        isNative = aNative;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<MethodCall> getInvocations() {
        return invocations;
    }

    public void setInvocations(List<MethodCall> invocations) {
        this.invocations = invocations;
    }

    public List<MethodCall> getCalledBy() {
        return calledBy;
    }

    public void setCalledBy(List<MethodCall> calledBy) {
        this.calledBy = calledBy;
    }

    public boolean isPublic() {
        return accessModifiers.contains("public");
    }

    public boolean isPrivate() {
        return accessModifiers.contains("private");
    }

    public boolean isProtected() {
        return accessModifiers.contains("protected");
    }

    /**
     * Gets a readable method signature
     */
    public String getReadableSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(parameters.get(i).getType());
        }
        sb.append(")");
        return sb.toString();
    }
}

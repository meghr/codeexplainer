package com.codeexplainer.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents metadata extracted from a Java class.
 */
public class ClassMetadata {

    private String className;
    private String packageName;
    private String fullyQualifiedName;
    private String superClassName;
    private List<String> interfaces = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private List<FieldMetadata> fields = new ArrayList<>();
    private List<MethodMetadata> methods = new ArrayList<>();
    private Set<String> accessModifiers = new HashSet<>();
    private ClassType classType;
    private String sourceFile;
    private int majorVersion;
    private int minorVersion;

    public enum ClassType {
        CLASS,
        INTERFACE,
        ENUM,
        ANNOTATION,
        RECORD,
        ABSTRACT_CLASS
    }

    // Getters and Setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields;
    }

    public List<MethodMetadata> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodMetadata> methods) {
        this.methods = methods;
    }

    public Set<String> getAccessModifiers() {
        return accessModifiers;
    }

    public void setAccessModifiers(Set<String> accessModifiers) {
        this.accessModifiers = accessModifiers;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public boolean isInterface() {
        return classType == ClassType.INTERFACE;
    }

    public boolean isAbstract() {
        return classType == ClassType.ABSTRACT_CLASS;
    }

    public boolean isEnum() {
        return classType == ClassType.ENUM;
    }
}

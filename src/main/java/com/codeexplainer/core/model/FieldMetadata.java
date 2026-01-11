package com.codeexplainer.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents metadata extracted from a Java field.
 */
public class FieldMetadata {

    private String fieldName;
    private String type;
    private String genericType;
    private Set<String> accessModifiers = new HashSet<>();
    private List<String> annotations;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isVolatile;
    private boolean isTransient;
    private Object defaultValue;

    // Getters and Setters
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGenericType() {
        return genericType;
    }

    public void setGenericType(String genericType) {
        this.genericType = genericType;
    }

    public Set<String> getAccessModifiers() {
        return accessModifiers;
    }

    public void setAccessModifiers(Set<String> accessModifiers) {
        this.accessModifiers = accessModifiers;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    public void setVolatile(boolean aVolatile) {
        isVolatile = aVolatile;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public void setTransient(boolean aTransient) {
        isTransient = aTransient;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
}

package com.codeexplainer.core.model;

/**
 * Represents a method parameter with its type and name.
 */
public class ParameterInfo {

    private String name;
    private String type;
    private String genericType;
    private int index;

    public ParameterInfo() {
    }

    public ParameterInfo(String name, String type, int index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}

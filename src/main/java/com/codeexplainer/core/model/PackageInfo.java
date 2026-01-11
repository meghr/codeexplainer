package com.codeexplainer.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Java package with its classes.
 */
public class PackageInfo {

    private String packageName;
    private String description;
    private List<String> classNames = new ArrayList<>();
    private int classCount;
    private int interfaceCount;
    private int enumCount;

    // Getters and Setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getInterfaceCount() {
        return interfaceCount;
    }

    public void setInterfaceCount(int interfaceCount) {
        this.interfaceCount = interfaceCount;
    }

    public int getEnumCount() {
        return enumCount;
    }

    public void setEnumCount(int enumCount) {
        this.enumCount = enumCount;
    }
}

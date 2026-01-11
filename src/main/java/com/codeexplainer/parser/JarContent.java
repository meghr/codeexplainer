package com.codeexplainer.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the extracted contents of a JAR file.
 */
public class JarContent {

    private String jarName;
    private String jarPath;
    private String extractedPath;
    private List<String> classFiles = new ArrayList<>();
    private List<String> resourceFiles = new ArrayList<>();
    private List<String> configFiles = new ArrayList<>();
    private List<String> nestedJars = new ArrayList<>();
    private byte[] manifestBytes;
    private long jarSize;
    private String jarHash;

    // Getters and Setters
    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getExtractedPath() {
        return extractedPath;
    }

    public void setExtractedPath(String extractedPath) {
        this.extractedPath = extractedPath;
    }

    public List<String> getClassFiles() {
        return classFiles;
    }

    public void setClassFiles(List<String> classFiles) {
        this.classFiles = classFiles;
    }

    public void addClassFile(String classFile) {
        this.classFiles.add(classFile);
    }

    public List<String> getResourceFiles() {
        return resourceFiles;
    }

    public void setResourceFiles(List<String> resourceFiles) {
        this.resourceFiles = resourceFiles;
    }

    public void addResourceFile(String resourceFile) {
        this.resourceFiles.add(resourceFile);
    }

    public List<String> getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(List<String> configFiles) {
        this.configFiles = configFiles;
    }

    public void addConfigFile(String configFile) {
        this.configFiles.add(configFile);
    }

    public List<String> getNestedJars() {
        return nestedJars;
    }

    public void setNestedJars(List<String> nestedJars) {
        this.nestedJars = nestedJars;
    }

    public void addNestedJar(String nestedJar) {
        this.nestedJars.add(nestedJar);
    }

    public byte[] getManifestBytes() {
        return manifestBytes;
    }

    public void setManifestBytes(byte[] manifestBytes) {
        this.manifestBytes = manifestBytes;
    }

    public long getJarSize() {
        return jarSize;
    }

    public void setJarSize(long jarSize) {
        this.jarSize = jarSize;
    }

    public String getJarHash() {
        return jarHash;
    }

    public void setJarHash(String jarHash) {
        this.jarHash = jarHash;
    }

    public int getTotalClassCount() {
        return classFiles.size();
    }

    public int getTotalResourceCount() {
        return resourceFiles.size() + configFiles.size();
    }
}

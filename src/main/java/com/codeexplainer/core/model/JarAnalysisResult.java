package com.codeexplainer.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the complete analysis result of a JAR file.
 * This is the main output model containing all extracted information.
 */
public class JarAnalysisResult {

    private String jarName;
    private String jarPath;
    private String jarHash;
    private LocalDateTime analysisTime;
    private ManifestInfo manifestInfo;
    private List<PackageInfo> packages = new ArrayList<>();
    private List<ClassMetadata> classes = new ArrayList<>();
    private List<EndpointInfo> endpoints = new ArrayList<>();
    private List<ServiceInfo> services = new ArrayList<>();
    private DependencyGraph dependencyGraph;
    private AnalysisStatus status;
    private String errorMessage;

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

    public String getJarHash() {
        return jarHash;
    }

    public void setJarHash(String jarHash) {
        this.jarHash = jarHash;
    }

    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
    }

    public ManifestInfo getManifestInfo() {
        return manifestInfo;
    }

    public void setManifestInfo(ManifestInfo manifestInfo) {
        this.manifestInfo = manifestInfo;
    }

    public List<PackageInfo> getPackages() {
        return packages;
    }

    public void setPackages(List<PackageInfo> packages) {
        this.packages = packages;
    }

    public List<ClassMetadata> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassMetadata> classes) {
        this.classes = classes;
    }

    public List<EndpointInfo> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointInfo> endpoints) {
        this.endpoints = endpoints;
    }

    public List<ServiceInfo> getServices() {
        return services;
    }

    public void setServices(List<ServiceInfo> services) {
        this.services = services;
    }

    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    public void setDependencyGraph(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Statistics
    public int getTotalClasses() {
        return classes.size();
    }

    public int getTotalPackages() {
        return packages.size();
    }

    public int getTotalEndpoints() {
        return endpoints.size();
    }

    public int getTotalServices() {
        return services.size();
    }
}

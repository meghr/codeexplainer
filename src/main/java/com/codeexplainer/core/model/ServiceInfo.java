package com.codeexplainer.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a service/component detected in the JAR.
 */
public class ServiceInfo {

    private String serviceName;
    private String className;
    private ServiceType serviceType;
    private List<String> dependencies = new ArrayList<>();
    private List<String> publicMethods = new ArrayList<>();
    private String description;

    public enum ServiceType {
        SERVICE,
        REPOSITORY,
        COMPONENT,
        CONTROLLER,
        REST_CONTROLLER,
        CONFIGURATION,
        OTHER
    }

    // Getters and Setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getPublicMethods() {
        return publicMethods;
    }

    public void setPublicMethods(List<String> publicMethods) {
        this.publicMethods = publicMethods;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

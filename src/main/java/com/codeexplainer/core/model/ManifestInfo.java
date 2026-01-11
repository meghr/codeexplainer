package com.codeexplainer.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents JAR manifest information.
 */
public class ManifestInfo {

    private String mainClass;
    private String implementationTitle;
    private String implementationVersion;
    private String implementationVendor;
    private String specificationTitle;
    private String specificationVersion;
    private String builtBy;
    private String buildJdk;
    private String createdBy;
    private Map<String, String> customAttributes = new HashMap<>();

    // Getters and Setters
    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getImplementationTitle() {
        return implementationTitle;
    }

    public void setImplementationTitle(String implementationTitle) {
        this.implementationTitle = implementationTitle;
    }

    public String getImplementationVersion() {
        return implementationVersion;
    }

    public void setImplementationVersion(String implementationVersion) {
        this.implementationVersion = implementationVersion;
    }

    public String getImplementationVendor() {
        return implementationVendor;
    }

    public void setImplementationVendor(String implementationVendor) {
        this.implementationVendor = implementationVendor;
    }

    public String getSpecificationTitle() {
        return specificationTitle;
    }

    public void setSpecificationTitle(String specificationTitle) {
        this.specificationTitle = specificationTitle;
    }

    public String getSpecificationVersion() {
        return specificationVersion;
    }

    public void setSpecificationVersion(String specificationVersion) {
        this.specificationVersion = specificationVersion;
    }

    public String getBuiltBy() {
        return builtBy;
    }

    public void setBuiltBy(String builtBy) {
        this.builtBy = builtBy;
    }

    public String getBuildJdk() {
        return buildJdk;
    }

    public void setBuildJdk(String buildJdk) {
        this.buildJdk = buildJdk;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }
}

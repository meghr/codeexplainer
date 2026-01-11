package com.codeexplainer.core.model;

/**
 * Enum representing the status of a JAR analysis.
 */
public enum AnalysisStatus {
    PENDING("Analysis is queued"),
    IN_PROGRESS("Analysis is running"),
    COMPLETED("Analysis completed successfully"),
    FAILED("Analysis failed with errors"),
    PARTIAL("Analysis completed with warnings");

    private final String description;

    AnalysisStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

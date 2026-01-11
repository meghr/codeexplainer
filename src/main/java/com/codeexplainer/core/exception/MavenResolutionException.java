package com.codeexplainer.core.exception;

/**
 * Exception thrown when Maven dependency resolution fails.
 */
public class MavenResolutionException extends CodeExplainerException {

    public MavenResolutionException(String message) {
        super(message, "MAVEN_RESOLUTION_ERROR");
    }

    public MavenResolutionException(String message, Throwable cause) {
        super(message, "MAVEN_RESOLUTION_ERROR", cause);
    }
}

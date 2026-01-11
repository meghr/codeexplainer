package com.codeexplainer.core.exception;

/**
 * Base exception for Code Explainer application.
 */
public class CodeExplainerException extends RuntimeException {

    private final String errorCode;

    public CodeExplainerException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
    }

    public CodeExplainerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CodeExplainerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
    }

    public CodeExplainerException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

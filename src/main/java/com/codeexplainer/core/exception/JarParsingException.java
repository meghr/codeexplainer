package com.codeexplainer.core.exception;

/**
 * Exception thrown when JAR parsing fails.
 */
public class JarParsingException extends CodeExplainerException {

    public JarParsingException(String message) {
        super(message, "JAR_PARSING_ERROR");
    }

    public JarParsingException(String message, Throwable cause) {
        super(message, "JAR_PARSING_ERROR", cause);
    }
}

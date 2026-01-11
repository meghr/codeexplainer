package com.codeexplainer.core.exception;

/**
 * Exception thrown when bytecode analysis fails.
 */
public class BytecodeAnalysisException extends CodeExplainerException {

    public BytecodeAnalysisException(String message) {
        super(message, "BYTECODE_ANALYSIS_ERROR");
    }

    public BytecodeAnalysisException(String message, Throwable cause) {
        super(message, "BYTECODE_ANALYSIS_ERROR", cause);
    }
}

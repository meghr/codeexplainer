package com.codeexplainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Code Explainer Application
 * 
 * A comprehensive tool to analyze Java JAR files and Maven dependencies,
 * generating documentation, flow diagrams, and actionable insights.
 */
@SpringBootApplication
@EnableAsync
public class CodeExplainerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeExplainerApplication.class, args);
    }
}

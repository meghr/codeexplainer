package com.codeexplainer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Code Explainer application.
 * Maps to the 'codeexplainer' prefix in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "codeexplainer")
public class CodeExplainerProperties {

    private String tempDir;
    private String outputDir;
    private MavenConfig maven = new MavenConfig();
    private DiagramConfig diagram = new DiagramConfig();
    private AnalysisConfig analysis = new AnalysisConfig();

    // Getters and Setters
    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public MavenConfig getMaven() {
        return maven;
    }

    public void setMaven(MavenConfig maven) {
        this.maven = maven;
    }

    public DiagramConfig getDiagram() {
        return diagram;
    }

    public void setDiagram(DiagramConfig diagram) {
        this.diagram = diagram;
    }

    public AnalysisConfig getAnalysis() {
        return analysis;
    }

    public void setAnalysis(AnalysisConfig analysis) {
        this.analysis = analysis;
    }

    /**
     * Maven repository configuration
     */
    public static class MavenConfig {
        private String localRepo;
        private List<RemoteRepository> remoteRepos = new ArrayList<>();

        public String getLocalRepo() {
            return localRepo;
        }

        public void setLocalRepo(String localRepo) {
            this.localRepo = localRepo;
        }

        public List<RemoteRepository> getRemoteRepos() {
            return remoteRepos;
        }

        public void setRemoteRepos(List<RemoteRepository> remoteRepos) {
            this.remoteRepos = remoteRepos;
        }
    }

    /**
     * Remote Maven repository definition
     */
    public static class RemoteRepository {
        private String id;
        private String url;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * Diagram generation configuration
     */
    public static class DiagramConfig {
        private String format = "svg";
        private int maxClassesPerDiagram = 50;
        private boolean includePrivateMethods = false;

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public int getMaxClassesPerDiagram() {
            return maxClassesPerDiagram;
        }

        public void setMaxClassesPerDiagram(int maxClassesPerDiagram) {
            this.maxClassesPerDiagram = maxClassesPerDiagram;
        }

        public boolean isIncludePrivateMethods() {
            return includePrivateMethods;
        }

        public void setIncludePrivateMethods(boolean includePrivateMethods) {
            this.includePrivateMethods = includePrivateMethods;
        }
    }

    /**
     * Analysis engine configuration
     */
    public static class AnalysisConfig {
        private int maxDepth = 10;
        private boolean includeDependencies = true;
        private boolean cacheEnabled = true;
        private int cacheTtlHours = 24;

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        public boolean isIncludeDependencies() {
            return includeDependencies;
        }

        public void setIncludeDependencies(boolean includeDependencies) {
            this.includeDependencies = includeDependencies;
        }

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public int getCacheTtlHours() {
            return cacheTtlHours;
        }

        public void setCacheTtlHours(int cacheTtlHours) {
            this.cacheTtlHours = cacheTtlHours;
        }
    }
}

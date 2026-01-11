package com.codeexplainer.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Service for resolving and downloading Maven artifacts.
 * Supports Maven Central and custom repositories.
 */
@Service
public class MavenArtifactResolver {

    private static final Logger log = LoggerFactory.getLogger(MavenArtifactResolver.class);

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    private final List<String> repositories;
    private final Path cacheDir;

    public MavenArtifactResolver() {
        this.repositories = new ArrayList<>();
        this.repositories.add(MAVEN_CENTRAL);
        this.cacheDir = Paths.get(System.getProperty("user.home"), ".code-explainer", "cache");
    }

    public MavenArtifactResolver(List<String> repositories, Path cacheDir) {
        this.repositories = new ArrayList<>(repositories);
        if (!this.repositories.contains(MAVEN_CENTRAL)) {
            this.repositories.add(MAVEN_CENTRAL);
        }
        this.cacheDir = cacheDir;
    }

    /**
     * Resolves and downloads a Maven artifact.
     */
    public ResolvedArtifact resolve(MavenCoordinates coordinates) throws ArtifactResolutionException {
        log.info("Resolving artifact: {}", coordinates);

        // Check cache first
        Path cachedPath = getCachePath(coordinates);
        if (Files.exists(cachedPath)) {
            log.info("Found cached artifact: {}", cachedPath);
            return new ResolvedArtifact(coordinates, cachedPath, true);
        }

        // Try each repository
        for (String repo : repositories) {
            try {
                Path downloaded = downloadArtifact(coordinates, repo);
                return new ResolvedArtifact(coordinates, downloaded, false);
            } catch (IOException e) {
                log.debug("Artifact not found in {}: {}", repo, e.getMessage());
            }
        }

        throw new ArtifactResolutionException(
                "Could not resolve artifact: " + coordinates + " from any repository");
    }

    /**
     * Resolves multiple artifacts.
     */
    public List<ResolvedArtifact> resolveAll(List<MavenCoordinates> coordinatesList) {
        List<ResolvedArtifact> resolved = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (MavenCoordinates coords : coordinatesList) {
            try {
                resolved.add(resolve(coords));
            } catch (ArtifactResolutionException e) {
                failed.add(coords.toString());
                log.warn("Failed to resolve: {}", coords);
            }
        }

        if (!failed.isEmpty()) {
            log.warn("Failed to resolve {} artifacts: {}", failed.size(), failed);
        }

        return resolved;
    }

    /**
     * Resolves an artifact with its transitive dependencies.
     */
    public List<ResolvedArtifact> resolveWithDependencies(MavenCoordinates coordinates)
            throws ArtifactResolutionException {
        log.info("Resolving {} with dependencies", coordinates);

        List<ResolvedArtifact> result = new ArrayList<>();
        Set<String> resolved = new HashSet<>();

        resolveTransitive(coordinates, result, resolved);

        return result;
    }

    /**
     * Gets the POM for an artifact.
     */
    public Optional<String> getPom(MavenCoordinates coordinates) {
        MavenCoordinates pomCoords = new MavenCoordinates(
                coordinates.groupId(),
                coordinates.artifactId(),
                coordinates.version(),
                "pom",
                null);

        for (String repo : repositories) {
            try {
                String url = buildUrl(pomCoords, repo);
                String pom = downloadAsString(url);
                return Optional.of(pom);
            } catch (IOException e) {
                log.debug("POM not found in {}", repo);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if an artifact exists in any repository.
     */
    public boolean exists(MavenCoordinates coordinates) {
        for (String repo : repositories) {
            try {
                String url = buildUrl(coordinates, repo);
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                int code = conn.getResponseCode();
                conn.disconnect();
                if (code == 200)
                    return true;
            } catch (IOException e) {
                // Continue to next repo
            }
        }
        return false;
    }

    /**
     * Adds a custom repository.
     */
    public void addRepository(String repositoryUrl) {
        if (!repositories.contains(repositoryUrl)) {
            repositories.add(0, repositoryUrl); // Add at beginning for priority
            log.info("Added repository: {}", repositoryUrl);
        }
    }

    /**
     * Clears the artifact cache.
     */
    public void clearCache() throws IOException {
        if (Files.exists(cacheDir)) {
            Files.walk(cacheDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", p);
                        }
                    });
            log.info("Cache cleared");
        }
    }

    // ============= Private Methods =============

    private void resolveTransitive(MavenCoordinates coordinates,
            List<ResolvedArtifact> result,
            Set<String> resolved) throws ArtifactResolutionException {
        String key = coordinates.groupId() + ":" + coordinates.artifactId();
        if (resolved.contains(key))
            return;
        resolved.add(key);

        // Resolve the artifact itself
        ResolvedArtifact artifact = resolve(coordinates);
        result.add(artifact);

        // Parse POM for dependencies
        Optional<String> pomContent = getPom(coordinates);
        if (pomContent.isPresent()) {
            List<MavenCoordinates> deps = parseDependencies(pomContent.get());
            for (MavenCoordinates dep : deps) {
                try {
                    resolveTransitive(dep, result, resolved);
                } catch (ArtifactResolutionException e) {
                    log.debug("Could not resolve transitive dependency: {}", dep);
                }
            }
        }
    }

    private List<MavenCoordinates> parseDependencies(String pom) {
        List<MavenCoordinates> deps = new ArrayList<>();

        // Simple XML parsing for dependencies (not using full XML parser)
        int depStart = pom.indexOf("<dependencies>");
        int depEnd = pom.indexOf("</dependencies>");

        if (depStart == -1 || depEnd == -1)
            return deps;

        String dependenciesBlock = pom.substring(depStart, depEnd);
        String[] blocks = dependenciesBlock.split("<dependency>");

        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            String groupId = extractTag(block, "groupId");
            String artifactId = extractTag(block, "artifactId");
            String version = extractTag(block, "version");
            String scope = extractTag(block, "scope");

            // Skip test and provided scope
            if ("test".equals(scope) || "provided".equals(scope))
                continue;

            if (groupId != null && artifactId != null && version != null) {
                // Skip version placeholders
                if (!version.startsWith("${")) {
                    deps.add(new MavenCoordinates(groupId, artifactId, version, "jar", null));
                }
            }
        }

        return deps;
    }

    private String extractTag(String content, String tagName) {
        String start = "<" + tagName + ">";
        String end = "</" + tagName + ">";
        int startIdx = content.indexOf(start);
        int endIdx = content.indexOf(end);
        if (startIdx != -1 && endIdx > startIdx) {
            return content.substring(startIdx + start.length(), endIdx).trim();
        }
        return null;
    }

    private Path downloadArtifact(MavenCoordinates coordinates, String repo) throws IOException {
        String url = buildUrl(coordinates, repo);
        log.info("Downloading: {}", url);

        Path targetPath = getCachePath(coordinates);
        Files.createDirectories(targetPath.getParent());

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            throw new IOException("HTTP " + conn.getResponseCode());
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        conn.disconnect();
        log.info("Downloaded to: {}", targetPath);
        return targetPath;
    }

    private String downloadAsString(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            throw new IOException("HTTP " + conn.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private String buildUrl(MavenCoordinates coords, String repo) {
        String groupPath = coords.groupId().replace('.', '/');
        String filename = coords.artifactId() + "-" + coords.version();
        if (coords.classifier() != null) {
            filename += "-" + coords.classifier();
        }
        filename += "." + coords.packaging();

        return repo + "/" + groupPath + "/" + coords.artifactId() + "/" +
                coords.version() + "/" + filename;
    }

    private Path getCachePath(MavenCoordinates coords) {
        String groupPath = coords.groupId().replace('.', File.separatorChar);
        String filename = coords.artifactId() + "-" + coords.version();
        if (coords.classifier() != null) {
            filename += "-" + coords.classifier();
        }
        filename += "." + coords.packaging();

        return cacheDir.resolve(groupPath)
                .resolve(coords.artifactId())
                .resolve(coords.version())
                .resolve(filename);
    }

    // ============= Records and Exceptions =============

    public record MavenCoordinates(
            String groupId,
            String artifactId,
            String version,
            String packaging,
            String classifier) {
        public MavenCoordinates(String groupId, String artifactId, String version) {
            this(groupId, artifactId, version, "jar", null);
        }

        public static MavenCoordinates parse(String gav) {
            String[] parts = gav.split(":");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid GAV: " + gav);
            }
            String packaging = parts.length > 3 ? parts[3] : "jar";
            String classifier = parts.length > 4 ? parts[4] : null;
            return new MavenCoordinates(parts[0], parts[1], parts[2], packaging, classifier);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId).append(":").append(artifactId).append(":").append(version);
            if (!"jar".equals(packaging)) {
                sb.append(":").append(packaging);
            }
            if (classifier != null) {
                sb.append(":").append(classifier);
            }
            return sb.toString();
        }
    }

    public record ResolvedArtifact(
            MavenCoordinates coordinates,
            Path localPath,
            boolean fromCache) {
    }

    public static class ArtifactResolutionException extends Exception {
        public ArtifactResolutionException(String message) {
            super(message);
        }

        public ArtifactResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

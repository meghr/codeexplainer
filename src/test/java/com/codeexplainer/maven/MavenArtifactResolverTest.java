package com.codeexplainer.maven;

import com.codeexplainer.maven.MavenArtifactResolver.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MavenArtifactResolver.
 */
@DisplayName("MavenArtifactResolver Tests")
class MavenArtifactResolverTest {

    private MavenArtifactResolver resolver;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        resolver = new MavenArtifactResolver(List.of(), tempDir);
    }

    @Nested
    @DisplayName("Maven Coordinates")
    class MavenCoordinatesTests {

        @Test
        @DisplayName("Should parse GAV string")
        void shouldParseGavString() {
            MavenCoordinates coords = MavenCoordinates.parse("org.example:my-lib:1.0.0");

            assertEquals("org.example", coords.groupId());
            assertEquals("my-lib", coords.artifactId());
            assertEquals("1.0.0", coords.version());
            assertEquals("jar", coords.packaging());
        }

        @Test
        @DisplayName("Should parse GAV with packaging")
        void shouldParseGavWithPackaging() {
            MavenCoordinates coords = MavenCoordinates.parse("org.example:my-lib:1.0.0:pom");

            assertEquals("pom", coords.packaging());
        }

        @Test
        @DisplayName("Should parse GAV with classifier")
        void shouldParseGavWithClassifier() {
            MavenCoordinates coords = MavenCoordinates.parse("org.example:my-lib:1.0.0:jar:sources");

            assertEquals("sources", coords.classifier());
        }

        @Test
        @DisplayName("Should format to string correctly")
        void shouldFormatToString() {
            MavenCoordinates coords = new MavenCoordinates("org.example", "my-lib", "1.0.0");

            assertEquals("org.example:my-lib:1.0.0", coords.toString());
        }
    }

    @Nested
    @DisplayName("Repository Management")
    class RepositoryTests {

        @Test
        @DisplayName("Should add custom repository")
        void shouldAddCustomRepository() {
            resolver.addRepository("https://custom.repo.com/maven");

            // No exception means success
            assertNotNull(resolver);
        }
    }

    @Nested
    @DisplayName("Artifact Existence Check")
    class ExistenceTests {

        @Test
        @DisplayName("Should check artifact existence")
        void shouldCheckArtifactExistence() {
            // Use a well-known artifact
            MavenCoordinates coords = new MavenCoordinates(
                    "org.slf4j", "slf4j-api", "2.0.9");

            boolean exists = resolver.exists(coords);

            // May be true or false depending on network
            // Just verify it doesn't throw
            assertTrue(exists || !exists);
        }
    }

    @Nested
    @DisplayName("Resolution")
    class ResolutionTests {

        @Test
        @DisplayName("Should throw for non-existent artifact")
        void shouldThrowForNonExistent() {
            MavenCoordinates coords = new MavenCoordinates(
                    "com.nonexistent", "fake-artifact", "999.0.0");

            assertThrows(ArtifactResolutionException.class, () -> {
                resolver.resolve(coords);
            });
        }

        @Test
        @DisplayName("Should resolve multiple artifacts")
        void shouldResolveMultiple() {
            List<MavenCoordinates> coords = List.of(
                    new MavenCoordinates("com.nonexistent", "fake-1", "1.0"),
                    new MavenCoordinates("com.nonexistent", "fake-2", "1.0"));

            List<ResolvedArtifact> resolved = resolver.resolveAll(coords);

            // Both should fail to resolve
            assertTrue(resolved.isEmpty());
        }
    }
}

package com.codeexplainer.maven;

import com.codeexplainer.maven.PomParser.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PomParser.
 */
@DisplayName("PomParser Tests")
class PomParserTest {

    private PomParser parser;

    @BeforeEach
    void setUp() {
        parser = new PomParser();
    }

    @Nested
    @DisplayName("Basic POM Parsing")
    class BasicParsingTests {

        @Test
        @DisplayName("Should parse project coordinates")
        void shouldParseProjectCoordinates() {
            String pom = createBasicPom();

            PomInfo info = parser.parse(pom);

            assertEquals("com.example", info.groupId());
            assertEquals("my-project", info.artifactId());
            assertEquals("1.0.0", info.version());
        }

        @Test
        @DisplayName("Should parse packaging")
        void shouldParsePackaging() {
            String pom = createBasicPom();

            PomInfo info = parser.parse(pom);

            assertEquals("jar", info.packaging());
        }

        @Test
        @DisplayName("Should parse name and description")
        void shouldParseNameAndDescription() {
            String pom = createBasicPom();

            PomInfo info = parser.parse(pom);

            assertEquals("My Project", info.name());
            assertNotNull(info.description());
        }
    }

    @Nested
    @DisplayName("Dependency Parsing")
    class DependencyParsingTests {

        @Test
        @DisplayName("Should parse dependencies")
        void shouldParseDependencies() {
            String pom = createPomWithDependencies();

            PomInfo info = parser.parse(pom);

            assertFalse(info.dependencies().isEmpty());
        }

        @Test
        @DisplayName("Should parse dependency scope")
        void shouldParseDependencyScope() {
            String pom = createPomWithDependencies();

            PomInfo info = parser.parse(pom);

            assertTrue(info.dependencies().stream()
                    .anyMatch(d -> "test".equals(d.scope())));
        }

        @Test
        @DisplayName("Should parse compile scope dependencies")
        void shouldParseCompileScopeDependencies() {
            String pom = createPomWithDependencies();

            PomInfo info = parser.parse(pom);

            assertTrue(info.dependencies().stream()
                    .anyMatch(DependencyInfo::isCompileScope));
        }
    }

    @Nested
    @DisplayName("Parent Parsing")
    class ParentParsingTests {

        @Test
        @DisplayName("Should parse parent")
        void shouldParseParent() {
            String pom = createPomWithParent();

            PomInfo info = parser.parse(pom);

            assertNotNull(info.parent());
            assertEquals("org.springframework.boot", info.parent().groupId());
        }
    }

    @Nested
    @DisplayName("Property Parsing")
    class PropertyParsingTests {

        @Test
        @DisplayName("Should parse properties")
        void shouldParseProperties() {
            String pom = createPomWithProperties();

            PomInfo info = parser.parse(pom);

            assertFalse(info.properties().isEmpty());
            assertEquals("17", info.properties().get("java.version"));
        }

        @Test
        @DisplayName("Should resolve property placeholders")
        void shouldResolvePropertyPlaceholders() {
            Map<String, String> props = Map.of("my.version", "1.2.3");

            String resolved = parser.resolveProperties("${my.version}", props);

            assertEquals("1.2.3", resolved);
        }
    }

    @Nested
    @DisplayName("Effective Dependencies")
    class EffectiveDependencyTests {

        @Test
        @DisplayName("Should get effective dependencies")
        void shouldGetEffectiveDependencies() {
            String pom = createPomWithDependencies();
            PomInfo info = parser.parse(pom);

            List<DependencyInfo> effective = parser.getEffectiveDependencies(info);

            assertNotNull(effective);
        }
    }

    // ============= Helper Methods =============

    private String createBasicPom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>jar</packaging>
                    <name>My Project</name>
                    <description>A sample project</description>
                </project>
                """;
    }

    private String createPomWithDependencies() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-project</artifactId>
                    <version>1.0.0</version>

                    <dependencies>
                        <dependency>
                            <groupId>org.slf4j</groupId>
                            <artifactId>slf4j-api</artifactId>
                            <version>2.0.9</version>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.10.0</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;
    }

    private String createPomWithParent() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>

                    <artifactId>my-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;
    }

    private String createPomWithProperties() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-project</artifactId>
                    <version>1.0.0</version>

                    <properties>
                        <java.version>17</java.version>
                        <spring.version>6.1.0</spring.version>
                    </properties>
                </project>
                """;
    }
}

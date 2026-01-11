package com.codeexplainer.detector;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.FieldMetadata;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ServiceInfo;
import com.codeexplainer.detector.ComponentDetector.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComponentDetector.
 */
@DisplayName("ComponentDetector Tests")
class ComponentDetectorTest {

    private ComponentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ComponentDetector();
    }

    @Nested
    @DisplayName("Component Detection")
    class ComponentDetectionTests {

        @Test
        @DisplayName("Should detect Spring components")
        void shouldDetectSpringComponents() {
            List<ClassMetadata> classes = createSpringComponents();

            List<DetectedComponent> components = detector.detectComponents(classes);

            assertFalse(components.isEmpty());
        }

        @Test
        @DisplayName("Should detect services")
        void shouldDetectServices() {
            List<ClassMetadata> classes = createSpringComponents();

            List<DetectedComponent> components = detector.detectComponents(classes);

            assertTrue(components.stream().anyMatch(c -> c.type() == ComponentType.SERVICE));
        }

        @Test
        @DisplayName("Should detect controllers")
        void shouldDetectControllers() {
            List<ClassMetadata> classes = createSpringComponents();

            List<DetectedComponent> components = detector.detectComponents(classes);

            assertTrue(components.stream().anyMatch(c -> c.type() == ComponentType.CONTROLLER));
        }

        @Test
        @DisplayName("Should detect repositories")
        void shouldDetectRepositories() {
            List<ClassMetadata> classes = createSpringComponents();

            List<DetectedComponent> components = detector.detectComponents(classes);

            assertTrue(components.stream().anyMatch(c -> c.type() == ComponentType.REPOSITORY));
        }
    }

    @Nested
    @DisplayName("Service Detection")
    class ServiceDetectionTests {

        @Test
        @DisplayName("Should create ServiceInfo objects")
        void shouldCreateServiceInfo() {
            List<ClassMetadata> classes = createSpringComponents();

            List<ServiceInfo> services = detector.detectServices(classes);

            assertFalse(services.isEmpty());
            assertNotNull(services.get(0).getServiceName());
        }
    }

    @Nested
    @DisplayName("Entity Detection")
    class EntityDetectionTests {

        @Test
        @DisplayName("Should detect entity classes")
        void shouldDetectEntities() {
            List<ClassMetadata> classes = createEntities();

            List<DetectedComponent> entities = detector.detectEntities(classes);

            assertFalse(entities.isEmpty());
            assertTrue(entities.stream().allMatch(e -> e.type() == ComponentType.ENTITY));
        }
    }

    @Nested
    @DisplayName("Configuration Detection")
    class ConfigurationTests {

        @Test
        @DisplayName("Should detect configuration classes")
        void shouldDetectConfigurations() {
            List<ClassMetadata> classes = createConfigurations();

            List<DetectedComponent> configs = detector.detectConfigurations(classes);

            assertFalse(configs.isEmpty());
        }

        @Test
        @DisplayName("Should detect beans in configurations")
        void shouldDetectBeans() {
            List<ClassMetadata> classes = createConfigurations();

            List<BeanDefinition> beans = detector.detectBeans(classes);

            assertFalse(beans.isEmpty());
        }
    }

    @Nested
    @DisplayName("Grouping and Statistics")
    class GroupingTests {

        @Test
        @DisplayName("Should group by type")
        void shouldGroupByType() {
            List<ClassMetadata> classes = createSpringComponents();
            List<DetectedComponent> components = detector.detectComponents(classes);

            Map<ComponentType, List<DetectedComponent>> grouped = detector.groupByType(components);

            assertNotNull(grouped);
            assertFalse(grouped.isEmpty());
        }

        @Test
        @DisplayName("Should calculate statistics")
        void shouldCalculateStatistics() {
            List<ClassMetadata> classes = createSpringComponents();
            List<DetectedComponent> components = detector.detectComponents(classes);

            ComponentStatistics stats = detector.getStatistics(components);

            assertNotNull(stats);
            assertEquals(components.size(), stats.totalComponents());
        }
    }

    @Nested
    @DisplayName("Dependency Detection")
    class DependencyTests {

        @Test
        @DisplayName("Should detect component dependencies")
        void shouldDetectDependencies() {
            List<ClassMetadata> classes = createComponentsWithDependencies();

            Map<String, List<String>> deps = detector.detectDependencies(classes);

            assertNotNull(deps);
        }
    }

    // ============= Helper Methods =============

    private List<ClassMetadata> createSpringComponents() {
        List<ClassMetadata> classes = new ArrayList<>();

        // Service
        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("UserService");
        service.setFullyQualifiedName("com.example.service.UserService");
        service.setClassType(ClassMetadata.ClassType.CLASS);
        service.getAnnotations().add("org.springframework.stereotype.Service");

        MethodMetadata getUser = new MethodMetadata();
        getUser.setMethodName("getUser");
        getUser.setAccessModifiers(Set.of("public"));
        service.getMethods().add(getUser);
        classes.add(service);

        // Controller
        ClassMetadata controller = new ClassMetadata();
        controller.setPackageName("com.example.controller");
        controller.setClassName("UserController");
        controller.setFullyQualifiedName("com.example.controller.UserController");
        controller.setClassType(ClassMetadata.ClassType.CLASS);
        controller.getAnnotations().add("org.springframework.web.bind.annotation.RestController");
        classes.add(controller);

        // Repository
        ClassMetadata repo = new ClassMetadata();
        repo.setPackageName("com.example.repository");
        repo.setClassName("UserRepository");
        repo.setFullyQualifiedName("com.example.repository.UserRepository");
        repo.setClassType(ClassMetadata.ClassType.INTERFACE);
        repo.getAnnotations().add("org.springframework.stereotype.Repository");
        classes.add(repo);

        return classes;
    }

    private List<ClassMetadata> createEntities() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata entity = new ClassMetadata();
        entity.setPackageName("com.example.model");
        entity.setClassName("User");
        entity.setFullyQualifiedName("com.example.model.User");
        entity.setClassType(ClassMetadata.ClassType.CLASS);
        entity.getAnnotations().add("javax.persistence.Entity");
        entity.getAnnotations().add("javax.persistence.Table");
        classes.add(entity);

        return classes;
    }

    private List<ClassMetadata> createConfigurations() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata config = new ClassMetadata();
        config.setPackageName("com.example.config");
        config.setClassName("AppConfig");
        config.setFullyQualifiedName("com.example.config.AppConfig");
        config.setClassType(ClassMetadata.ClassType.CLASS);
        config.getAnnotations().add("org.springframework.context.annotation.Configuration");

        MethodMetadata beanMethod = new MethodMetadata();
        beanMethod.setMethodName("dataSource");
        beanMethod.setAccessModifiers(Set.of("public"));
        beanMethod.setReturnType("DataSource");
        beanMethod.getAnnotations().add("org.springframework.context.annotation.Bean");
        config.getMethods().add(beanMethod);

        classes.add(config);

        return classes;
    }

    private List<ClassMetadata> createComponentsWithDependencies() {
        List<ClassMetadata> classes = new ArrayList<>();

        ClassMetadata service = new ClassMetadata();
        service.setPackageName("com.example.service");
        service.setClassName("OrderService");
        service.setFullyQualifiedName("com.example.service.OrderService");
        service.setClassType(ClassMetadata.ClassType.CLASS);
        service.getAnnotations().add("org.springframework.stereotype.Service");

        // Add injected field (final, non-static = likely injected)
        FieldMetadata repoField = new FieldMetadata();
        repoField.setFieldName("userRepository");
        repoField.setType("UserRepository");
        repoField.setFinal(true);
        repoField.setStatic(false);
        service.getFields().add(repoField);

        classes.add(service);

        return classes;
    }
}

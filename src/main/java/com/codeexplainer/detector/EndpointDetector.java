package com.codeexplainer.detector;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.EndpointInfo;
import com.codeexplainer.core.model.MethodMetadata;
import com.codeexplainer.core.model.ParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting REST endpoints from class metadata.
 * Identifies Spring MVC and JAX-RS endpoints.
 */
@Service
public class EndpointDetector {

    private static final Logger log = LoggerFactory.getLogger(EndpointDetector.class);

    // Spring MVC annotations
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "Controller", "RestController");

    private static final Set<String> REQUEST_MAPPING_ANNOTATIONS = Set.of(
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping",
            "DeleteMapping", "PatchMapping");

    // JAX-RS annotations
    private static final Set<String> JAXRS_ANNOTATIONS = Set.of(
            "Path", "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");

    /**
     * Detects all REST endpoints from class metadata.
     */
    public List<EndpointInfo> detectEndpoints(List<ClassMetadata> classes) {
        log.info("Detecting REST endpoints from {} classes", classes.size());

        List<EndpointInfo> endpoints = new ArrayList<>();

        for (ClassMetadata clazz : classes) {
            if (isController(clazz)) {
                String basePath = extractClassPath(clazz);

                for (MethodMetadata method : clazz.getMethods()) {
                    Optional<EndpointInfo> endpoint = extractEndpoint(clazz, method, basePath);
                    endpoint.ifPresent(endpoints::add);
                }
            }
        }

        log.info("Detected {} endpoints", endpoints.size());
        return endpoints;
    }

    /**
     * Groups endpoints by HTTP method.
     */
    public Map<String, List<EndpointInfo>> groupByHttpMethod(List<EndpointInfo> endpoints) {
        return endpoints.stream()
                .collect(Collectors.groupingBy(EndpointInfo::getHttpMethod));
    }

    /**
     * Groups endpoints by controller class.
     */
    public Map<String, List<EndpointInfo>> groupByController(List<EndpointInfo> endpoints) {
        return endpoints.stream()
                .collect(Collectors.groupingBy(EndpointInfo::getControllerClass));
    }

    /**
     * Groups endpoints by path prefix.
     */
    public Map<String, List<EndpointInfo>> groupByPathPrefix(List<EndpointInfo> endpoints) {
        return endpoints.stream()
                .collect(Collectors.groupingBy(e -> {
                    String path = e.getPath();
                    if (path == null || path.isEmpty() || path.equals("/"))
                        return "/";
                    String[] parts = path.split("/");
                    return parts.length > 1 ? "/" + parts[1] : "/";
                }));
    }

    /**
     * Finds endpoints matching a pattern.
     */
    public List<EndpointInfo> findByPathPattern(List<EndpointInfo> endpoints, String pattern) {
        String regex = pattern.replace("*", ".*").replace("{", "\\{").replace("}", "\\}");
        return endpoints.stream()
                .filter(e -> e.getPath() != null && e.getPath().matches(regex))
                .collect(Collectors.toList());
    }

    /**
     * Gets endpoint statistics.
     */
    public EndpointStatistics getStatistics(List<EndpointInfo> endpoints) {
        Map<String, Long> byMethod = endpoints.stream()
                .collect(Collectors.groupingBy(EndpointInfo::getHttpMethod, Collectors.counting()));

        Map<String, Long> byController = endpoints.stream()
                .collect(Collectors.groupingBy(EndpointInfo::getControllerClass, Collectors.counting()));

        long authenticated = endpoints.stream()
                .filter(e -> e.getRequiredRoles() != null && !e.getRequiredRoles().isEmpty())
                .count();

        long deprecated = endpoints.stream()
                .filter(EndpointInfo::isDeprecated)
                .count();

        return new EndpointStatistics(
                endpoints.size(),
                byMethod,
                byController,
                (int) authenticated,
                (int) deprecated);
    }

    /**
     * Generates OpenAPI-style summary for endpoints.
     */
    public String generateEndpointSummary(List<EndpointInfo> endpoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("# API Endpoints\n\n");

        Map<String, List<EndpointInfo>> byController = groupByController(endpoints);

        for (Map.Entry<String, List<EndpointInfo>> entry : byController.entrySet()) {
            sb.append("## ").append(simplifyClassName(entry.getKey())).append("\n\n");

            for (EndpointInfo endpoint : entry.getValue()) {
                sb.append("### ").append(endpoint.getHttpMethod())
                        .append(" `").append(endpoint.getPath()).append("`\n\n");

                if (endpoint.getDescription() != null) {
                    sb.append(endpoint.getDescription()).append("\n\n");
                }

                if (endpoint.getRequestParameters() != null && !endpoint.getRequestParameters().isEmpty()) {
                    sb.append("**Parameters:**\n");
                    for (ParameterInfo param : endpoint.getRequestParameters()) {
                        sb.append("- `").append(param.getName()).append("`: ")
                                .append(param.getType()).append("\n");
                    }
                    sb.append("\n");
                }

                if (endpoint.getResponseType() != null) {
                    sb.append("**Returns:** `").append(endpoint.getResponseType()).append("`\n\n");
                }

                if (endpoint.isDeprecated()) {
                    sb.append("> ⚠️ **Deprecated**\n\n");
                }
            }
        }

        return sb.toString();
    }

    // ============= Helper Methods =============

    private boolean isController(ClassMetadata clazz) {
        return clazz.getAnnotations().stream()
                .anyMatch(a -> CONTROLLER_ANNOTATIONS.stream().anyMatch(a::contains) ||
                        a.contains("Path"));
    }

    private String extractClassPath(ClassMetadata clazz) {
        // Look for RequestMapping or Path annotation on class
        for (String annotation : clazz.getAnnotations()) {
            if (annotation.contains("RequestMapping") || annotation.contains("Path")) {
                return extractPathValue(annotation);
            }
        }
        return "";
    }

    private Optional<EndpointInfo> extractEndpoint(ClassMetadata clazz,
            MethodMetadata method,
            String basePath) {
        String httpMethod = null;
        String methodPath = "";

        for (String annotation : method.getAnnotations()) {
            // Spring MVC mappings
            if (annotation.contains("GetMapping")) {
                httpMethod = "GET";
                methodPath = extractPathValue(annotation);
            } else if (annotation.contains("PostMapping")) {
                httpMethod = "POST";
                methodPath = extractPathValue(annotation);
            } else if (annotation.contains("PutMapping")) {
                httpMethod = "PUT";
                methodPath = extractPathValue(annotation);
            } else if (annotation.contains("DeleteMapping")) {
                httpMethod = "DELETE";
                methodPath = extractPathValue(annotation);
            } else if (annotation.contains("PatchMapping")) {
                httpMethod = "PATCH";
                methodPath = extractPathValue(annotation);
            } else if (annotation.contains("RequestMapping")) {
                httpMethod = extractHttpMethod(annotation);
                methodPath = extractPathValue(annotation);
            }
            // JAX-RS annotations
            else if (annotation.contains("@GET") || annotation.endsWith(".GET"))
                httpMethod = "GET";
            else if (annotation.contains("@POST") || annotation.endsWith(".POST"))
                httpMethod = "POST";
            else if (annotation.contains("@PUT") || annotation.endsWith(".PUT"))
                httpMethod = "PUT";
            else if (annotation.contains("@DELETE") || annotation.endsWith(".DELETE"))
                httpMethod = "DELETE";
            else if (annotation.contains("@Path") || annotation.endsWith(".Path")) {
                methodPath = extractPathValue(annotation);
            }
        }

        if (httpMethod == null) {
            return Optional.empty();
        }

        String fullPath = normalizePath(basePath + methodPath);

        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setPath(fullPath);
        endpoint.setHttpMethod(httpMethod);
        endpoint.setMethodName(method.getMethodName());
        endpoint.setControllerClass(clazz.getFullyQualifiedName());
        endpoint.setResponseType(method.getReturnType());
        endpoint.setRequestParameters(method.getParameters());
        endpoint.setDeprecated(method.getAnnotations().stream().anyMatch(a -> a.contains("Deprecated")));

        // Check for security annotations
        List<String> roles = extractSecurityRoles(method);
        endpoint.setRequiredRoles(roles);

        // Generate description
        endpoint.setDescription(generateDescription(method));

        return Optional.of(endpoint);
    }

    private String extractPathValue(String annotation) {
        // Try to extract path from annotation value
        // e.g., @GetMapping("/users") or @RequestMapping(path = "/users")
        int start = annotation.indexOf("\"");
        if (start >= 0) {
            int end = annotation.indexOf("\"", start + 1);
            if (end > start) {
                return annotation.substring(start + 1, end);
            }
        }
        // Check for value attribute
        if (annotation.contains("value=")) {
            int valueStart = annotation.indexOf("value=") + 6;
            while (valueStart < annotation.length() &&
                    (annotation.charAt(valueStart) == '"' || annotation.charAt(valueStart) == ' ')) {
                valueStart++;
            }
            int valueEnd = valueStart;
            while (valueEnd < annotation.length() &&
                    annotation.charAt(valueEnd) != '"' && annotation.charAt(valueEnd) != ',') {
                valueEnd++;
            }
            if (valueEnd > valueStart) {
                return annotation.substring(valueStart, valueEnd);
            }
        }
        return "";
    }

    private String extractHttpMethod(String annotation) {
        if (annotation.contains("GET"))
            return "GET";
        if (annotation.contains("POST"))
            return "POST";
        if (annotation.contains("PUT"))
            return "PUT";
        if (annotation.contains("DELETE"))
            return "DELETE";
        if (annotation.contains("PATCH"))
            return "PATCH";
        return "GET"; // Default
    }

    private List<String> extractSecurityRoles(MethodMetadata method) {
        List<String> roles = new ArrayList<>();
        for (String annotation : method.getAnnotations()) {
            if (annotation.contains("RolesAllowed") ||
                    annotation.contains("PreAuthorize") ||
                    annotation.contains("Secured")) {
                // Extract role names from annotation
                int start = annotation.indexOf("\"");
                while (start >= 0) {
                    int end = annotation.indexOf("\"", start + 1);
                    if (end > start) {
                        roles.add(annotation.substring(start + 1, end));
                    }
                    start = annotation.indexOf("\"", end + 1);
                }
            }
        }
        return roles;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty())
            return "/";
        path = path.replace("//", "/");
        if (!path.startsWith("/"))
            path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String generateDescription(MethodMetadata method) {
        // Convert method name to readable description
        String name = method.getMethodName();
        StringBuilder desc = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c) && desc.length() > 0) {
                desc.append(" ");
            }
            desc.append(Character.toLowerCase(c));
        }
        return Character.toUpperCase(desc.charAt(0)) + desc.substring(1);
    }

    private String simplifyClassName(String fqn) {
        if (fqn == null)
            return "Unknown";
        return fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
    }

    // ============= Records =============

    public record EndpointStatistics(
            int totalEndpoints,
            Map<String, Long> byHttpMethod,
            Map<String, Long> byController,
            int authenticatedEndpoints,
            int deprecatedEndpoints) {
    }
}

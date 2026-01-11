package com.codeexplainer.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a REST endpoint detected in the JAR.
 */
public class EndpointInfo {

    private String httpMethod;
    private String urlPattern;
    private String controllerClass;
    private String handlerMethod;
    private String requestBodyType;
    private String responseType;
    private List<ParameterInfo> pathVariables = new ArrayList<>();
    private List<ParameterInfo> queryParams = new ArrayList<>();
    private List<String> consumes = new ArrayList<>();
    private List<String> produces = new ArrayList<>();

    // Additional fields for endpoint detection
    private List<ParameterInfo> requestParameters = new ArrayList<>();
    private boolean deprecated = false;
    private List<String> requiredRoles = new ArrayList<>();
    private String description;

    // Getters and Setters
    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(String controllerClass) {
        this.controllerClass = controllerClass;
    }

    public String getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(String handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    public String getRequestBodyType() {
        return requestBodyType;
    }

    public void setRequestBodyType(String requestBodyType) {
        this.requestBodyType = requestBodyType;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public List<ParameterInfo> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(List<ParameterInfo> pathVariables) {
        this.pathVariables = pathVariables;
    }

    public List<ParameterInfo> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<ParameterInfo> queryParams) {
        this.queryParams = queryParams;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(List<String> consumes) {
        this.consumes = consumes;
    }

    public List<String> getProduces() {
        return produces;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    // New field accessors

    public String getPath() {
        return urlPattern;
    }

    public void setPath(String path) {
        this.urlPattern = path;
    }

    public String getMethodName() {
        return handlerMethod;
    }

    public void setMethodName(String methodName) {
        this.handlerMethod = methodName;
    }

    public List<ParameterInfo> getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(List<ParameterInfo> requestParameters) {
        this.requestParameters = requestParameters;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<String> getRequiredRoles() {
        return requiredRoles;
    }

    public void setRequiredRoles(List<String> requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

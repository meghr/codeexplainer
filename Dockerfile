# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install graphviz for PlantUML diagram generation
RUN apk add --no-cache graphviz fontconfig ttf-dejavu

# Create non-root user
RUN addgroup -g 1001 codeexplainer && \
    adduser -u 1001 -G codeexplainer -D codeexplainer

# Create directories
RUN mkdir -p /app/data /app/output /app/logs && \
    chown -R codeexplainer:codeexplainer /app

# Copy the built JAR
COPY --from=builder /app/target/*.jar app.jar
RUN chown codeexplainer:codeexplainer app.jar

USER codeexplainer

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

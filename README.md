# Code Explainer

A comprehensive tool to analyze Java JAR files and Maven dependencies, generating architecture reports, flow diagrams, and actionable insights.

## Features

- 📦 **JAR File Analysis**: Upload and analyze any Java JAR file (Support for Java 8 to 25).
- 🔍 **Bytecode Inspection**: Deep analysis using ASM 9.7.1.
- 🏗️ **Architecture Overview**: Automatic detection of:
    - **Spring Components** (@Service, @Repository, @Controller)
    - **REST Endpoints** (@GetMapping, @PostMapping, etc.)
    - **Data Transfer Objects (DTOs)**
    - **Application Layers** (Controller -> Service -> Repository)
- 📊 **Visualizations**: 
    - **Class Diagrams**
    - **Sequence Flows**
    - **Access/Call Graphs**
- 📈 **Input/Output Analysis**: Analyze method parameters, return types, and potential side effects.
- 📄 **Reporting**: 
    - **Interactive Web Dashboard**
    - **PDF Exports**: Professionally formatted reports.
    - **JSON Exports**: Machine-readable analysis data.
- 🔗 **Maven Integration**: Resolve and analyze Maven coordinates directly.

## Quick Start

### Prerequisites

- **Java 25** (Required for running the latest build)
- **Maven 3.8+**
- **Docker** (Optional, for containerized run)

### Run with Maven

```bash
# Clone the repository
git clone https://github.com/your-org/code-explainer.git
cd code-explainer

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

The application will start at `http://localhost:8082`.

### Run with Docker

```bash
docker-compose up -d
```

## Architecture

The project follows a modular service-oriented architecture:

```
src/main/java/com/codeexplainer/
├── analyzer/        # Bytecode analysis (Method bodies, Instructions)
├── config/          # Spring & App Configuration
├── core/            # Domain models (ClassMetadata, MethodMetadata)
├── detector/        # Component & Endpoint detection strategies
├── diagram/         # PlantUML diagram generation
├── docs/            # Documentation & Report generation
├── export/          # Export services (PDF, JSON)
├── extractor/       # Metadata extraction (Classes, Hierarchies)
├── graph/           # Dependency graph builders
├── maven/           # Maven artifact resolution
├── parser/          # JAR & Class file parsing
└── web/             # REST Controllers & Exception Handling
```

## API Documentation

The application provides a comprehensive REST API. 
Full Swagger/OpenAPI documentation is available at:
`http://localhost:8082/swagger-ui/index.html`

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analyze` | Upload a JAR file for analysis. Returns `sessionId`. |
| GET | `/api/sessions/{id}/report` | Get full analysis report JSON. |
| GET | `/api/sessions/{id}/export` | Export report (`?format=pdf` or `?format=json`). |
| GET | `/api/sessions/{id}/docs` | Get generated markdown documentation. |
| GET | `/api/health` | Service health check. |

## User Guide

For detailed instructions on how to use the Web Interface and interpret reports, please refer to the [User Guide](USER_GUIDE.md).

## Technology Stack

- **Framework**: Spring Boot 3.2
- **Language**: Java 25 (Preview features enabled)
- **Bytecode**: ASM 9.7.1
- **PDF Generation**: iText 7 Core
- **Frontend**: Vanilla JS + CSS (Dark Theme)

## License

MIT License

# Code Explainer - User Guide

Welcome to the Code Explainer! This tool helps you understand the internal structure, dependencies, and behavior of Java applications by analyzing their JAR files without needing access to the original source code.

## 🚀 Getting Started

1.  **Open the Web Dashboard**
    Open your browser and navigate to `http://localhost:8082`.

2.  **Upload a JAR File**
    - Drag and drop your `.jar` file into the upload zone.
    - Or click the zone to browse your files.
    - *Note: Larger JARs may take a few seconds to analyze.*

## 🖥️ Web Interface Overview

Once a file is uploaded, you will be presented with the Analysis Dashboard tailored to your application.

### 1. Dashboard Summary
The top bar displays key metrics:
- **Classes**: Total number of classes found.
- **Methods**: Total method count.
- **Interfaces**: Number of interfaces processed.
- **Packages**: Number of unique packages.

### 2. Classes View
Explore the code structure.
- **Search**: Filter classes by name.
- **Table Columns**:
    - **Class Name**: The simple name of the class.
    - **Package**: The package it belongs to.
    - **Type**: (Class, Interface, Enum, Record, etc.)
    - **Methods/Fields**: Counts of structure members.

### 3. Endpoints View (API Discovery)
This view visualizes detected REST APIs (Spring MVC, JAX-RS).
- **HTTP Method**: Color-coded badges (GET, POST, PUT, DELETE).
- **Path**: The URL pattern.
- **Handler**: The Java method handling the request.

### 4. Components View (Spring Architecture)
Visualize the architectural building blocks.
- **Services**: Business logic components (`@Service`).
- **Repositories**: Data access layers (`@Repository`).
- **Controllers**: Web identifiers (`@Controller`, `@RestController`).

### 5. I/O Analysis
Deep dive into data flow.
- **DTOs**: Detected Data Transfer Objects.
- **Method I/O**: Breakdown of method parameters and return types.

### 6. Documentation & Export
The **Docs** tab provides a generated markdown summary of the project.

#### Exporting Reports
Use the buttons in the top-right corner of the Docs view:
- **Export PDF**: Downloads a professionally formatted, printable report containing executive summaries, component tables, and quality metrics.
- **Export JSON**: Downloads the raw analysis data for further processing or integration with other tools.

## 📊 Interpreting Reports

### Quality Issues
The report highlights potential code quality issues:
- **God Class**: Classes with too many methods or fields.
- **Deep Inheritance**: Class hierarchies deeper than recommended (e.g., > 5 levels).
- **Circular Dependencies**: Packages that depend on each other.

### Flow Diagrams
(If enabled in configuration)
- **Class Diagrams**: Show relationships (Inheritance `extends`, Implementation `implements`).
- **Sequence Digrams**: Show the partial flow of logic within identifying methods.

## ❓ FAQ

**Q: Can I analyze WAR files?**
A: Currently, the tool is optimized for JAR files. WAR files *may* work if they contain classes directly, but `WEB-INF/lib` scanning is experimental.

**Q: Why don't I see any Endpoints?**
A: The tool looks for standard Spring `@RequestMapping` compatible annotations. If you use a custom framework, they may not be detected.

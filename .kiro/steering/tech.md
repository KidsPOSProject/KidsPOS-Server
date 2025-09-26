# Technology Stack

## Architecture Overview

### System Architecture

- **Type**: Monolithic Web Application
- **Pattern**: MVC (Model-View-Controller)
- **Deployment**: Single JAR deployment
- **Database**: Embedded SQLite database
- **API Style**: RESTful API + Server-side rendering

### Architectural Decisions

- **Monolithic Design**: Simplicity and ease of deployment for event-based usage
- **Embedded Database**: Zero configuration, portable data storage
- **Server-side Rendering**: Reduced client complexity, works on any browser
- **Stateless REST API**: Clean separation between frontend and backend logic

## Backend Technology

### Core Framework

- **Spring Boot 3.2.0**: Enterprise Java framework
    - Spring Web: RESTful web services
    - Spring Data JPA: Database abstraction layer
    - Spring Boot DevTools: Development productivity

### Programming Language

- **Kotlin 2.0.21**: Modern JVM language
    - Target JVM: 21
    - Kotlin Spring Plugin: Spring integration
    - Kotlin JPA Plugin: JPA/Hibernate support
    - Kotlin Reflection: Runtime introspection

### Database Layer

- **SQLite**: Lightweight embedded database
    - JDBC Driver: `org.xerial:sqlite-jdbc`
    - Hibernate Dialect: `com.enigmabridge:hibernate4-sqlite-dialect:0.1.2`
    - Connection String: `jdbc:sqlite:./kidspos.db`

### ORM & Data Access

- **Hibernate JPA**: Object-relational mapping
    - Auto DDL: Update mode (auto-creates/updates schema)
    - Entity Management: Automatic table generation
    - Repository Pattern: Spring Data JPA repositories

### Additional Libraries

- **Jackson**: JSON serialization/deserialization
    - `jackson-module-kotlin`: Kotlin-specific support
- **iText7 Core 7.2.3**: PDF generation for reports
- **ZXing 3.5.1**: Barcode generation and scanning
    - Core library: Barcode algorithms
    - JavaSE library: Java-specific implementations
- **Apache POI 5.2.4**: Excel file generation for reports
    - POI: Core library for Excel manipulation
    - POI-OOXML: Support for XLSX format
- **SpringDoc OpenAPI 2.2.0**: API documentation
    - Swagger UI: Interactive API testing
    - OpenAPI 3.0 specification generation

## Frontend Technology

### Template Engine

- **Thymeleaf 3.0.15.RELEASE**: Server-side HTML templating
    - Natural templating: Valid HTML that renders correctly
    - Layout Dialect 3.1.0: Template inheritance support
    - Mode: HTML

### UI Framework

- **Bootstrap 5.2.1**: CSS framework (local copy)
    - Responsive design support
    - Component library
    - Grid system

### JavaScript Libraries

- **jQuery 3.6.0**: DOM manipulation and AJAX
- **jQuery UI 1.13.2**: UI interactions and widgets
- **DataTables 1.10.18**: Advanced table functionality
    - Sorting and filtering
    - Pagination
    - Search capabilities
    - Bootstrap 4 integration

### Additional Frontend

- **QR Code Generator**: JavaScript-based QR code generation
    - `qrcode.js` and `qrcode.min.js`
- **Custom Scripts**: `scripts.js` for application-specific behavior

## Development Environment

### Build System

- **Gradle 8.10**: Build automation
    - Build file: `build.gradle` (Groovy DSL)
    - Wrapper included for version consistency

### IDE Support

- **Kotlin Support**: Full IDE integration
- **Spring Boot Support**: Development tools and hot reload
- **JPA Support**: Entity and repository generation

### Code Quality Tools

- **Detekt 1.23.8**: Static code analysis for Kotlin
    - Configuration: `config/detekt/detekt.yml`
    - Baseline: `config/detekt/baseline.xml`
    - Custom rules and thresholds configured
    - Integration with Gradle build process

- **Ktlint 1.5.0**: Kotlin code formatting and linting
    - Automatic formatting enforcement
    - Integration with Gradle build
    - Pre-commit hook ready
    - Console output with colored error messages

### Testing Infrastructure

- **JUnit 5**: Unit testing framework
    - Spring Boot Test integration
    - MockK for mocking
    - JaCoCo for code coverage

- **Visual Regression Testing (VRT)**: UI consistency testing
    - **Playwright 1.48.0**: Browser automation and screenshot comparison
    - **GitHub Actions**: Automated VRT on every PR
    - **Node.js 18+**: Required for Playwright execution
    - Test commands:
        - `npm run test:vrt`: Run visual regression tests
        - `npm run test:vrt:update`: Update baseline snapshots
        - `npm run test:vrt:report`: View test results

### Development Commands

```bash
# Build the application
./gradlew build

# Build without detekt (faster)
./gradlew build -x detekt

# Create executable JAR
./gradlew bootJar

# Run the application
./gradlew bootRun

# Clean build artifacts
./gradlew clean

# Stage for deployment (creates app.jar)
./gradlew stage

# Clean staged JAR
./gradlew cleanJar

# Run tests
./gradlew test

# Generate test coverage report
./gradlew jacocoTestReport

# Run static code analysis
./gradlew detekt

# Format code with ktlint
./gradlew ktlintFormat

# Check code formatting
./gradlew ktlintCheck

# Visual regression testing
npm run test:vrt
npm run test:vrt:update
npm run test:vrt:report
```

## Common Commands

### Application Management

```bash
# Start the server
java -jar app.jar

# Build and stage
./gradlew clean stage

# Run in development mode
./gradlew bootRun
```

### Database Operations

```bash
# SQLite database location
ls -la ./kidspos.db

# Connect to database (requires sqlite3 CLI)
sqlite3 ./kidspos.db

# View tables
.tables

# View schema
.schema
```

### Git Operations

```bash
# Check status
git status

# Create commit
git add .
git commit -m "message"

# Push to remote
git push origin master
```

## Environment Variables

### Spring Configuration

- `SPRING_PROFILES_ACTIVE`: Active Spring profile (default: none)
- `SERVER_PORT`: Override default port 8080
- `SPRING_DATASOURCE_URL`: Override database URL

### JVM Options

- `-Xmx`: Maximum heap size (e.g., `-Xmx512m`)
- `-Xms`: Initial heap size (e.g., `-Xms256m`)
- `-Dspring.profiles.active`: Set active profile

### Application Properties

Configuration via `application.yaml`:

- `server.port`: Application port (default: 8080)
- `spring.datasource.url`: Database connection string
- `spring.jpa.hibernate.ddl-auto`: Schema management (update)
- `spring.thymeleaf.mode`: Template mode (HTML)

## Port Configuration

### Default Ports

- **Application Server**: 8080 (HTTP)
- **Database**: N/A (embedded SQLite file)
- **Development Tools**: Various (assigned by IDE)

### Port Override

```bash
# Via environment variable
export SERVER_PORT=9090
java -jar app.jar

# Via command line
java -jar app.jar --server.port=9090

# Via application.yaml
server:
  port: 9090
```

## Deployment Considerations

### Production Readiness

- **JAR Packaging**: Single deployable artifact
- **Embedded Server**: Tomcat included in JAR
- **Database Portability**: SQLite file can be backed up/moved
- **Configuration**: Externalized via application.yaml

### System Requirements

- **Java Runtime**: JRE 21 or higher
- **Memory**: Minimum 512MB, recommended 1GB
- **Disk Space**: 100MB for application + database growth
- **Network**: Port 8080 accessible
- **Node.js**: 18+ (for Visual Regression Testing only)

### Security Considerations

- **No Authentication**: Currently no user authentication
- **Local Network**: Designed for LAN usage
- **Data Privacy**: All data stored locally
- **HTTPS**: Not configured by default (can be added)

## Technology Constraints

### Current Limitations

- **Single Database**: SQLite doesn't support concurrent writes efficiently
- **No Caching Layer**: Direct database access for all operations
- **No Message Queue**: Synchronous processing only
- **Limited Scalability**: Monolithic architecture limits horizontal scaling

### Future Enhancement Opportunities

- **Database Migration**: PostgreSQL/MySQL for production
- **Caching**: Redis/Hazelcast for performance
- **Authentication**: Spring Security integration
- **API Documentation**: Swagger/OpenAPI integration
- **Monitoring**: Actuator endpoints for health checks
- **Logging**: Structured logging with SLF4J/Logback
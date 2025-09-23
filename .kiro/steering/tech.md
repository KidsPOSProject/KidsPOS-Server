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

- **Spring Boot 2.7.3**: Enterprise Java framework
    - Spring Web: RESTful web services
    - Spring Data JPA: Database abstraction layer
    - Spring Boot DevTools: Development productivity

### Programming Language

- **Kotlin 1.6.21**: Modern JVM language
    - Target JVM: 1.8
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

## Frontend Technology

### Template Engine

- **Thymeleaf 3.0.15.RELEASE**: Server-side HTML templating
    - Natural templating: Valid HTML that renders correctly
    - Layout Dialect 3.1.0: Template inheritance support
    - Mode: HTML

### UI Framework

- **Bootstrap 4.1.1**: CSS framework (local copy)
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

- **Gradle**: Build automation
    - Build file: `build.gradle` (Groovy DSL)
    - Wrapper included for version consistency

### IDE Support

- **Kotlin Support**: Full IDE integration
- **Spring Boot Support**: Development tools and hot reload
- **JPA Support**: Entity and repository generation

### Development Commands

```bash
# Build the application
./gradlew build

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

- **Java Runtime**: JRE 8 or higher
- **Memory**: Minimum 256MB, recommended 512MB
- **Disk Space**: 100MB for application + database growth
- **Network**: Port 8080 accessible

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
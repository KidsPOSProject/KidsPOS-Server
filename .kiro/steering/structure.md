# Project Structure

## Root Directory Organization

```
KidsPOS-Server/
├── .kiro/                      # Kiro spec-driven development
│   ├── specs/                  # Feature specifications
│   └── steering/               # Project steering documents
├── .claude/                    # Claude AI configuration
│   ├── commands/               # Custom slash commands
│   └── CLAUDE.md              # Project-specific AI instructions
├── src/                        # Source code root
│   └── main/                   # Main application code
│       ├── kotlin/            # Kotlin source files
│       └── resources/         # Resources and assets
├── build/                      # Build output directory
├── gradle/                     # Gradle wrapper files
├── .gitignore                  # Git ignore rules
├── build.gradle               # Gradle build configuration
├── settings.gradle            # Gradle settings
├── gradlew                    # Gradle wrapper script (Unix)
├── gradlew.bat               # Gradle wrapper script (Windows)
├── CLAUDE.md                  # AI assistant guidelines
├── kidspos.db                # SQLite database file
└── app.jar                    # Staged application JAR
```

## Source Code Structure

### Kotlin Package Structure (`src/main/kotlin/`)

```
info/nukoneko/kidspos/
├── common/                     # Shared utilities and extensions
│   ├── CharExtensions.kt      # Character utility extensions
│   ├── Commander.kt           # Command execution utilities
│   ├── IntExtensions.kt       # Integer utility extensions
│   ├── PrintCommand.kt        # Printing command definitions
│   └── StringExtensions.kt    # String utility extensions
├── receipt/                    # Receipt printing module
│   ├── ReceiptDetail.kt      # Receipt detail model
│   └── ReceiptPrinter.kt     # Receipt printing logic
└── server/                     # Main server application
    ├── ServerApplication.kt    # Spring Boot application entry
    ├── controller/            # Web controllers
    │   ├── api/              # REST API controllers
    │   └── front/            # Web UI controllers
    ├── entity/               # JPA entities
    ├── repository/           # Data access repositories
    └── service/              # Business logic services
```

### Controller Layer (`server/controller/`)

#### API Controllers (`controller/api/`)
```
api/
├── ItemApiController.kt        # Item CRUD operations
├── SaleApiController.kt        # Sales transactions
├── SettingApiController.kt     # Settings management
├── StaffApiController.kt       # Staff management
├── StoreApiController.kt       # Store management
└── model/                      # API request/response models
    ├── ItemBean.kt            # Item data transfer object
    ├── SaleBean.kt            # Sale data transfer object
    ├── SettingBean.kt         # Setting data transfer object
    ├── StaffBean.kt           # Staff data transfer object
    └── StoreBean.kt           # Store data transfer object
```

#### Frontend Controllers (`controller/front/`)
```
front/
├── IpController.kt             # IP address display
├── ItemsController.kt          # Items management UI
├── SalesController.kt          # Sales management UI
├── SettingsController.kt       # Settings UI
├── StaffsController.kt         # Staff management UI
├── StoresController.kt         # Store management UI
└── TopController.kt            # Homepage/dashboard
```

### Entity Layer (`server/entity/`)
```
entity/
├── ItemEntity.kt               # Product/item entity
├── SaleEntity.kt               # Sales transaction entity
├── SaleDetailEntity.kt         # Sales line item entity
├── SettingEntity.kt            # System setting entity
├── StaffEntity.kt              # Staff member entity
└── StoreEntity.kt              # Store location entity
```

### Repository Layer (`server/repository/`)
```
repository/
├── ItemRepository.kt           # Item data access
├── SaleRepository.kt           # Sale data access
├── SaleDetailRepository.kt     # Sale detail data access
├── SettingRepository.kt        # Setting data access
├── StaffRepository.kt          # Staff data access
└── StoreRepository.kt          # Store data access
```

### Service Layer (`server/service/`)
```
service/
├── BarcodeService.kt           # Barcode generation/scanning
├── ItemService.kt              # Item business logic
├── SaleService.kt              # Sales transaction logic
├── SettingService.kt           # Settings management logic
├── StaffService.kt             # Staff management logic
└── StoreService.kt             # Store management logic
```

### Resources Structure (`src/main/resources/`)

```
resources/
├── static/                     # Static web assets
│   ├── Bootstrap-4-4.1.1/    # Bootstrap CSS framework
│   ├── DataTables-1.10.18/   # DataTables plugin
│   ├── QrCode/                # QR code generation
│   ├── datatables.js          # DataTables configuration
│   ├── datatables.min.js      # DataTables minified
│   └── scripts.js             # Custom JavaScript
├── templates/                  # Thymeleaf HTML templates
│   ├── fragments/             # Reusable template fragments
│   ├── items/                # Item management views
│   ├── sales/                # Sales management views
│   ├── settings/             # Settings views
│   ├── staffs/               # Staff management views
│   ├── stores/               # Store management views
│   └── index.html            # Main page template
├── application.yaml           # Spring Boot configuration
└── tables.schema              # Database schema definition
```

## Code Organization Patterns

### Layered Architecture
1. **Controller Layer**: HTTP request handling and response formatting
2. **Service Layer**: Business logic and transaction management
3. **Repository Layer**: Database access and queries
4. **Entity Layer**: Domain models and database mapping

### Package Organization
- **By Feature**: Each feature has its own controller, service, repository
- **By Layer**: Clear separation between layers within features
- **Common Package**: Shared utilities isolated from business logic

### Dependency Flow
```
Controller → Service → Repository → Entity
     ↓          ↓           ↓
   Model     Common     Database
```

## File Naming Conventions

### Kotlin Files
- **Entities**: `{Name}Entity.kt` (e.g., `ItemEntity.kt`)
- **Controllers**: `{Name}Controller.kt` or `{Name}ApiController.kt`
- **Services**: `{Name}Service.kt` (e.g., `ItemService.kt`)
- **Repositories**: `{Name}Repository.kt` (e.g., `ItemRepository.kt`)
- **Models/DTOs**: `{Name}Bean.kt` (e.g., `ItemBean.kt`)
- **Extensions**: `{Type}Extensions.kt` (e.g., `StringExtensions.kt`)

### Resource Files
- **Templates**: `{feature}/{action}.html` (e.g., `items/list.html`)
- **Fragments**: `fragments/{name}.html` (e.g., `fragments/header.html`)
- **Static Assets**: Maintain original library structure
- **Configuration**: `application.yaml` (Spring Boot convention)

### Database Conventions
- **Table Names**: Lowercase, singular (e.g., `item`, `sale`)
- **Column Names**: Lowercase with underscores (e.g., `store_id`)
- **Entity Mapping**: Match table names via `@Table` annotation

## Import Organization

### Standard Import Order
1. Java standard library (`java.*`, `javax.*`)
2. Kotlin standard library (`kotlin.*`)
3. Spring Framework (`org.springframework.*`)
4. Third-party libraries (alphabetical)
5. Project imports (`info.nukoneko.kidspos.*`)

### Import Examples
```kotlin
// Standard libraries
import java.util.Date
import javax.persistence.Entity

// Kotlin
import kotlin.collections.List

// Spring
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired

// Third-party
import com.google.zxing.BarcodeFormat

// Project
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
```

## Key Architectural Principles

### Separation of Concerns
- **Clear Layer Boundaries**: Each layer has distinct responsibilities
- **No Cross-layer Skipping**: Controllers don't directly access repositories
- **Single Responsibility**: Each class focuses on one aspect

### Spring Boot Conventions
- **Annotation-based Configuration**: Use Spring annotations consistently
- **Dependency Injection**: `@Autowired` for dependencies
- **Component Scanning**: Rely on Spring's automatic component discovery

### Data Access Patterns
- **Repository Pattern**: Abstract database operations
- **Entity-first Design**: Entities define database schema
- **JPA Conventions**: Follow JPA/Hibernate best practices

### Code Style Guidelines
- **Kotlin Idioms**: Use Kotlin features (data classes, extensions)
- **Immutability**: Prefer `val` over `var` where possible
- **Null Safety**: Leverage Kotlin's null safety features
- **Expression Bodies**: Use expression syntax for simple functions

### Testing Structure (Future)
```
src/test/kotlin/                # Test source root
└── info/nukoneko/kidspos/
    ├── unit/                   # Unit tests
    ├── integration/           # Integration tests
    └── e2e/                   # End-to-end tests
```

### Configuration Management
- **Environment-specific**: Use Spring profiles for different environments
- **Externalized Config**: Keep configuration in `application.yaml`
- **Secure Defaults**: Never commit sensitive data to repository
- **Version Control**: Track all configuration changes

## Module Dependencies

### Core Dependencies
- `common` → Used by all modules
- `receipt` → Depends on `common`
- `server` → Depends on `common` and `receipt`

### External Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Thymeleaf
- SQLite JDBC Driver
- Hibernate SQLite Dialect
- Jackson Kotlin Module
- iText7 for PDF
- ZXing for Barcodes
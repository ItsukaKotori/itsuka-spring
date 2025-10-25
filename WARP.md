# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Itsuka-Spring is an enterprise-grade Spring Boot microservices framework with reusable starter components. The project is built with Gradle, Java 17, Spring Boot 3.5.6, and follows a modular architecture with multiple starter modules providing common functionality.

## Development Commands

### Build & Test
```bash
# Full build with tests
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :itsuka-system:test

# Run specific test class
./gradlew :itsuka-system:test --tests "*ClassName*"

# Run specific test method
./gradlew :itsuka-system:test --tests "*ClassName.methodName*"
```

### Run Applications
```bash
# Run system management module (port 8080)
./gradlew :itsuka-system:bootRun

# Run API gateway (port 8888)
./gradlew :itsuka-gateway:bootRun

# Run compiled JAR
java -jar itsuka-system/build/libs/itsuka-system-1.0.0.jar
```

## Architecture

### Module Structure

The project consists of starter modules in `common/` and application modules at the root level:

**Starter Modules** (reusable components):
- `itsuka-spring-boot-starter` - Core utilities, response wrappers, exceptions, reflection tools
- `itsuka-security-spring-boot-starter` - JWT authentication, access control (IP/User-Agent blacklist/whitelist), security context
- `itsuka-web-spring-boot-starter` - Global exception handling, request/response logging, Jackson configuration
- `itsuka-mybatis-plus-spring-boot-starter` - Database entity base classes, data permissions, auto-fill handlers
- `itsuka-redis-spring-boot-starter` - Redis integration with TTL randomization, database switching
- `itsuka-dict-spring-boot-starter` - Dictionary translation with `@DictField` and `@DictModel` annotations
- `itsuka-file-spring-boot-starter` - MinIO object storage integration
- `itsuka-mqtt-spring-boot-starter` - Netty-based MQTT server and client

**Application Modules**:
- `itsuka-system` - System management application (users, roles, resources)
- `itsuka-gateway` - Spring Cloud Gateway with security filters

### Entity Hierarchy

All domain entities follow this inheritance chain:

```
BaseDomain (id, createTime, updateTime)
  ↓
AuditableDomain (+ createBy, updateBy)
  ↓
SoftDeleteDomain (+ deleted field)
  ↓
BusinessDomain (+ status, sortOrder, remark)
  ↓
TenantDomain (+ tenantId)
```

**When to use each base class:**
- Use `BaseDomain` for simple entities needing only timestamps
- Use `AuditableDomain` when tracking who created/modified records
- Use `SoftDeleteDomain` when records should be soft-deleted
- Use `BusinessDomain` for typical business entities with status and sorting
- Use `TenantDomain` for multi-tenant scenarios

### Response Pattern

All API endpoints use `Response<T>` wrapper:
```java
Response.success(data)           // Success with data
Response.success()               // Success without data
Response.error(message)          // Business error
Response.error(code, message)    // Error with custom code
```

Business exceptions use `ServiceException` which is caught by `GlobalExceptionHandler`.

### Security Architecture

**JWT Authentication Flow:**
1. Client calls `/api/auth/login` (excluded from auth filter)
2. Server validates credentials and returns JWT token
3. Client includes token in `Authorization: Bearer <token>` header
4. `AuthenticationFilter` validates token and populates `SecurityContextManager`
5. Controllers access user via `SecurityContextManager.getCurrentUser()`

**Access Control:**
- Gateway and other services use `AccessControlManager` for IP/User-Agent/path filtering
- Configured via `itsuka.security.access-control.*` properties
- Supports whitelist/blacklist for IPs, paths, and User-Agent patterns

**Data Permissions:**
- `DataPermissionInterceptor` automatically filters queries based on creator
- Use `DataPermissionEscaper.escape(() -> ...)` to bypass for admin queries

### Configuration

Key configuration prefixes:
- `itsuka.security.jwt.*` - JWT token settings (secret, expiration, issuer)
- `itsuka.security.auth.*` - Authentication filter settings (enabled, excluded-paths)
- `itsuka.security.access-control.*` - IP/User-Agent/path whitelist/blacklist
- `itsuka.dict.*` - Dictionary translation settings (cache-type)
- `itsuka.mqtt.*` - MQTT server/client configuration

### Database Migrations

- Flyway is used for database migrations
- Migration scripts are in `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`
- Flyway is configured to baseline on existing databases

## Code Patterns

### Service Layer
- Services implement interfaces in `service/` package
- Implementations go in `service/impl/` with `@Service` annotation
- Use `@Transactional` for operations modifying multiple tables

### Controller Layer
- Use `@Tag` for OpenAPI documentation on controller classes
- Use `@Operation` for individual endpoints
- All endpoints return `Response<T>` type
- Input validation via `@Validated` on DTOs

### DTO Mapping
- Use MapStruct with `@Mapper(componentModel = "spring")` for DTO conversions
- Converters are auto-discovered by Spring as beans
- Convention: `EntityConverter` interface with `toDto()` and `toEntity()` methods

### Testing
- Test files in `src/test/java/` following same package structure
- Use `@SpringBootTest` for integration tests
- Testcontainers is available for database testing

## Common Issues

**JWT Secret:** Must be at least 256 bits (32+ characters). Each module (gateway, system) should have its own secret in production.

**MyBatis Logging:** The starter uses `StdOutImpl` for SQL logging. Disable in production by removing/commenting `log-impl` configuration.

**Redis Database Switching:** Use `@DbSwitch(database = N)` annotation on methods to dynamically switch Redis databases.

**Dictionary Translation:** Annotate response fields with `@DictField` or entire response classes with `@DictModel`. The interceptor automatically translates codes to readable text.

**MQTT Startup:** Use `@EnableMqttServer`, `@EnableMqttClient`, or `@EnableMqtt` (both) to activate MQTT functionality.

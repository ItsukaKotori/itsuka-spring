# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Itsuka-Spring is an enterprise-grade microservices framework built on Spring Boot 4.0.3, providing reusable Starter components for rapid application development. The project uses Java 21, Gradle 8.x with Kotlin DSL, and follows a multi-module architecture.

## Build Commands

### Core Development Commands
```bash
# Full build with tests
./gradlew clean build

# Build without tests (faster)
./gradlew clean build -x test

# Run the gateway application
./gradlew :itsuka-gateway:bootRun

# Run tests for specific module
./gradlew :common:itsuka-spring-boot-starter:test

# Publish to Nexus repository
./gradlew publish
```

### Gradle Configuration
- Java 21 target compatibility
- Parallel builds and build cache enabled
- Lombok 1.18.30 via `io.freefair.lombok` plugin
- MapStruct 1.6.3 for bean mapping

## Architecture

### Module Structure
```
itsuka-spring/
├── common/                                          # Aggregation module
│   ├── itsuka-spring-boot-starter/                  # Core utilities (Response, Result, LoginUser, exceptions)
│   ├── itsuka-web-spring-boot-starter/              # Web MVC enhancements (global exception handler, interceptors)
│   ├── itsuka-mybatis-plus-spring-boot-starter/     # Database (entity hierarchy, data permissions, auto-fill)
│   ├── itsuka-redis-spring-boot-starter/            # Redis integration (cache with TTL randomization, @DbSwitch)
│   ├── itsuka-dict-spring-boot-starter/             # Dictionary translation (@DictField, caching)
│   ├── itsuka-file-spring-boot-starter/             # MinIO object storage
│   ├── itsuka-mail-spring-boot-starter/             # Email functionality
│   └── itsuka-security-spring-boot-starter/         # JWT, access control, security context
└── itsuka-gateway/                                  # API Gateway (Spring Cloud Gateway)
```

### Entity Hierarchy (MyBatis Plus)
```
BaseDomain (ID)
  ↓
AuditableDomain (createTime, updateTime, createdBy, updatedBy)
  ↓
BusinessDomain (deleted, tenantId) - supports soft delete and multi-tenancy
  ↓
TenantDomain (extends BusinessDomain)
```

### Auto-Configuration Pattern
All starter modules use Spring Boot 4.x auto-configuration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (not the legacy `spring.factories`).

### Gateway Filter Chain
1. GlobalLoggingFilter - Request/response logging
2. AccessControlFilter - IP/User-Agent/path whitelist-blacklist
3. AuthenticationFilter - JWT validation
4. RateLimitFilter - Rate limiting

## Package Naming Conventions
- Core utilities: `org.itsuka.core.*`
- Security module: `com.itsuka.security.*` (note the different root package)
- Other starters: `org.itsuka.{module}.*`
- Gateway: `com.itsuka.gateway.*`

## Configuration Properties
All custom properties use the `itsuka.*` prefix:

```yaml
itsuka:
  security:
    jwt: # JWT token configuration
    auth: # Authentication (excluded paths)
    access-control: # IP/User-Agent whitelist-blacklist
  dict: # Dictionary translation
  mqtt: # MQTT configuration
```

## Technology Stack
- Spring Boot 4.0.3 (uses Jackson 3.x, Jakarta EE 10+)
- Spring Cloud 2025.1.1
- MyBatis Plus 3.5.16 (Spring Boot 4 starter)
- SpringDoc OpenAPI 2.8.13
- MySQL Connector 8.0.33
- MinIO 8.6.0
- JWT (jjwt 0.13.0)

## Key Design Patterns

### Security Context
Thread-safe user context management via `SecurityContextManager`. Use `@DataPermissionEscaper` annotation to bypass data permission checks when needed.

### Data Permissions
Implemented via MyBatis interceptor `DataPermissionInterceptor`. Filters data based on creator/tenant relationships.

### Dictionary Translation
Use `@DictField` annotation on entity fields for automatic dictionary translation. Supports both in-place replacement and additional text field modes. Caching available via ConcurrentHashMap or Redis.

### Caching Strategy
Redis caching implements TTL randomization to prevent cache avalanche (all keys expiring simultaneously).

## Gateway Configuration
- Default port: 8888
- Routes to system-service (8081) and music-service (8082)
- Swagger aggregation at `/v3/api-docs/{service}`
- CORS enabled globally
- Actuator endpoints: health, info, gateway

## Publishing
The project is configured for Maven Central/Nexus publishing with proper POM, sources, and javadoc artifacts. Requires `nexus.properties` file with repository credentials.

## Common Issues

### Spring Boot 4.x Migration Notes
- Uses `jakarta.*` namespace instead of `javax.*`
- AutoConfiguration.imports instead of spring.factories
- Jackson 3.x (cobertura-specific changes may be needed)

### Package Structure
The security module uses `com.itsuka.security.*` instead of `org.itsuka.security.*` - this is intentional but inconsistent with other modules.

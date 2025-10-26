# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview
- Multi-module Gradle project with Spring Boot 3.5.6 and Java 17
- Custom starter modules in `common/` directory for shared functionality
- MyBatis-Plus for ORM with custom extensions
- Flyway for database migrations
- MapStruct for object mapping

## Build Commands
```bash
# Build specific module
./gradlew :itsuka-system:build

# Run tests
./gradlew :itsuka-system:test

# BootRun specific module
./gradlew :itsuka-system:bootRun

# Build all modules
./gradlew build
```

## Code Style & Patterns

### Entity Hierarchy
- All entities extend `BaseDomain` (basic fields)
- Use `AuditableDomain` for audit fields (createBy, updateBy, createTime, updateTime)
- Use `SoftDeleteDomain` for logical delete (deleted field, version for optimistic locking)
- Example: `public class User extends AuditableDomain`

### MyBatis-Plus Usage
- Prefer BaseMapper methods over custom SQL
- Use `CustomMapper` for common query patterns
- Use `LambdaQueryWrapper` for dynamic queries
- Use `PageUtil.toPage()` and `PageUtil.toResult()` for pagination

### Service Layer Pattern
- Extend `ServiceImpl<Mapper, Entity>`
- Use `@Transactional` for write operations
- Inject Mappers directly to avoid circular dependencies
- Use `Wrappers.lambdaQuery()` for query building

### Database Migrations
- Located in `itsuka-system/src/main/resources/db/migration/`
- Follow naming pattern: `V{number}__Description.sql`
- Use Flyway for version control

### Custom Starters Usage
- `itsuka-spring-boot-starter`: Core utilities and base classes
- `itsuka-mybatis-plus-spring-boot-starter`: MyBatis-Plus extensions
- `itsuka-web-spring-boot-starter`: Web layer utilities
- `itsuka-security-spring-boot-starter`: Security configurations
- `itsuka-redis-spring-boot-starter`: Redis integration

### Testing Patterns
- Use `@SpringBootTest` for integration tests
- Use `@Transactional` on test methods that modify data
- Test classes in `src/test/java/` mirror main package structure
- Use `assert*` methods from JUnit

### Critical Gotchas
- Always use `userMapper.selectList()` instead of `userService.list()` to avoid circular dependencies
- Entities must extend appropriate domain base class for automatic field filling
- Use `@MapperScan(basePackages = "com.itsuka.system.mapper")` in main application
- Custom ObjectMapper is configured globally - use `@Autowired ObjectMapper objectMapper` if needed
- Logical delete uses `deleted` field with values 0 (not deleted) and 1 (deleted)

### MapStruct Usage
- Use `@Mapper` interface with `INSTANCE = Mappers.getMapper(ClassName.class)`
- Use `@Mapping` to ignore fields in DTO conversion
- Annotation processors configured in build.gradle

### Package Structure
- `domain/`: Entity classes
- `dto/`: Data Transfer Objects (grouped by feature)
- `mapper/`: MyBatis mapper interfaces
- `service/`: Service layer (interfaces and impl/)
- `controller/`: REST controllers
- `convert/`: MapStruct converters

### Documentation Guidelines
- DO NOT auto-generate API documentation after code changes
- SpringDoc OpenAPI is configured but should be used manually when needed
- API documentation should only be updated when major API changes are made
- Avoid committing auto-generated documentation files to version control

### Git Commit Guidelines
- ALWAYS generate Chinese commit messages when pushing code
- Use clear and descriptive Chinese commit messages that explain the changes
- Follow conventional commit format with Chinese descriptions

### Response Pattern
- Use `Response.success(data)` for successful responses
- Use `Response.error(message)` for errors
- All responses wrapped in `Response<T>` from `itsuka-spring-boot-starter`
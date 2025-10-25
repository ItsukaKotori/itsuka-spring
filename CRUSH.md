# Itsuka Spring Development Guide

## Build Commands
- **Build all modules**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Run single test**: `./gradlew :itsuka-system:test --tests "*ClassName*"`
- **Run single method**: `./gradlew :itsuka-system:test --tests "*ClassName.methodName*"`
- **Run gateway**: `./gradlew :itsuka-gateway:bootRun`
- **Run system**: `./gradlew :itsuka-system:bootRun`
- **Clean build**: `./gradlew clean build`

## Code Style Guidelines

### Java Conventions
- **Java 17** compatibility, UTF-8 encoding
- Use **Lombok** annotations (`@Data`, `@RequiredArgsConstructor`, `@EqualsAndHashCode(callSuper = true)`)
- Use **MapStruct** for DTO/Entity conversion with `@Mapper` annotation
- Domain entities extend `BaseDomain` or `AuditableDomain` from mybatis-plus-starter

### Package Structure
- Controllers: `controller/` with `@Tag` and `@Operation` annotations
- Services: `service/` with `impl/` subpackage for implementations
- Domain entities: `domain/` extending base domain classes
- DTOs: `dto/` with feature subpackages and validation annotations
- Mappers: `mapper/` for MyBatis interfaces, XML files in `resources/mapper/`

### Error Handling
- Use `Response<T>` wrapper for all API responses
- Throw `ServiceException` for business logic errors
- Use `@Validated` on DTOs for input validation
- Implement proper exception handling with global exception handler

### Database & Testing
- MyBatis Plus for database operations with custom mappers
- Flyway migrations in `src/main/resources/db/migration/`
- Test files follow Maven structure with `@SpringBootTest`
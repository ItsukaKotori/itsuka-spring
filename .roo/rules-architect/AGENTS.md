# AGENTS.md - Architect Mode

This file provides guidance to agents when working with code in this repository.

## Architect Mode Specific Rules

### Project Architecture Understanding
- Multi-module Gradle project with custom starter modules in `common/` directory
- Spring Boot 3.5.6 with Java 17
- MyBatis-Plus for ORM with custom extensions
- Layered architecture: Controller → Service → Mapper → Database

### Module Structure Analysis
- `itsuka-system`: Main application module with user management and organization
- `common/itsuka-spring-boot-starter`: Core utilities and base classes
- `common/itsuka-mybatis-plus-spring-boot-starter`: MyBatis-Plus extensions
- `common/itsuka-web-spring-boot-starter`: Web layer utilities
- `common/itsuka-security-spring-boot-starter`: Security configurations
- `common/itsuka-redis-spring-boot-starter`: Redis integration

### Design Patterns
- **Domain-Driven Design**: Entities extend base domain classes
- **Repository Pattern**: MyBatis-Plus mappers with custom extensions
- **Service Layer Pattern**: ServiceImpl extends ServiceImpl<Mapper, Entity>
- **DTO Pattern**: Separate DTOs for each feature with MapStruct conversion
- **Starter Pattern**: Custom auto-configuration modules for shared functionality

### Database Design Principles
- Use Flyway for version control with V{number}__Description.sql naming
- Logical delete pattern with `deleted` field (0=not deleted, 1=deleted)
- Optimistic locking with `version` field
- Audit fields automatically filled via @TableField(fill = FieldFill.INSERT/UPDATE)

### Integration Points
- **Security**: JWT-based authentication with role-based authorization
- **Caching**: Redis integration through custom starter
- **API Documentation**: SpringDoc OpenAPI 3.0
- **Database**: MySQL for production, H2 for development/testing

### Custom Extension Points
- Custom ObjectMapper configuration in itsuka-spring-boot-starter
- MyBatis-Plus extensions in itsuka-mybatis-plus-spring-boot-starter
- Global exception handling in itsuka-web-spring-boot-starter
- Security utilities in itsuka-security-spring-boot-starter

### Architectural Decisions
- **Circular Dependency Prevention**: Inject Mappers directly instead of Services
- **Base Class Hierarchy**: BaseDomain → AuditableDomain → SoftDeleteDomain
- **Auto-Configuration**: Spring Boot starter pattern for shared functionality
- **Testing Strategy**: @SpringBootTest with @Transactional for data-modifying tests

### Technology Stack Constraints
- Java 17 minimum requirement
- Spring Boot 3.5.6 framework
- MyBatis-Plus 3.5.7 for ORM
- MapStruct 1.5.3 for object mapping
- MySQL 8.0+ for production database

### Key Architectural Questions
When designing new features, consider:
1. Which base domain class should entities extend?
2. How to avoid circular dependencies between services?
3. Should this functionality go in a custom starter?
4. What are the security implications?
5. How will this affect the database schema?
# AGENTS.md - Ask Mode

This file provides guidance to agents when working with code in this repository.

## Ask Mode Specific Rules

### Project Context Analysis
- Multi-module Gradle project with Spring Boot 3.5.6 and Java 17
- Custom starter modules provide shared functionality across modules
- MyBatis-Plus with custom extensions for ORM
- Organization management is a core feature with tree structure

### Key Components to Understand
- **itsuka-system**: Main application with user, role, resource, and organization management
- **common modules**: Shared functionality through custom Spring Boot starters
- **Database**: MySQL with Flyway migrations, H2 for testing
- **Security**: JWT-based authentication with role-based authorization

### Documentation Sources
- **README.md**: Module overview and API documentation
- **Migration scripts**: Database schema evolution in `src/main/resources/db/migration/`
- **Code structure**: Standard Spring Boot package organization
- **Custom starters**: Documentation in each starter's README.md

### Common Questions to Prepare For
When users ask about:
1. **Database issues**: Check migration scripts and entity relationships
2. **Authentication problems**: Review security configuration and JWT setup
3. **API questions**: Consult README.md for endpoint documentation
4. **Architecture decisions**: Review custom starter modules and base classes
5. **Build/deployment issues**: Check Gradle configuration and module dependencies

### Non-Obvious Project Knowledge
- **Entity inheritance pattern**: BaseDomain → AuditableDomain → SoftDeleteDomain
- **Circular dependency solution**: Inject Mappers directly, not Services
- **Custom utility pattern**: CustomMapper provides common query methods
- **Pagination pattern**: PageUtil.toPage() and PageUtil.toResult() for consistent pagination
- **Response pattern**: Response.success(data) and Response.error(message) for API responses
- **Migration pattern**: V{number}__Description.sql naming convention with Flyway

### Critical Gotchas
- **Logical delete**: Uses `deleted` field with values 0 (not deleted) and 1 (deleted)
- **Optimistic locking**: Uses `version` field from SoftDeleteDomain
- **Audit fields**: Automatically filled via @TableField(fill = FieldFill.INSERT/UPDATE)
- **Mapper scanning**: Must use @MapperScan(basePackages = "com.itsuka.system.mapper")
- **Custom ObjectMapper**: Globally configured in itsuka-spring-boot-starter

### Feature Relationships
- **Users → Organizations**: Many-to-one via organization_id field
- **Users → Roles**: Many-to-many via user_role table
- **Roles → Resources**: Many-to-many via resource_role table
- **Organizations**: Tree structure via parent_id field

### Testing Context
- Integration tests use @SpringBootTest with @Transactional
- H2 in-memory database for testing
- Test classes mirror main package structure
- Use assert* methods from JUnit for assertions
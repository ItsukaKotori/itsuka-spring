# AGENTS.md - Code Mode

This file provides guidance to agents when working with code in this repository.

## Code Mode Specific Rules

### MyBatis-Plus Optimization
- ALWAYS prefer BaseMapper methods over custom SQL
- Use `CustomMapper` only for complex queries that can't be built with LambdaQueryWrapper
- Use `Wrappers.lambdaQuery()` for dynamic queries
- Use `PageUtil.toPage()` and `PageUtil.toResult()` for pagination
- Example: `list(queryWrapper)` instead of `mapper.selectByCustomMethod()`

### Circular Dependency Prevention
- Inject Mappers directly instead of Services to avoid circular dependencies
- NEVER inject UserService in OrganizationService - inject UserMapper instead
- Use `userMapper.selectList(queryWrapper)` instead of `userService.list()`

### Entity Inheritance
- Use `BaseDomain` for basic entities (id only)
- Use `AuditableDomain` for entities needing audit fields
- Use `SoftDeleteDomain` for entities requiring logical delete
- Call `entity.initForInsert(userId)` for new entities with audit info

### MapStruct Conversion
- Always use `@Mapping(target = "fieldName", ignore = true)` for fields to ignore
- Use `INSTANCE = Mappers.getMapper(ClassName.class)` pattern
- Ignore audit fields in DTO conversions: createTime, updateTime, createBy, updateBy

### Service Layer Patterns
- Extend `ServiceImpl<Mapper, Entity>` for standard CRUD operations
- Use `@Transactional` for write operations
- Use `getById()` instead of custom select methods
- Use `count(queryWrapper)` instead of custom count methods

### Testing Requirements
- Use `@SpringBootTest` for integration tests
- Use `@Transactional` on test methods that modify data
- Test classes must mirror main package structure
- Use `assert*` methods from JUnit for assertions

### Critical Gotchas
- Logical delete uses `deleted` field: 0=not deleted, 1=deleted
- Optimistic locking uses `version` field from SoftDeleteDomain
- Custom ObjectMapper is globally configured - use `@Autowired ObjectMapper objectMapper`
- Database migrations in `src/main/resources/db/migration/` follow V{number}__Description.sql pattern
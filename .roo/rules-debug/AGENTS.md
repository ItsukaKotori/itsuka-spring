# AGENTS.md - Debug Mode

This file provides guidance to agents when working with code in this repository.

## Debug Mode Specific Rules

### Database Debugging
- Check Flyway migrations in `itsuka-system/src/main/resources/db/migration/`
- Use H2 database for debugging - configured in itsuka-system build.gradle
- MyBatis-Plus SQL logging: add `logging.level.com.itsuka.system.mapper=DEBUG` to application.yml

### Common Debug Scenarios
- Circular dependency issues: Check for Service-to-Service injections, use Mapper injection instead
- Logical delete problems: Check `deleted` field values (0=not deleted, 1=deleted)
- Optimistic locking failures: Check `version` field conflicts in SoftDeleteDomain entities

### Logging Configuration
- Application logs: Check console output for Spring Boot startup
- SQL logs: Enable MyBatis-Plus logging with `mybatis-plus.configuration.log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`
- Test logs: Use `@Slf4j` with `log.info()` for debugging test flow

### Test Debugging
- Use `@Transactional` on test methods that modify data
- Check data rollback between tests
- Use H2 in-memory database for isolated test environments

### Security Debugging
- JWT token issues: Check secret key length (must be at least 256 bits)
- Permission problems: Verify role-resource associations in database
- Authentication failures: Check UserDetailsService implementation

### Performance Debugging
- Database connection pool: Check HikariCP metrics in Actuator endpoints
- Slow queries: Enable MyBatis-Plus performance monitoring
- Memory issues: Check Gradle JVM args in gradle.properties

### Critical Debug Points
- Mapper scan failures: Verify `@MapperScan(basePackages = "com.itsuka.system.mapper")`
- Auto-configuration loading: Check starter imports in META-INF files
- Bean creation conflicts: Check `@Primary` annotations on duplicate beans
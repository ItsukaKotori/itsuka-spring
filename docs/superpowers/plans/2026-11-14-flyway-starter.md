# itsuka-flyway-spring-boot-starter 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增一个 Flyway starter，让接入方从多个 classpath 路径加载相互独立的 SQL 迁移流，并默认关闭 Boot 原生 Flyway 自动配置。

**Architecture:** 每个 `itsuka.flyway.sources[]` 对应一个独立 `Flyway` 实例和独立历史表。Registrar 在自动配置解析阶段动态注册 Bean，并让 Boot 的 `FlywayAutoConfiguration` 因 `@ConditionalOnMissingBean(Flyway.class)` 退让。EnvironmentPostProcessor 在没有 source 时强制关闭原生 Flyway，`native-enabled=true` 时切回 Boot 原生模式。

**Tech Stack:** Java 21、Spring Boot 4.0.3、Flyway 11.14.1、Gradle Kotlin DSL、JUnit 5、AssertJ、H2、Testcontainers。

**Spec:** `docs/superpowers/specs/2026-11-14-flyway-starter-design.md`

## Global Constraints

- 模块名：`common/itsuka-flyway-spring-boot-starter`；包名：`org.itsuka.flyway`。
- Java 21；Spring Boot 4.0.3；Flyway 版本由 Boot BOM 管理（11.14.1）。
- 只新增模块，并修改 `settings.gradle.kts` 与根 `build.gradle.kts` 的模块描述；不改其他模块。
- `api("org.springframework.boot:spring-boot-starter-flyway")`；`runtimeOnly("org.flywaydb:flyway-mysql")`。
- 每个 source 一个独立历史表，默认 `flyway_schema_history_<normalized-name>`。
- source 间按配置顺序串行；用 `dependsOn` 链硬保证；单 source 内由 Flyway 排序。
- `native-enabled=true` 与 `sources` 非空互斥，启动失败；与 `enabled` 无关。
- 通用 `spring.flyway.*` 配置完整映射（跳过 `locations`、`table`）；`fail-on-missing-locations` 由 starter 覆盖。
- 测试使用 H2 `MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`；MySQL IT 使用 `mysql:8.4`。
- 提交信息使用 `feat(flyway): ...` / `test(flyway): ...` / `chore(flyway): ...`。

## Review Focus

以下输入/失败模式最容易伤到使用者，每个都在对应任务中有测试：

1. 用户显式设置 `spring.flyway.enabled=false` 时，source 仍迁移且 JPA 仍认为 schema 受管（Task 4 的 `SchemaManagementProvider` 测试）。
2. `native-enabled=true` 同时配置 sources 时，无论 `enabled` 真假都启动失败（Task 2 与 Task 5）。
3. source 顺序与失败停止：第 N 个失败后第 N+1 个不执行（Task 4 顺序测试、Task 5 失败测试）。
4. 不同 source 重复 `V1` 版本仍各自迁移到独立历史表（Task 4 主集成测试）。
5. 路径写错时默认启动失败，而不是 Flyway 默认的静默跳过（Task 5 缺失路径测试）。

---

## File Structure

```text
common/itsuka-flyway-spring-boot-starter/
├── build.gradle.kts
├── src/main/java/org/itsuka/flyway/
│   ├── ItsukaFlywayAutoConfiguration.java
│   ├── ItsukaFlywayDataSourceResolver.java
│   ├── ItsukaFlywayEnvironmentPostProcessor.java
│   ├── ItsukaFlywayFactory.java
│   ├── ItsukaFlywayProperties.java
│   ├── ItsukaFlywayPropertiesValidator.java
│   ├── ItsukaFlywayRegistrar.java
│   └── ItsukaFlywaySchemaManagementProvider.java
├── src/main/resources/META-INF/spring.factories
├── src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── src/test/
    ├── java/org/itsuka/flyway/
    │   ├── ItsukaFlywayAutoConfigurationTest.java
    │   ├── ItsukaFlywayDataSourceResolverTest.java
    │   ├── ItsukaFlywayEnvironmentPostProcessorIT.java
    │   ├── ItsukaFlywayEnvironmentPostProcessorTest.java
    │   ├── ItsukaFlywayFactoryTest.java
    │   ├── ItsukaFlywayPropertiesTest.java
    │   ├── ItsukaFlywayPropertiesValidatorTest.java
    │   └── MySqlFlywayIT.java
    └── resources/
        ├── db/migration/V1__native.sql
        └── flyway/
            ├── fail/V1__fail.sql
            ├── order/V1__create_order.sql
            ├── order/V2__insert_order.sql
            ├── user/V1__create_user.sql
            └── user/V2__insert_user.sql
```

修改：

- `settings.gradle.kts`
- `build.gradle.kts`

---

### Task 1: 模块骨架、配置属性与校验

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `common/itsuka-flyway-spring-boot-starter/build.gradle.kts`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayProperties.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayPropertiesValidator.java`
- Test: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayPropertiesTest.java`
- Test: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayPropertiesValidatorTest.java`

**Interfaces:**
- Produces:
  - `ItsukaFlywayProperties`：`isEnabled()`、`setEnabled(boolean)`、`isNativeEnabled()`、`setNativeEnabled(boolean)`、`isFailOnMissingLocations()`、`setFailOnMissingLocations(boolean)`、`getSources()`、`setSources(List<Source>)`。
  - `ItsukaFlywayProperties.Source`：`getName()/setName(String)`、`getLocation()/setLocation(String)`、`getTable()/setTable(String)`、`normalizedName(): String`、`resolvedTable(): String`。
  - `ItsukaFlywayPropertiesValidator.validate(ItsukaFlywayProperties): void`（包级私有静态方法，失败抛 `IllegalStateException`）。

- [ ] **Step 1: 写属性失败测试**

```java
class ItsukaFlywayPropertiesTest {

    @Test
    void defaults() {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isNativeEnabled()).isFalse();
        assertThat(properties.isFailOnMissingLocations()).isTrue();
        assertThat(properties.getSources()).isEmpty();
    }

    @Test
    void derivesTableNameFromNormalizedName() {
        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order-center");
        assertThat(source.normalizedName()).isEqualTo("order_center");
        assertThat(source.resolvedTable()).isEqualTo("flyway_schema_history_order_center");
    }

    @Test
    void explicitTableWins() {
        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setTable("custom_history");
        assertThat(source.resolvedTable()).isEqualTo("custom_history");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayPropertiesTest'`
Expected: 编译失败，找不到 `ItsukaFlywayProperties`。

- [ ] **Step 3: 创建模块与属性类**

`settings.gradle.kts` 在 `itsuka-log-spring-boot-starter` 后增加：

```kotlin
include("common:itsuka-flyway-spring-boot-starter")
```

根 `build.gradle.kts` 的 `moduleDescriptions` 增加：

```kotlin
"itsuka-flyway-spring-boot-starter" to "Flyway 多路径迁移：多个 classpath 路径独立版本流与历史表"
```

模块 `build.gradle.kts`：

```kotlin
dependencies {
    api(project(":common:itsuka-spring-boot-starter"))
    api("org.springframework.boot:spring-boot-starter-flyway")

    runtimeOnly("org.flywaydb:flyway-mysql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.flywaydb:flyway-mysql")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}
```

`ItsukaFlywayProperties`：

```java
@ConfigurationProperties("itsuka.flyway")
public class ItsukaFlywayProperties {
    private boolean enabled = true;
    private boolean nativeEnabled = false;
    private boolean failOnMissingLocations = true;
    private List<Source> sources = new ArrayList<>();
    // getters/setters

    public static class Source {
        private String name;
        private String location;
        private String table;
        // getters/setters

        public String normalizedName() {
            return (name != null) ? name.replace('-', '_') : null;
        }

        public String resolvedTable() {
            return (table != null && !table.isBlank())
                ? table
                : "flyway_schema_history_" + normalizedName();
        }
    }
}
```

- [ ] **Step 4: 运行属性测试确认通过**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayPropertiesTest'`
Expected: PASS。

- [ ] **Step 5: 写校验失败测试**

需要的 import：`org.itsuka.flyway.ItsukaFlywayProperties.Source`（测试与生产类同包，但嵌套类需要显式 import）以及 AssertJ。

```java
class ItsukaFlywayPropertiesValidatorTest {

    private static ItsukaFlywayProperties properties(Source... sources) {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        properties.setSources(List.of(sources));
        return properties;
    }

    private static Source source(String name, String location, String table) {
        Source source = new Source();
        source.setName(name);
        source.setLocation(location);
        source.setTable(table);
        return source;
    }

    @Test
    void acceptsValidSources() {
        assertThatCode(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", "classpath:flyway/order", null),
                       source("user", "classpath:flyway/user", "custom_user_history"))))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("", "classpath:flyway/order", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[0].name");
    }

    @Test
    void rejectsInvalidName() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("1order", "classpath:flyway/order", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[0].name");
    }

    @Test
    void rejectsDuplicateNormalizedNames() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order-center", "classpath:flyway/a", null),
                       source("order_center", "classpath:flyway/b", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("重复");
    }

    @Test
    void rejectsBlankLocation() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", " ", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[0].location");
    }

    @Test
    void rejectsInvalidExplicitTable() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", "classpath:flyway/order", "bad-name"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("table");
    }

    @Test
    void rejectsTooLongDerivedTable() {
        String longName = "a".repeat(60);
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source(longName, "classpath:flyway/order", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("64");
    }

    @Test
    void rejectsDuplicateTablesIgnoringCase() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", "classpath:flyway/order", "same_history"),
                       source("user", "classpath:flyway/user", "SAME_HISTORY"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("历史表");
    }
}
```

- [ ] **Step 6: 运行校验测试确认失败**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayPropertiesValidatorTest'`
Expected: 编译失败，找不到 `ItsukaFlywayPropertiesValidator`。

- [ ] **Step 7: 实现 `ItsukaFlywayPropertiesValidator`**

签名：

```java
final class ItsukaFlywayPropertiesValidator {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
    private static final Pattern TABLE_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final int MAX_TABLE_LENGTH = 64;

    static void validate(ItsukaFlywayProperties properties) { ... }
}
```

规则：

- `name` 非空且匹配 `NAME_PATTERN`；`normalizedName()` 转小写后唯一。
- `location` 非空。
- `resolvedTable()` 匹配 `TABLE_PATTERN`、长度 ≤ 64、转小写后唯一。
- 错误信息以 `itsuka.flyway.sources[<index>]` 开头，并包含具体字段名。

- [ ] **Step 8: 运行本任务全部测试**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test`
Expected: PASS（其他测试类还不存在，正常）。

- [ ] **Step 9: 提交**

```bash
git add settings.gradle.kts build.gradle.kts common/itsuka-flyway-spring-boot-starter
git commit -m "feat(flyway): add module scaffold, properties, and validation"
```

---

### Task 2: 环境后处理器与模式规则

**Files:**
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayEnvironmentPostProcessor.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Test: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayEnvironmentPostProcessorTest.java`

**Interfaces:**
- Consumes: `ItsukaFlywayProperties`（Task 1）。
- Produces:
  - `ItsukaFlywayEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered`。
  - `static final String DISABLED_PROPERTY_SOURCE_NAME = "itsukaFlywayDefaults"`。
  - `static final String CONFLICT_MESSAGE = "itsuka.flyway.native-enabled=true 与 itsuka.flyway.sources 不能同时使用；请二选一。"`。

- [ ] **Step 1: 写失败测试**

```java
class ItsukaFlywayEnvironmentPostProcessorTest {

    private final ItsukaFlywayEnvironmentPostProcessor postProcessor =
        new ItsukaFlywayEnvironmentPostProcessor();

    private static MockEnvironment environment(String... properties) {
        MockEnvironment environment = new MockEnvironment();
        for (int i = 0; i < properties.length; i += 2) {
            environment.setProperty(properties[i], properties[i + 1]);
        }
        return environment;
    }

    @Test
    void disablesNativeWhenNoSourcesConfigured() {
        MockEnvironment environment = environment();
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(environment.getPropertySources().contains("itsukaFlywayDefaults")).isTrue();
    }

    @Test
    void disablesNativeWhenStarterDisabled() {
        MockEnvironment environment = environment("itsuka.flyway.enabled", "false");
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
    }

    @Test
    void leavesNativeAloneWhenSourcesConfigured() {
        MockEnvironment environment = environment(
            "itsuka.flyway.sources[0].name", "order",
            "itsuka.flyway.sources[0].location", "classpath:flyway/order");
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isNull();
    }

    @Test
    void leavesNativeAloneInNativeMode() {
        MockEnvironment environment = environment("itsuka.flyway.native-enabled", "true");
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isNull();
    }

    @Test
    void rejectsNativeModeWithSources() {
        MockEnvironment environment = environment(
            "itsuka.flyway.native-enabled", "true",
            "itsuka.flyway.sources[0].name", "order",
            "itsuka.flyway.sources[0].location", "classpath:flyway/order");
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("二选一");
    }

    @Test
    void rejectsNativeModeWithSourcesEvenWhenStarterDisabled() {
        MockEnvironment environment = environment(
            "itsuka.flyway.enabled", "false",
            "itsuka.flyway.native-enabled", "true",
            "itsuka.flyway.sources[0].name", "order",
            "itsuka.flyway.sources[0].location", "classpath:flyway/order");
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("二选一");
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayEnvironmentPostProcessorTest'`
Expected: 编译失败，找不到 `ItsukaFlywayEnvironmentPostProcessor`。

- [ ] **Step 3: 实现后处理器与注册文件**

逻辑：

需要的 import：`org.springframework.boot.EnvironmentPostProcessor`（Boot 4 的新包；旧的 `org.springframework.boot.env.EnvironmentPostProcessor` 已 deprecated for removal）。

```java
public class ItsukaFlywayEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DISABLED_PROPERTY_SOURCE_NAME = "itsukaFlywayDefaults";
    static final String CONFLICT_MESSAGE =
        "itsuka.flyway.native-enabled=true 与 itsuka.flyway.sources 不能同时使用；请二选一。";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ItsukaFlywayProperties properties = Binder.get(environment)
            .bind("itsuka.flyway", ItsukaFlywayProperties.class)
            .orElseGet(ItsukaFlywayProperties::new);

        if (properties.isNativeEnabled() && !properties.getSources().isEmpty()) {
            throw new IllegalStateException(CONFLICT_MESSAGE);
        }
        boolean multiSourceWillRun = properties.isEnabled()
            && !properties.isNativeEnabled()
            && !properties.getSources().isEmpty();
        if (properties.isNativeEnabled() || multiSourceWillRun) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(
            DISABLED_PROPERTY_SOURCE_NAME,
            Map.of("spring.flyway.enabled", "false")));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

`src/main/resources/META-INF/spring.factories`：

```properties
org.springframework.boot.EnvironmentPostProcessor=\
org.itsuka.flyway.ItsukaFlywayEnvironmentPostProcessor
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayEnvironmentPostProcessorTest'`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-flyway-spring-boot-starter
git commit -m "feat(flyway): disable native flyway by default with native opt-in"
```

---

### Task 3: 数据源解析与 Flyway 工厂

**Files:**
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayDataSourceResolver.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayFactory.java`
- Test: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayDataSourceResolverTest.java`
- Test: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayFactoryTest.java`
- Test resource: `src/test/resources/flyway/order/V1__create_order.sql`
- Test resource: `src/test/resources/flyway/order/V2__insert_order.sql`

**Interfaces:**
- Consumes: `ItsukaFlywayProperties`（Task 1）。
- Produces:
  - `@FunctionalInterface interface ItsukaFlywayDataSourceResolver { DataSource resolve(String sourceName); }`（包级私有）。
  - `static ItsukaFlywayDataSourceResolver ItsukaFlywayDataSourceResolver.of(FlywayProperties properties, ObjectProvider<DataSource> dataSource, ObjectProvider<DataSource> flywayDataSource, ObjectProvider<FlywayConnectionDetails> flywayConnectionDetails, ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails)`。
  - `ItsukaFlywayFactory(FlywayProperties, ItsukaFlywayProperties, ResourceLoader, ItsukaFlywayDataSourceResolver, List<FlywayConfigurationCustomizer>, List<JavaMigration>, List<Callback>)`。
  - `Flyway ItsukaFlywayFactory.create(ItsukaFlywayProperties.Source source)`。

- [ ] **Step 1: 写数据源解析失败测试**

需要的 import：`javax.sql.DataSource`、`org.springframework.boot.flyway.autoconfigure.FlywayProperties`、`FlywayConnectionDetails`、`org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails`、`org.springframework.jdbc.datasource.SimpleDriverDataSource`、`org.h2.jdbcx.JdbcDataSource`、`org.springframework.beans.factory.ObjectProvider`、`org.springframework.beans.factory.NoSuchBeanDefinitionException`、`java.util.Collections/Iterator/List`。

```java
class ItsukaFlywayDataSourceResolverTest {

    private final FlywayProperties properties = new FlywayProperties();

    @Test
    void annotatedDataSourceWins() {
        DataSource primary = dataSource("primary");
        DataSource flyway = dataSource("flyway");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), provider(flyway), emptyProvider(), emptyProvider());
        assertThat(resolver.resolve("order")).isSameAs(flyway);
    }

    @Test
    void configuredUrlBuildsSimpleDriverDataSource() {
        properties.setUrl("jdbc:h2:mem:flyway-details");
        properties.setUser("sa");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider());
        DataSource resolved = resolver.resolve("order");
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl()).isEqualTo("jdbc:h2:mem:flyway-details");
    }

    @Test
    void flywayConnectionDetailsBeanWinsOverPrimaryAndConfiguredUrl() {
        properties.setUrl("jdbc:h2:mem:properties-url");
        DataSource primary = dataSource("primary");
        FlywayConnectionDetails details = new FlywayConnectionDetails() {
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
            @Override public String getJdbcUrl() { return "jdbc:h2:mem:details-bean"; }
        };
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), emptyProvider(), provider(details), emptyProvider());
        DataSource resolved = resolver.resolve("order");
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl()).isEqualTo("jdbc:h2:mem:details-bean");
    }

    @Test
    void primaryDataSourceWinsOverJdbcConnectionDetails() {
        DataSource primary = dataSource("primary");
        JdbcConnectionDetails details = new JdbcConnectionDetails() {
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
            @Override public String getJdbcUrl() { return "jdbc:h2:mem:jdbc-details"; }
            @Override public String getDriverClassName() { return "org.h2.Driver"; }
        };
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), emptyProvider(), emptyProvider(), provider(details));
        assertThat(resolver.resolve("order")).isSameAs(primary);
    }

    @Test
    void jdbcConnectionDetailsIsUsedWhenNoPrimaryDataSourceExists() {
        JdbcConnectionDetails details = new JdbcConnectionDetails() {
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
            @Override public String getJdbcUrl() { return "jdbc:h2:mem:jdbc-details"; }
            @Override public String getDriverClassName() { return "org.h2.Driver"; }
        };
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, emptyProvider(), emptyProvider(), emptyProvider(), provider(details));
        DataSource resolved = resolver.resolve("order");
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl()).isEqualTo("jdbc:h2:mem:jdbc-details");
        assertThat(((SimpleDriverDataSource) resolved).getUsername()).isEqualTo("sa");
    }

    @Test
    void derivesFromPrimaryDataSourceUsingConfiguredUser() {
        properties.setUser("flyway-user");
        properties.setPassword("flyway-password");
        DataSource primary = dataSource("primary-derived");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), emptyProvider(), emptyProvider(), emptyProvider());
        DataSource resolved = resolver.resolve("order");
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl())
            .isEqualTo("jdbc:h2:mem:primary-derived;DB_CLOSE_DELAY=-1");
        assertThat(((SimpleDriverDataSource) resolved).getUsername()).isEqualTo("flyway-user");
    }

    @Test
    void primaryDataSourceIsFallback() {
        DataSource primary = dataSource("primary");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), emptyProvider(), emptyProvider(), emptyProvider());
        assertThat(resolver.resolve("order")).isSameAs(primary);
    }

    @Test
    void missingDataSourceFailsWithHint() {
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider());
        assertThatThrownBy(() -> resolver.resolve("order"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("order")
            .hasMessageContaining("DataSource")
            .hasMessageContaining("@FlywayDataSource")
            .hasMessageContaining("FlywayConnectionDetails")
            .hasMessageContaining("JdbcConnectionDetails")
            .hasMessageContaining("spring.flyway.url");
    }

    private static JdbcDataSource dataSource(String name) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override public T getObject() { return value; }
            @Override public T getObject(Object... args) { return value; }
            @Override public Iterator<T> iterator() { return List.of(value).iterator(); }
        };
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override public T getObject() { throw new NoSuchBeanDefinitionException(Object.class); }
            @Override public T getObject(Object... args) { throw new NoSuchBeanDefinitionException(Object.class); }
            @Override public Iterator<T> iterator() { return Collections.emptyIterator(); }
        };
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayDataSourceResolverTest'`
Expected: 编译失败，找不到 `ItsukaFlywayDataSourceResolver`。

- [ ] **Step 3: 实现数据源解析器**

`of(...)` 返回的 lambda 按以下顺序解析：

1. `flywayDataSource.getIfAvailable()`：非空直接返回。
2. `flywayConnectionDetails.getIfAvailable()`：`getJdbcUrl()` 非空时用 `DataSourceBuilder` 构造 `SimpleDriverDataSource` 并返回；`getUsername()` 非空且 `dataSource.getIfUnique()` 非空时从该 DataSource 派生，使用详情的用户名/密码。
3. `spring.flyway.url` 非空：用 `FlywayProperties` 构造独立的 `SimpleDriverDataSource` 并返回。
4. `spring.flyway.user` 非空且 `dataSource.getIfUnique()` 非空：从该 DataSource 派生，使用 `spring.flyway.user/password`。
5. `dataSource.getIfUnique()` 非空：直接返回（唯一或 `@Primary` 的主 DataSource 优先于 `JdbcConnectionDetails`，避免绕过连接池）。
6. `jdbcConnectionDetails.getIfAvailable()` 非空：适配为 `FlywayConnectionDetails`（含 url/username/password/driver-class-name），有 URL 时构造 `SimpleDriverDataSource`。
7. 抛 `IllegalStateException`，消息包含当前 source 名以及全部支持的数据源来源：
   `Flyway 迁移数据源缺失（source: <name>）：请提供一个 DataSource Bean、@FlywayDataSource Bean、FlywayConnectionDetails Bean、JdbcConnectionDetails Bean，或配置 spring.flyway.url。`

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayDataSourceResolverTest'`
Expected: PASS。

- [ ] **Step 5: 写工厂失败测试与测试脚本**

`src/test/resources/flyway/order/V1__create_order.sql`：

```sql
CREATE TABLE flyway_order_test (id BIGINT PRIMARY KEY, name VARCHAR(64));
```

`src/test/resources/flyway/order/V2__insert_order.sql`：

```sql
INSERT INTO flyway_order_test (id, name) VALUES (1, 'order-1');
```

需要的 import：`org.flywaydb.core.Flyway`、`org.flywaydb.core.api.Location`、`org.springframework.boot.flyway.autoconfigure.FlywayProperties`、`FlywayConfigurationCustomizer`、`org.springframework.core.io.DefaultResourceLoader`、`org.h2.jdbcx.JdbcDataSource`。

```java
class ItsukaFlywayFactoryTest {

    @Test
    void mapsCommonPropertiesAndSourceOverrides() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:factory;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        FlywayProperties flywayProperties = new FlywayProperties();
        flywayProperties.setInstalledBy("migration-test");
        flywayProperties.setOutOfOrder(true);
        flywayProperties.setCleanDisabled(false);
        flywayProperties.setValidateOnMigrate(false);
        flywayProperties.setTablespace("test_ts");
        flywayProperties.setPlaceholders(Map.of("app_table", "flyway_placeholder_test"));

        ItsukaFlywayProperties starterProperties = new ItsukaFlywayProperties();
        starterProperties.setFailOnMissingLocations(true);

        ItsukaFlywayFactory factory = new ItsukaFlywayFactory(
            flywayProperties, starterProperties, new DefaultResourceLoader(),
            (sourceName) -> dataSource, List.of(), List.of(), List.of());

        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setLocation("classpath:flyway/order");

        Flyway flyway = factory.create(source);

        assertThat(flyway.getConfiguration().getInstalledBy()).isEqualTo("migration-test");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isTrue();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isFalse();
        assertThat(flyway.getConfiguration().isValidateOnMigrate()).isFalse();
        assertThat(flyway.getConfiguration().getTablespace()).isEqualTo("test_ts");
        assertThat(flyway.getConfiguration().getPlaceholders())
            .containsEntry("app_table", "flyway_placeholder_test");
        assertThat(flyway.getConfiguration().getTable()).isEqualTo("flyway_schema_history_order");
        assertThat(flyway.getConfiguration().getLocations())
            .extracting(Location::getDescriptor)
            .containsExactly("classpath:flyway/order");
        assertThat(flyway.getConfiguration().isFailOnMissingLocations()).isTrue();
    }

    @Test
    void appliesCustomizerBeforeSourceLocationAndTable() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:factory-customizer;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        FlywayConfigurationCustomizer customizer = configuration -> {
            configuration.installedBy("custom");
            configuration.table("ignored_by_source");
            configuration.locations("classpath:ignored");
        };

        ItsukaFlywayFactory factory = new ItsukaFlywayFactory(
            new FlywayProperties(), new ItsukaFlywayProperties(), new DefaultResourceLoader(),
            (sourceName) -> dataSource, List.of(customizer), List.of(), List.of());

        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setLocation("classpath:flyway/order");

        Flyway flyway = factory.create(source);

        assertThat(flyway.getConfiguration().getInstalledBy()).isEqualTo("custom");
        assertThat(flyway.getConfiguration().getTable()).isEqualTo("flyway_schema_history_order");
        assertThat(flyway.getConfiguration().getLocations())
            .extracting(Location::getDescriptor)
            .containsExactly("classpath:flyway/order");
    }
}
```

- [ ] **Step 6: 运行确认失败**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayFactoryTest'`
Expected: 编译失败，找不到 `ItsukaFlywayFactory`。

- [ ] **Step 7: 实现 `ItsukaFlywayFactory`**

构造器保存参数。`create(source)` 按顺序：

1. `FluentConfiguration configuration = new FluentConfiguration(resourceLoader.getClassLoader());`
2. `configuration.dataSource(dataSourceResolver.resolve(source.getName()))`（每次创建 source 时解析）。
3. `configureCommon(configuration)`：完整复制 Boot 4.0.3 `FlywayAutoConfiguration.FlywayConfiguration#configureProperties` 的方法体，删除 `locations(...)` 和 `table(...)` 两处调用。`callbackLocations` 的 `{vendor}` 解析用私有方法实现：
   - 无 `{vendor}` 时原样返回；
   - 否则用 `JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL)` 取 URL，`DatabaseDriver.fromJdbcUrl(url)` 得到 vendor，替换后返回；
   - `MetaDataAccessException` 包装为 `IllegalStateException`。
4. `configureCallbacks(configuration)`：非空时 `configuration.callbacks(callbacks.toArray(Callback[]::new))`。
5. `configureJavaMigrations(configuration)`：非空时 `configuration.javaMigrations(javaMigrations.toArray(JavaMigration[]::new))`。
6. 依次调用 `customizers`。
7. `configuration.locations(source.getLocation());`
8. `configuration.table(source.resolvedTable());`
9. `configuration.failOnMissingLocations(starterProperties.isFailOnMissingLocations());`
10. `return configuration.load();`

- [ ] **Step 8: 运行工厂测试确认通过**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayFactoryTest'`
Expected: PASS。

- [ ] **Step 9: 提交**

```bash
git add common/itsuka-flyway-spring-boot-starter
git commit -m "feat(flyway): add data source resolver and Flyway factory"
```

---

### Task 4: 自动配置、Registrar、Schema 管理与 H2 集成

**Files:**
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayRegistrar.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywaySchemaManagementProvider.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/java/org/itsuka/flyway/ItsukaFlywayAutoConfiguration.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayAutoConfigurationTest.java`
- Test resource: `src/test/resources/flyway/user/V1__create_user.sql`
- Test resource: `src/test/resources/flyway/user/V2__insert_user.sql`
- Test resource: `src/test/resources/db/migration/V1__native.sql`

**Interfaces:**
- Consumes: Task 1-3 的全部类型。
- Produces:
  - `ItsukaFlywayRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware`。
  - `ItsukaFlywaySchemaManagementProvider implements SchemaManagementProvider`，构造器 `(ObjectProvider<Flyway>)`。
  - `ItsukaFlywayAutoConfiguration`，Bean 名 `itsukaFlywayFactory`、`itsukaFlywaySchemaManagementProvider`。
  - 每个 source 的 Bean 名 `flyway_<normalized>`、`flywayInitializer_<normalized>`。

- [ ] **Step 1: 写多 source 失败测试**

```java
class ItsukaFlywayAutoConfigurationTest {

    private JdbcDataSource dataSource;

    private ApplicationContextRunner runner() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ItsukaFlywayAutoConfiguration.class))
            .withBean(DataSource.class, () -> dataSource)
            .withPropertyValues(
                "itsuka.flyway.sources[0].name=order",
                "itsuka.flyway.sources[0].location=classpath:flyway/order",
                "itsuka.flyway.sources[1].name=user",
                "itsuka.flyway.sources[1].location=classpath:flyway/user",
                // 多个 Flyway 实例共享同一 schema 时，后续 source 需要 baseline 才能通过
                // Flyway 的非空 schema 检查；baseline-version=0 保证每个 source 的 V1 仍会执行。
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0");
    }

    @Test
    void migratesEachSourceIntoItsOwnHistoryTable() {
        runner().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasBean("flyway_order");
            assertThat(ctx).hasBean("flyway_user");
            assertThat(ctx).doesNotHaveBean("flyway");
            assertThat(ctx).hasBean("flywayInitializer_order");
            assertThat(ctx).hasBean("flywayInitializer_user");

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_order_test", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_user_test", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history_order where type = 'SQL'", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history_user where type = 'SQL'", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                    + "where lower(table_name) = 'flyway_schema_history'", Integer.class)).isZero();
        });
    }
}
```

测试脚本：

`src/test/resources/flyway/user/V1__create_user.sql`：

```sql
CREATE TABLE flyway_user_test (id BIGINT PRIMARY KEY, name VARCHAR(64));
```

`src/test/resources/flyway/user/V2__insert_user.sql`：

```sql
INSERT INTO flyway_user_test (id, name) VALUES (1, 'user-1');
```

`src/test/resources/db/migration/V1__native.sql`：

```sql
CREATE TABLE flyway_native_test (id BIGINT PRIMARY KEY);
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayAutoConfigurationTest'`
Expected: 编译失败，找不到 `ItsukaFlywayAutoConfiguration`。

- [ ] **Step 3: 实现 Registrar**

签名与关键逻辑：

```java
final class ItsukaFlywayRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private Environment environment;
    private BeanFactory beanFactory;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ItsukaFlywayProperties properties = Binder.get(environment)
            .bind("itsuka.flyway", ItsukaFlywayProperties.class)
            .orElseGet(ItsukaFlywayProperties::new);
        ItsukaFlywayPropertiesValidator.validate(properties);

        String previousInitializer = null;
        for (int i = 0; i < properties.getSources().size(); i++) {
            ItsukaFlywayProperties.Source source = properties.getSources().get(i);
            String normalized = source.normalizedName();

            RootBeanDefinition flywayDefinition = new RootBeanDefinition(Flyway.class);
            flywayDefinition.setInstanceSupplier(() -> {
                ItsukaFlywayFactory factory =
                    beanFactory.getBean("itsukaFlywayFactory", ItsukaFlywayFactory.class);
                return factory.create(source);
            });
            registry.registerBeanDefinition("flyway_" + normalized, flywayDefinition);

            RootBeanDefinition initializerDefinition =
                new RootBeanDefinition(FlywayMigrationInitializer.class);
            initializerDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
                0, new RuntimeBeanReference("flyway_" + normalized));
            initializerDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
                1, migrationStrategy());
            initializerDefinition.getPropertyValues().add("order", i);
            if (previousInitializer != null) {
                initializerDefinition.setDependsOn(previousInitializer);
            }
            String initializerName = "flywayInitializer_" + normalized;
            registry.registerBeanDefinition(initializerName, initializerDefinition);
            previousInitializer = initializerName;
        }
    }

    private FlywayMigrationStrategy migrationStrategy() {
        return flyway -> {
            FlywayMigrationStrategy userStrategy =
                beanFactory.getBeanProvider(FlywayMigrationStrategy.class).getIfAvailable();
            if (userStrategy != null) {
                userStrategy.migrate(flyway);
            }
            else {
                flyway.migrate();
            }
        };
    }
}
```

- [ ] **Step 4: 实现 Schema 管理 Provider**

```java
final class ItsukaFlywaySchemaManagementProvider implements SchemaManagementProvider {

    private final ObjectProvider<Flyway> flyways;

    ItsukaFlywaySchemaManagementProvider(ObjectProvider<Flyway> flyways) {
        this.flyways = flyways;
    }

    @Override
    public SchemaManagement getSchemaManagement(DataSource dataSource) {
        boolean managed = flyways.orderedStream()
            .map(flyway -> flyway.getConfiguration().getDataSource())
            .anyMatch(dataSource::equals);
        return managed ? SchemaManagement.MANAGED : SchemaManagement.UNMANAGED;
    }
}
```

- [ ] **Step 5: 实现自动配置与注册文件**

```java
@AutoConfiguration(before = FlywayAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnMissingBean(Flyway.class)
@ConditionalOnBooleanProperty(prefix = "itsuka.flyway", name = "enabled", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = "itsuka.flyway", name = "native-enabled",
    havingValue = false, matchIfMissing = true)
@EnableConfigurationProperties({ItsukaFlywayProperties.class, FlywayProperties.class})
@Import({ItsukaFlywayRegistrar.class, DatabaseInitializationDependencyConfigurer.class})
public class ItsukaFlywayAutoConfiguration {

    @Bean
    ItsukaFlywayFactory itsukaFlywayFactory(
            FlywayProperties flywayProperties,
            ItsukaFlywayProperties itsukaFlywayProperties,
            ResourceLoader resourceLoader,
            ObjectProvider<DataSource> dataSource,
            @FlywayDataSource ObjectProvider<DataSource> flywayDataSource,
            ObjectProvider<FlywayConnectionDetails> flywayConnectionDetails,
            ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails,
            ObjectProvider<FlywayConfigurationCustomizer> configurationCustomizers,
            ObjectProvider<JavaMigration> javaMigrations,
            ObjectProvider<Callback> callbacks) {
        return new ItsukaFlywayFactory(
            flywayProperties,
            itsukaFlywayProperties,
            resourceLoader,
            ItsukaFlywayDataSourceResolver.of(
                flywayProperties, dataSource, flywayDataSource,
                flywayConnectionDetails, jdbcConnectionDetails),
            configurationCustomizers.orderedStream().toList(),
            javaMigrations.orderedStream().toList(),
            callbacks.orderedStream().toList());
    }

    @Bean
    SchemaManagementProvider itsukaFlywaySchemaManagementProvider(ObjectProvider<Flyway> flyways) {
        return new ItsukaFlywaySchemaManagementProvider(flyways);
    }
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```text
org.itsuka.flyway.ItsukaFlywayAutoConfiguration
```

- [ ] **Step 6: 运行主集成测试**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayAutoConfigurationTest'`
Expected: PASS。

- [ ] **Step 7: 追加 Boot 退让测试**

```java
@Test
void bootNativeFlywayBacksOffWhenSourcesAreConfigured() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ItsukaFlywayAutoConfiguration.class, FlywayAutoConfiguration.class))
        .withBean(DataSource.class, () -> {
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
            return dataSource;
        })
        .withPropertyValues(
            "itsuka.flyway.sources[0].name=order",
            "itsuka.flyway.sources[0].location=classpath:flyway/order")
        .run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasBean("flyway_order");
            assertThat(ctx).doesNotHaveBean("flyway");
            assertThat(ctx).doesNotHaveBean("flywayInitializer");
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from information_schema.tables "
                    + "where lower(table_name) = 'flyway_schema_history'", Integer.class)).isZero();
        });
}
```

- [ ] **Step 8: 追加顺序、策略、Customizer、Schema Provider 测试**

顺序与策略：

```java
@TestConfiguration
static class RecordingStrategyConfiguration {
    @Bean
    RecordingFlywayMigrationStrategy recordingFlywayMigrationStrategy() {
        return new RecordingFlywayMigrationStrategy();
    }
}

static class RecordingFlywayMigrationStrategy implements FlywayMigrationStrategy {
    final List<String> migratedTables = new CopyOnWriteArrayList<>();

    @Override
    public void migrate(Flyway flyway) {
        migratedTables.add(flyway.getConfiguration().getTable());
        flyway.migrate();
    }
}

@Test
void migratesSourcesInConfiguredOrder() {
    runner().withUserConfiguration(RecordingStrategyConfiguration.class)
        .run(ctx -> assertThat(ctx.getBean(RecordingFlywayMigrationStrategy.class).migratedTables)
            .containsExactly("flyway_schema_history_order", "flyway_schema_history_user"));
}
```

Customizer：

```java
@TestConfiguration
static class CustomizerConfiguration {
    @Bean
    FlywayConfigurationCustomizer installedByCustomizer() {
        return configuration -> configuration.installedBy("custom-installed-by");
    }
}

@Test
void appliesCustomizerToEverySource() {
    runner().withUserConfiguration(CustomizerConfiguration.class)
        .run(ctx -> {
            assertThat(ctx.getBean("flyway_order", Flyway.class)
                .getConfiguration().getInstalledBy()).isEqualTo("custom-installed-by");
            assertThat(ctx.getBean("flyway_user", Flyway.class)
                .getConfiguration().getInstalledBy()).isEqualTo("custom-installed-by");
        });
}
```

Schema Provider：

```java
@Test
void reportsManagedForFlywayDataSource() {
    runner().run(ctx -> {
        SchemaManagementProvider provider = ctx.getBean(
            "itsukaFlywaySchemaManagementProvider", SchemaManagementProvider.class);
        assertThat(provider.getSchemaManagement(dataSource)).isEqualTo(SchemaManagement.MANAGED);
        JdbcDataSource other = new JdbcDataSource();
        other.setURL("jdbc:h2:mem:other-provider");
        assertThat(provider.getSchemaManagement(other)).isEqualTo(SchemaManagement.UNMANAGED);
    });
}

@Test
void reportsManagedEvenWhenNativeFlywayIsDisabled() {
    runner().withPropertyValues("spring.flyway.enabled=false").run(ctx -> {
        assertThat(ctx).hasBean("flyway_order");
        SchemaManagementProvider provider = ctx.getBean(
            "itsukaFlywaySchemaManagementProvider", SchemaManagementProvider.class);
        assertThat(provider.getSchemaManagement(dataSource)).isEqualTo(SchemaManagement.MANAGED);
    });
}
```

- [ ] **Step 9: 追加公共配置与 `enabled=false` 测试**

```java
@Test
void appliesCommonSpringFlywayProperties() {
    runner().withPropertyValues(
            "spring.flyway.installed-by=spring-boot-config",
            "spring.flyway.out-of-order=true",
            "spring.flyway.validate-on-migrate=false")
        .run(ctx -> {
            Flyway flyway = ctx.getBean("flyway_order", Flyway.class);
            assertThat(flyway.getConfiguration().getInstalledBy()).isEqualTo("spring-boot-config");
            assertThat(flyway.getConfiguration().isOutOfOrder()).isTrue();
            assertThat(flyway.getConfiguration().isValidateOnMigrate()).isFalse();
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select distinct installed_by from flyway_schema_history_order",
                String.class)).isEqualTo("spring-boot-config");
        });
}

@Test
void disabledStarterRegistersNoFlywayBeans() {
    runner().withPropertyValues("itsuka.flyway.enabled=false")
        .run(ctx -> {
            assertThat(ctx).doesNotHaveBean("flyway_order");
            assertThat(ctx).doesNotHaveBean("flyway_user");
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from information_schema.tables "
                    + "where lower(table_name) = 'flyway_order_test'", Integer.class)).isZero();
        });
}
```

- [ ] **Step 10: 追加原生模式测试**

```java
@Test
void nativeModeUsesBootAutoConfiguration() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ItsukaFlywayAutoConfiguration.class, FlywayAutoConfiguration.class))
        .withBean(DataSource.class, () -> {
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
            return dataSource;
        })
        .withPropertyValues(
            "itsuka.flyway.native-enabled=true",
            "spring.flyway.locations=classpath:db/migration")
        .run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(Flyway.class);
            assertThat(ctx).hasBean("flyway");
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from flyway_native_test", Integer.class)).isEqualTo(0);
        });
}
```

- [ ] **Step 11: 运行本任务全部测试**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayAutoConfigurationTest'`
Expected: PASS。

- [ ] **Step 12: 提交**

```bash
git add common/itsuka-flyway-spring-boot-starter
git commit -m "feat(flyway): add auto-configuration, registrar, and schema management"
```

---

### Task 5: 失败路径、真实启动与 MySQL 验证

**Files:**
- Test resource: `src/test/resources/flyway/fail/V1__fail.sql`
- Modify: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayAutoConfigurationTest.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/ItsukaFlywayEnvironmentPostProcessorIT.java`
- Create: `common/itsuka-flyway-spring-boot-starter/src/test/java/org/itsuka/flyway/MySqlFlywayIT.java`

**Interfaces:**
- Consumes: 所有实现。
- Produces: 无新生产类型；只增加测试与验证。

- [ ] **Step 1: 添加失败脚本**

`src/test/resources/flyway/fail/V1__fail.sql`：

```sql
INSERT INTO flyway_missing_table (id) VALUES (1);
```

- [ ] **Step 2: 写缺失路径、失败停止、表名冲突测试**

```java
@Test
void missingLocationFailsByDefault() {
    runner().withPropertyValues(
            "itsuka.flyway.sources[1].name=missing",
            "itsuka.flyway.sources[1].location=classpath:flyway/missing")
        .run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasStackTraceContaining("flyway/missing");
        });
}

@Test
void missingLocationCanBeIgnored() {
    runner().withPropertyValues(
            "itsuka.flyway.fail-on-missing-locations=false",
            "itsuka.flyway.sources[1].name=missing",
            "itsuka.flyway.sources[1].location=classpath:flyway/missing")
        .run(ctx -> assertThat(ctx).hasNotFailed());
}

@Test
void failureStopsLaterSources() {
    runner().withPropertyValues(
            "itsuka.flyway.sources[0].name=fail",
            "itsuka.flyway.sources[0].location=classpath:flyway/fail",
            "itsuka.flyway.sources[1].name=order",
            "itsuka.flyway.sources[1].location=classpath:flyway/order")
        .run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from information_schema.tables "
                    + "where lower(table_name) = 'flyway_order_test'", Integer.class)).isZero();
        });
}

@Test
void duplicateSourceNamesFailBeforeMigration() {
    runner().withPropertyValues(
            "itsuka.flyway.sources[0].name=order-center",
            "itsuka.flyway.sources[0].location=classpath:flyway/order",
            "itsuka.flyway.sources[1].name=order_center",
            "itsuka.flyway.sources[1].location=classpath:flyway/user")
        .run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasStackTraceContaining("重复");
        });
}
```

- [ ] **Step 3: 运行确认通过**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayAutoConfigurationTest'`
Expected: PASS。

- [ ] **Step 4: 写真实 `SpringApplication` 启动测试**

```java
class ItsukaFlywayEnvironmentPostProcessorIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    private static SpringApplication application() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }

    private static String[] dataSourceArgs() {
        return new String[] {
            "--spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "--spring.datasource.driver-class-name=org.h2.Driver",
            "--spring.datasource.username=sa",
            "--spring.datasource.password="
        };
    }

    @Test
    void noSourcesDisablesNativeFlyway() {
        try (ConfigurableApplicationContext context = application().run(dataSourceArgs())) {
            assertThat(context.getEnvironment().getProperty("spring.flyway.enabled"))
                .isEqualTo("false");
            assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
        }
    }

    @Test
    void nativeModeRunsBootFlyway() {
        String[] args = Stream.concat(
            Arrays.stream(dataSourceArgs()),
            Stream.of(
                "--itsuka.flyway.native-enabled=true",
                "--spring.flyway.locations=classpath:db/migration"))
            .toArray(String[]::new);
        try (ConfigurableApplicationContext context = application().run(args)) {
            assertThat(context.getBeansOfType(Flyway.class)).hasSize(1);
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from flyway_native_test", Integer.class)).isZero();
        }
    }

    @Test
    void nativeModeWorksWhenStarterIsDisabled() {
        String[] args = Stream.concat(
            Arrays.stream(dataSourceArgs()),
            Stream.of(
                "--itsuka.flyway.enabled=false",
                "--itsuka.flyway.native-enabled=true",
                "--spring.flyway.locations=classpath:db/migration"))
            .toArray(String[]::new);
        try (ConfigurableApplicationContext context = application().run(args)) {
            assertThat(context.getBeansOfType(Flyway.class)).hasSize(1);
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from flyway_native_test", Integer.class)).isZero();
        }
    }

    @Test
    void nativeModeWithSourcesFailsFast() {
        String[] args = Stream.concat(
            Arrays.stream(dataSourceArgs()),
            Stream.of(
                "--itsuka.flyway.native-enabled=true",
                "--itsuka.flyway.sources[0].name=order",
                "--itsuka.flyway.sources[0].location=classpath:flyway/order"))
            .toArray(String[]::new);
        assertThatThrownBy(() -> application().run(args))
            .hasStackTraceContaining("二选一");
    }
}
```

- [ ] **Step 5: 运行真实启动测试**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.ItsukaFlywayEnvironmentPostProcessorIT'`
Expected: PASS。

- [ ] **Step 6: 写 MySQL Testcontainers IT**

```java
@Testcontainers(disabledWithoutDocker = true)
class MySqlFlywayIT {

    // Testcontainers 2.0.3 的 MySQLContainer 解析类型不是泛型，直接使用原始类型。
    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Test
    void migratesMultipleSourcesWithDuplicateVersions() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());

        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ItsukaFlywayAutoConfiguration.class))
            .withBean(DataSource.class, () -> dataSource)
            .withPropertyValues(
                "itsuka.flyway.sources[0].name=order",
                "itsuka.flyway.sources[0].location=classpath:flyway/order",
                "itsuka.flyway.sources[1].name=user",
                "itsuka.flyway.sources[1].location=classpath:flyway/user",
                // 多个 Flyway 实例共享同一 schema 时，后续 source 需要 baseline 才能通过
                // Flyway 的非空 schema 检查；baseline-version=0 保证每个 source 的 V1 仍会执行。
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0")
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                assertThat(jdbc.queryForObject(
                    "select count(*) from flyway_order_test", Integer.class)).isEqualTo(1);
                assertThat(jdbc.queryForObject(
                    "select count(*) from flyway_user_test", Integer.class)).isEqualTo(1);
                assertThat(jdbc.queryForObject(
                    "select count(*) from flyway_schema_history_order where type = 'SQL'", Integer.class)).isEqualTo(2);
                assertThat(jdbc.queryForObject(
                    "select count(*) from flyway_schema_history_user where type = 'SQL'", Integer.class)).isEqualTo(2);
            });
    }
}
```

- [ ] **Step 7: 运行 MySQL IT**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test --tests 'org.itsuka.flyway.MySqlFlywayIT'`
Expected: PASS；无 Docker 时 SKIPPED。

- [ ] **Step 8: 全量测试与构建**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:test`
Expected: PASS（MySQL IT 无 Docker 时跳过）。

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:build`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 9: 提交**

```bash
git add common/itsuka-flyway-spring-boot-starter
git commit -m "test(flyway): cover failure paths, startup modes, and MySQL"
```

---

### Task 6: 最终一致性检查

**Files:**
- Review: 全部新增文件与设计文档。

- [ ] **Step 1: 核对自动配置注册文件**

确认：

```text
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容只有一行 `org.itsuka.flyway.ItsukaFlywayAutoConfiguration`。

- [ ] **Step 2: 核对 Bean 名与历史表名**

运行测试并通过后，确认：

- Bean：`flyway_order`、`flyway_user`、`flywayInitializer_order`、`flywayInitializer_user`。
- 历史表：`flyway_schema_history_order`、`flyway_schema_history_user`。
- 不存在默认 `flyway` Bean 和默认 `flyway_schema_history` 表。

- [ ] **Step 3: 核对 `native-enabled` 与 `enabled` 的矩阵**

逐个确认 Task 2 与 Task 5 的测试覆盖：

- `enabled=true/native=false/sources 非空` → 多路径迁移。
- `enabled=true/native=false/sources 空` → 不迁移。
- `enabled=true/native=true/sources 空` → Boot 原生。
- `enabled=true/native=true/sources 非空` → 启动失败。
- `enabled=false/native=false` → 不迁移。
- `enabled=false/native=true/sources 空` → Boot 原生。
- `enabled=false/native=true/sources 非空` → 启动失败。

- [ ] **Step 4: 核对模块构建与发布配置**

Run: `./gradlew :common:itsuka-flyway-spring-boot-starter:build`
Expected: BUILD SUCCESSFUL，且生成的 POM 包含 `spring-boot-starter-flyway`（compile）与 `flyway-mysql`（runtime）。

- [ ] **Step 5: 核对文档、计划与归属声明**

确认：

- 设计文档 §1、§3.1、§3.2、§4.4、§8.2 已记录共享 schema 的 baseline 要求、Boot 4 `EnvironmentPostProcessor` 新接口与注册键、测试的 baseline 属性与 `type = 'SQL'` 统计口径。
- 实现计划 Task 2/4/5 的示例代码与当前实现和测试一致（新 EPP 键、baseline 属性、`where type = 'SQL'`、原始 `MySQLContainer`）。
- `ItsukaFlywayFactory#configureProperties` 的注释声明属性映射改编自 Spring Boot `FlywayAutoConfiguration`，Apache License 2.0；映射内容未改动。

- [ ] **Step 6: 提交最终修正（如有）**

```bash
git add common/itsuka-flyway-spring-boot-starter settings.gradle.kts build.gradle.kts docs/superpowers
git commit -m "chore(flyway): final consistency fixes"
```

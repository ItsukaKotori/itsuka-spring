# itsuka-flyway-spring-boot-starter 设计

- 日期：2026-11-14
- 状态：已确认
- 目标模块：`common/itsuka-flyway-spring-boot-starter`
- 包名：`org.itsuka.flyway`

## 1. 目标

为 itsuka-spring 增加一个 Flyway starter，使接入方可以在同一个应用、同一个数据库上同时加载多个 classpath 路径下的 SQL 迁移脚本：

- 每个路径是一条独立的迁移流，有独立的历史表和版本序列；不同路径可以都从 `V1` 开始。
- 单个路径内部保持 Flyway 原生的版本顺序与校验规则。
- 多个路径之间互不影响：一个路径的版本号、历史表、校验和与另一个路径无关。
- 引入依赖后默认关闭 Spring Boot 原生的 Flyway 自动配置；可通过配置切回原生单路径模式。
- 公共的 `spring.flyway.*` 配置继续对每个路径生效。
- 配置错误、脚本错误、路径缺失都尽快失败，不静默跳过。

成功标准：

1. 在 Spring Boot 4 + H2 的集成测试中，配置两个 classpath 路径，两边都有 `V1__*.sql`，其中一边还有 `V2__*.sql`；启动后两张业务表都存在，两张独立历史表都能查到正确的迁移记录。
2. 默认配置下（不设置 `itsuka.flyway.native-enabled`），容器中不存在 Boot 原生的 `flyway` Bean，也不会创建默认的 `flyway_schema_history` 表。
3. 设置 `itsuka.flyway.native-enabled=true` 且不配置 sources 时，恢复 Boot 原生行为：存在 `flyway` Bean，并按 `spring.flyway.locations` 迁移。
4. `itsuka.flyway.native-enabled=true` 与 `itsuka.flyway.sources` 同时配置时，启动失败并给出明确原因。
5. 自定义 `FlywayMigrationStrategy` Bean 对每个 source 都生效；自定义 `FlywayConfigurationCustomizer` 也对每个 source 生效。
6. `spring.flyway.*` 中的通用配置（占位符、编码、baseline、校验、schema 等）对每个 source 生效。
7. 某个 source 的迁移失败时，应用启动失败；失败前已经完成的迁移保持已提交状态（Flyway 原生行为）。

## 2. 范围

### 2.1 做

- `itsuka.flyway.sources` 列表配置：每个 source 一个 classpath 路径、一个独立历史表。
- 每个 source 一个独立 `Flyway` 实例、一个独立 `FlywayMigrationInitializer`。
- 每个 source 可单独指定历史表名；默认由 source 名派生。
- 复用 `spring.flyway.*` 的通用配置，映射到每个 source。
- 支持 `@FlywayDataSource`、主 `DataSource`、`FlywayConnectionDetails`/`JdbcConnectionDetails`、`spring.flyway.url` 四种数据源选择方式。
- 支持 `Callback`、`JavaMigration`、`FlywayConfigurationCustomizer` Bean，对所有 source 生效。
- 默认关闭 Boot 原生 Flyway 自动配置；`itsuka.flyway.native-enabled=true` 切回原生模式。
- 配置校验、启动失败信息、配置元数据、单元/集成测试。
- 保留 Boot 的 `DatabaseInitializationDependencyConfigurer` 集成，确保 JPA 等组件等待迁移完成。
- 注册自己的 `SchemaManagementProvider`，即使 `spring.flyway.enabled=false`，JPA 仍能判断 schema 由 Flyway 管理。

### 2.2 不做

- 不做路径自动发现；必须显式配置。
- 不做跨 source 的依赖排序；source 之间不保证业务顺序。
- 不做并行迁移；按配置顺序串行执行。
- 不做每个 source 的完整 Flyway 配置覆盖；只允许覆盖历史表名。
- 不做多个 DataSource 的自动路由；所有 source 使用同一套数据源选择结果。
- 不提供 Controller、管理页面、clean/repair 接口。
- 不实现 Boot 原生自动配置的等价物；原生模式直接交给 Boot。
- 不适配 Flyway Teams 的 Oracle/PostgreSQL/SQL Server 扩展属性；需要时通过 `FlywayConfigurationCustomizer` 自行设置。
- 不做脚本内容生成、目录创建、版本号自动修正。

## 3. 对外契约

### 3.1 依赖与启用

模块依赖：

```kotlin
dependencies {
    api(project(":common:itsuka-spring-boot-starter"))
    api("org.springframework.boot:spring-boot-starter-flyway")
}
```

- `spring-boot-starter-flyway` 会带入 `spring-boot-flyway`、`spring-boot-starter-jdbc`、`flyway-core`。
- 自动配置注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 环境后处理器注册文件：`META-INF/spring.factories`
- 默认启用：`itsuka.flyway.enabled=true`，`matchIfMissing=true`。

### 3.2 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo
    username: demo
    password: demo
  flyway:
    baseline-on-migrate: true
    placeholders:
      app_schema: demo

itsuka:
  flyway:
    enabled: true
    native-enabled: false
    fail-on-missing-locations: true
    sources:
      - name: order
        location: classpath:db/migration/order
      - name: user
        location: classpath:db/migration/user
        table: flyway_user_history
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `itsuka.flyway.enabled` | `true` | 多路径 starter 的总开关 |
| `itsuka.flyway.native-enabled` | `false` | 是否切回 Boot 原生单路径 Flyway |
| `itsuka.flyway.fail-on-missing-locations` | `true` | source 路径不存在时是否启动失败 |
| `itsuka.flyway.sources[].name` | 无，必填 | source 名，唯一；用于派生 Bean 名和历史表名 |
| `itsuka.flyway.sources[].location` | 无，必填 | Flyway location，通常是 `classpath:...` |
| `itsuka.flyway.sources[].table` | `flyway_schema_history_<name>` | 该 source 的历史表名 |

约束：

- `name` 必须匹配 `^[A-Za-z][A-Za-z0-9_-]*$`，且在规范化后唯一。
- 规范化规则：`-` 转 `_`；`order-center` 与 `order_center` 视为同一个名字，不能同时出现。
- `location` 不能为空；原样传给 Flyway，因此 `classpath:`、`filesystem:` 都可用。
- `table` 必须匹配 `^[A-Za-z_][A-Za-z0-9_]*$`，长度不超过 64，且在同一应用内唯一（忽略大小写）。
- 未显式配置 `table` 时，默认值为 `flyway_schema_history_` 加上规范化后的 name；派生结果超过 64 个字符时启动失败，并提示显式配置 `table`。
- `sources` 为空时允许启动，但不会执行任何迁移。

### 3.3 模式

| `enabled` | `native-enabled` | `sources` | 行为 |
| --- | --- | --- | --- |
| `true` | `false` | 非空 | 多路径迁移 |
| `true` | `false` | 空 | 不迁移；Boot 原生 Flyway 也被关闭 |
| `true` | `true` | 空 | Boot 原生单路径迁移 |
| `true` | `true` | 非空 | 启动失败，提示二选一 |
| `false` | `false` | 任意 | 不迁移；sources 被忽略 |
| `false` | `true` | 空 | Boot 原生单路径迁移 |
| `false` | `true` | 非空 | 启动失败，提示二选一 |

- `itsuka.flyway.native-enabled=true` 是唯一的“重新开启原生行为”开关。
- 多路径模式下，`spring.flyway.enabled` 不再决定原生自动配置是否运行；starter 会接管 Flyway Bean 的创建。
- 多路径模式下，`spring.flyway.locations` 和 `spring.flyway.table` 被忽略；每个 source 使用自己的 `location` 和 `table`。

### 3.4 Bean 契约

每个 source 注册：

- `Flyway` Bean，名称为 `flyway_<规范化 name>`，例如 `flyway_order`、`flyway_user_center`。
- `FlywayMigrationInitializer` Bean，名称为 `flywayInitializer_<规范化 name>`，`order` 等于 source 在列表中的下标。
- 第 N 个 initializer 通过 `dependsOn` 依赖第 N-1 个 initializer，硬保证按配置顺序串行执行。

其他：

- `ItsukaFlywayProperties` 作为配置属性 Bean 暴露。
- 用户自定义的 `FlywayMigrationStrategy` Bean 会被每个 source 的初始化器使用；没有时等价于 `flyway.migrate()`。
- 用户自定义的 `FlywayConfigurationCustomizer`、`Callback`、`JavaMigration` Bean 会应用到每个 source。
- 用户自定义 `Flyway` Bean 时，starter 的整体自动配置退让（遵循 Boot 的 `@ConditionalOnMissingBean` 惯例）。
- Actuator 的 `/actuator/flyway` 会按 Bean 名列出所有 source 的迁移信息。

示例注入：

```java
@Service
public class OrderMigrationInfo {

    public OrderMigrationInfo(@Qualifier("flyway_order") Flyway flyway) {
        // ...
    }
}
```

### 3.5 数据源选择

所有 source 共用同一个迁移数据源，选择顺序与 Boot 原生行为保持一致：

1. 标注了 `@FlywayDataSource` 的 `DataSource` Bean。
2. `FlywayConnectionDetails` Bean；如果只有 `JdbcConnectionDetails` Bean，则适配其 url/username/password/driver。
3. `spring.flyway.url/user/password/driver-class-name`：有 URL 时构造独立的 `SimpleDriverDataSource`；只有 username/password 时基于主 DataSource 派生。
4. 唯一或 `@Primary` 的 `DataSource` Bean。

选择不到数据源时启动失败，错误信息包含当前 source 列表和常见原因（没有 DataSource、没有配置 `spring.flyway.url`）。

### 3.6 执行语义

- source 之间按配置列表顺序串行执行；该顺序由 Registrar 为 initializer 建立 `dependsOn` 链硬保证，而不是只依赖 Bean 注册顺序。
- 单个 source 内由 Flyway 决定版本顺序；versioned migration 先于 repeatable migration。
- 不同 source 的版本号、校验和、历史表完全独立。
- 任意 source 迁移失败，异常向上抛出，应用启动失败；后续 source 不再执行。
- 路径不存在且 `fail-on-missing-locations=true` 时，source 的 `Flyway` 创建/迁移阶段失败。
- 历史表由 Flyway 自动创建；starter 不执行额外 DDL。

## 4. 与 Spring Boot 自动配置的协作

### 4.1 多路径模式（默认）

- `ItsukaFlywayAutoConfiguration` 标注 `@AutoConfiguration(before = FlywayAutoConfiguration.class)`。
- 自动配置在解析阶段通过 `ItsukaFlywayRegistrar` 注册每个 source 的 `Flyway` Bean 定义。
- Boot 的 `FlywayAutoConfiguration.FlywayConfiguration` 类上有 `@ConditionalOnMissingBean(Flyway.class)`；看到 starter 注册的 `Flyway` Bean 后，Boot 的默认单实例配置整体退让：
  - 不创建默认 `flyway` Bean；
  - 不创建默认 `FlywayMigrationInitializer`；
  - 不迁移 `spring.flyway.locations` 默认路径。
- 默认情况下（`spring.flyway.enabled` 未被显式设为 false），Boot 的外层 `FlywayAutoConfiguration` 仍然生效，因此：
  - `FlywaySchemaManagementProvider` 存在，并把 starter 的多个 `Flyway` Bean 都视为 schema 管理来源；
  - `DatabaseInitializationDependencyConfigurer` 仍然导入，JPA 等组件会等待 `FlywayMigrationInitializer` 完成；
  - Actuator 的 Flyway 端点正常可用。
- 为覆盖用户显式设置 `spring.flyway.enabled=false` 的情况，starter 自己再导入一次 `DatabaseInitializationDependencyConfigurer`；该类内部按 Bean 名去重，不会重复注册。
- starter 自己注册一个 `ItsukaFlywaySchemaManagementProvider`：只要某个 `Flyway` Bean 使用当前 `DataSource`，就返回 `MANAGED`。Boot 的 `HibernateDefaultDdlAutoProvider` 会遍历所有 `SchemaManagementProvider`，因此与 Boot 原有 provider 共存不会冲突；这保证 `spring.flyway.enabled=false` 时 JPA 也不会误判为 `create-drop`。

### 4.2 没有 sources 的默认情况

- `itsuka.flyway.enabled=true`、`native-enabled=false`、`sources` 为空时，没有任何 `Flyway` Bean 来触发 Boot 的退让。
- `ItsukaFlywayEnvironmentPostProcessor` 在配置数据加载完成后，向 Environment 添加高优先级属性 `spring.flyway.enabled=false`，从根源上关闭 Boot 原生 Flyway。
- 这一行为保证“引入依赖即关闭原生行为”，即使没有配置任何 source。

### 4.3 原生模式

- `itsuka.flyway.native-enabled=true` 时，`ItsukaFlywayAutoConfiguration` 整体退让，不注册任何 source Bean。
- 后处理器不修改 `spring.flyway.enabled`，Boot 原生自动配置按原样运行。
- 如果同时配置了 `sources`，后处理器在启动早期抛出 `IllegalStateException`，提示：

  ```text
  itsuka.flyway.native-enabled=true 与 itsuka.flyway.sources 不能同时使用；请二选一。
  ```

### 4.4 环境后处理器

- 实现 `EnvironmentPostProcessor` 和 `Ordered`，`getOrder()` 返回 `Ordered.LOWEST_PRECEDENCE`，确保在 `ConfigDataEnvironmentPostProcessor` 之后执行。
- 使用 `Binder` 绑定 `itsuka.flyway`，支持 relaxed binding。
- 伪代码：

  ```java
  ItsukaFlywayProperties props = binder.bind("itsuka.flyway", ItsukaFlywayProperties.class)
      .orElseGet(ItsukaFlywayProperties::new);

  boolean multiSourceWillRun = props.isEnabled()
      && !props.isNativeEnabled()
      && !props.getSources().isEmpty();

  if (props.isNativeEnabled() && !props.getSources().isEmpty()) {
      throw new IllegalStateException("itsuka.flyway.native-enabled=true 与 itsuka.flyway.sources 不能同时使用；请二选一。");
  }
  if (props.isNativeEnabled()) {
      return;
  }
  if (multiSourceWillRun) {
      return;
  }
  environment.getPropertySources().addFirst(
      new MapPropertySource("itsukaFlywayDefaults",
          Map.of("spring.flyway.enabled", "false")));
  ```

## 5. 组件与包结构

```text
org.itsuka.flyway
├── ItsukaFlywayAutoConfiguration.java
├── ItsukaFlywayEnvironmentPostProcessor.java
├── ItsukaFlywayProperties.java
├── ItsukaFlywayPropertiesValidator.java
├── ItsukaFlywayRegistrar.java
├── ItsukaFlywayFactory.java
├── ItsukaFlywayDataSourceResolver.java
└── ItsukaFlywaySchemaManagementProvider.java
```

- `ItsukaFlywayProperties`：配置属性，包含 `enabled`、`nativeEnabled`、`failOnMissingLocations` 和 `sources`。
- `ItsukaFlywayPropertiesValidator`：集中校验 name/location/table 的格式、唯一性和派生表名长度。
- `ItsukaFlywayEnvironmentPostProcessor`：负责原生开关与模式冲突的早期处理。
- `ItsukaFlywayRegistrar`：从 Environment 绑定属性，校验后动态注册每个 source 的 `Flyway` 和 `FlywayMigrationInitializer` Bean 定义，并建立 initializer 的 `dependsOn` 链。
- `ItsukaFlywayFactory`：持有 `FlywayProperties`、数据源解析器、`ResourceLoader`、各类扩展 Bean；负责为单个 source 构建 `FluentConfiguration` 并返回 `Flyway`。
- `ItsukaFlywayDataSourceResolver`：按 3.5 的顺序解析迁移数据源。
- `ItsukaFlywaySchemaManagementProvider`：把 starter 管理的 DataSource 标记为 `MANAGED`，供 JPA 判断 `ddl-auto`。
- `ItsukaFlywayAutoConfiguration`：条件、属性绑定、工厂 Bean、`SchemaManagementProvider` Bean、`DatabaseInitializationDependencyConfigurer` 导入。

## 6. 属性映射

`ItsukaFlywayFactory` 为每个 source 新建一个 `FluentConfiguration`，按以下顺序配置：

1. `dataSource(...)`：按 3.5 的规则选择。
2. 通用属性：**完整复制** Boot 4.0.3 `FlywayProperties` 的 `configureProperties` 映射，只跳过 `locations` 和 `table`；`fail-on-missing-locations` 先按 Boot 映射，随后由 step 6 覆盖。这样不会出现“配置了但静默不生效”的属性。
3. `callbacks(...)`、`javaMigrations(...)`。
4. 用户 `FlywayConfigurationCustomizer`。
5. source 的 `locations(location)` 和 `table(table)`，确保 source 隔离不被前面的 customizer 破坏。
6. `failOnMissingLocations`：使用 `itsuka.flyway.fail-on-missing-locations` 的值。

完整映射包括但不限于：

- `encoding`、`connect-retries`、`connect-retries-interval`、`lock-retry-count`
- `default-schema`、`schemas`、`create-schemas`、`tablespace`
- `baseline-description`、`baseline-version`、`baseline-on-migrate`
- `installed-by`、`placeholders`、`placeholder-prefix`、`placeholder-suffix`、`placeholder-separator`、`placeholder-replacement`
- `sql-migration-prefix`、`sql-migration-suffixes`、`sql-migration-separator`、`repeatable-sql-migration-prefix`
- `target`、`clean-disabled`、`group`、`mixed`、`out-of-order`
- `skip-default-callbacks`、`skip-default-resolvers`、`validate-migration-naming`、`validate-on-migrate`
- `script-placeholder-prefix`、`script-placeholder-suffix`、`powershell-executable`
- `init-sqls`、`execute-in-transaction`
- `batch`、`stream`、`output-query-results`、`skip-executing-migrations`、`detect-encoding`
- `ignore-migration-patterns`、`jdbc-properties`、`loggers`、`community-db-support-enabled`
- `callback-locations`、`dry-run-output`、`error-overrides`、`kerberos-config-file`

明确不映射：

- `spring.flyway.locations`
- `spring.flyway.table`
- `spring.flyway.enabled`
- `spring.flyway.oracle.*`、`spring.flyway.postgresql.*`、`spring.flyway.sqlserver.*`

## 7. 构建改动

1. `settings.gradle.kts` 增加：
   ```kotlin
   include("common:itsuka-flyway-spring-boot-starter")
   ```
2. 根 `build.gradle.kts` 的 `moduleDescriptions` 增加：
   ```kotlin
   "itsuka-flyway-spring-boot-starter" to "Flyway 多路径迁移：多个 classpath 路径独立版本流与历史表"
   ```
3. 新模块 `build.gradle.kts`：
   ```kotlin
   dependencies {
       api(project(":common:itsuka-spring-boot-starter"))
       api("org.springframework.boot:spring-boot-starter-flyway")

       // Flyway 11 的 MySQL 支持在独立模块中；放入 runtime 让 MySQL 项目开箱即用。
       // PostgreSQL 等其他数据库由接入方自行添加对应的 Flyway 数据库模块。
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
4. 新模块自动进入现有发布配置（模块名以 `itsuka-` 开头）。
5. 不改动其他模块。

## 8. 测试

### 8.1 单元测试

- `ItsukaFlywayPropertiesTest`
  - 默认值。
  - `resolvedTable()` 的默认派生与显式覆盖。
  - name/table 格式校验。
  - name/table 规范化后重复校验。
  - 派生表名超过 64 字符时失败。
- `ItsukaFlywayPropertiesValidatorTest`
  - 空 name/location。
  - 非法字符。
  - 重复 name（含 `-`/`_` 等价情况）。
- `ItsukaFlywayEnvironmentPostProcessorTest`
  - 无 sources、非原生模式 → 添加 `spring.flyway.enabled=false`。
  - 有 sources、多路径模式 → 不添加。
  - 原生模式 → 不添加。
  - 原生模式 + sources → 抛出异常（无论 `enabled` 是 true 还是 false）。
  - `enabled=false`、非原生模式 → 添加。
- `ItsukaFlywayFactoryTest`
  - 使用 `FluentConfiguration` + H2，验证公共属性映射（占位符、baseline、encoding）和 source 的 location/table 覆盖顺序。
  - 数据源选择顺序（`@FlywayDataSource`、`FlywayConnectionDetails`、主 DataSource、`spring.flyway.url`）。
  - 少见的公共属性（`installed-by`、`out-of-order`、`clean-disabled`、`validate-on-migrate`、`tablespace` 等）能映射到 `Flyway.getConfiguration()`。

### 8.2 集成测试

- `ItsukaFlywayAutoConfigurationTest`（`ApplicationContextRunner` + H2）
  - 两个 source 都从 `V1` 开始，各自迁移成功；两张业务表存在；两张历史表内容正确。
  - 单 source 内 `V1` 建表、`V2` 插入；验证顺序。
  - 默认多路径模式下没有 Boot 原生 `flyway` Bean，也没有 `flyway_schema_history` 表。
  - `itsuka.flyway.enabled=false` → 没有 source Flyway Bean。
  - `native-enabled=true`、无 sources → Boot 原生 `flyway` Bean 存在并迁移。
  - 自定义 `FlywayMigrationStrategy` 对每个 source 都被调用，且调用顺序与 source 配置顺序一致。
  - 自定义 `FlywayConfigurationCustomizer` 对每个 source 生效。
  - `spring.flyway.placeholders`、`spring.flyway.baseline-on-migrate` 等公共配置生效。
  - 某个 source 路径不存在 → 启动失败（默认 `fail-on-missing-locations=true`）。
  - 某个 source 的 SQL 失败 → 启动失败，后续 source 不执行。
  - 派生表名冲突 → 启动失败。
  - `ItsukaFlywaySchemaManagementProvider` 对每个 source 使用的 DataSource 返回 `MANAGED`。
  - 用户显式设置 `spring.flyway.enabled=false` 时，source 仍然迁移，且 schema 仍被标记为 `MANAGED`。
- `ItsukaFlywayEnvironmentPostProcessorIT`（`SpringApplication`）
  - 不配置 sources 时，`spring.flyway.enabled` 被设为 false，Boot 原生迁移不执行。
  - 配置 `native-enabled=true` 时，Boot 原生迁移执行。
  - 原生模式 + sources → 启动失败，错误信息包含“二选一”。

### 8.3 MySQL 方言测试

- `MySqlFlywayIT`（Testcontainers MySQL 8）
  - 两个 source 各带重复 `V1`，迁移成功后两张历史表都存在。
  - 无 Docker 时通过 `@Testcontainers(disabledWithoutDocker = true)` 跳过。

### 8.4 验证命令

```bash
./gradlew :common:itsuka-flyway-spring-boot-starter:test
./gradlew :common:itsuka-flyway-spring-boot-starter:build
```

## 9. 已知限制

- 所有 source 共享同一个迁移数据源；需要不同数据库时，拆成多个应用或手工配置多个 Flyway。
- 不支持路径自动发现；新增路径必须改配置。
- source 串行顺序是确定的，但不承诺跨 source 的业务依赖；不要在 source B 的脚本中依赖 source A 刚创建的表。
- 只支持每个 source 一个 location；同一版本流拆到多个目录时，请合并目录或在 Flyway 中手工配置。
- 原生模式和多路径模式不能同时使用。
- 不映射 Oracle/PostgreSQL/SQL Server 的扩展属性。
- 通过 `runtimeOnly` 自带 `flyway-mysql`；PostgreSQL、Oracle 等数据库需要接入方自行添加对应的 Flyway 数据库模块。
- 不支持 `clean`、`repair` 的 HTTP/管理端点；需要时通过注入对应 `Flyway` Bean 自行调用。
- 迁移串行执行，启动时间随 source 数量线性增加。
- 历史表名派生后超过 64 字符会失败，需要显式配置 `table`。
- Flyway 版本由 Spring Boot 4.0.3 管理（当前为 11.14.1）；升级 Boot 时需回归测试公共属性映射。

## 10. 我的决定（可推翻）

1. source 用显式 `name` + `location` 配置，不做自动发现。
2. 每个 source 一条独立版本流、一个独立历史表；历史表默认 `flyway_schema_history_<name>`。
3. source 之间按配置顺序串行执行，不并行；通过 initializer 的 `dependsOn` 链硬保证，不只依赖 Bean 注册顺序。
4. `fail-on-missing-locations` 默认 `true`，避免路径写错时静默无迁移。
5. 原生模式与多路径模式互斥；只要 `native-enabled=true` 且 `sources` 非空就启动失败，与 `enabled` 无关。
6. `itsuka.flyway.native-enabled` 是唯一重新开启原生行为的开关；`spring.flyway.enabled` 在多路径模式下不参与决策。
7. 复用 `spring.flyway.*` 通用配置，完整复制 Boot 4.0.3 的 `FlywayProperties` 映射（除 `locations`、`table`）；扩展属性不映射。
8. 使用 Boot 的 `FlywayMigrationInitializer`，保留 `DatabaseInitializerDetector` 对 JPA 等组件的等待能力。
9. 每个 source 暴露一个 `Flyway` Bean，名称 `flyway_<name>`，方便注入和 Actuator 展示。
10. 用户自定义 `Flyway` Bean 时，整个 starter 自动配置退让。
11. 模块通过 `spring-boot-starter-flyway` 引入 Flyway 和 JDBC，并通过 `runtimeOnly` 带入 `flyway-mysql`；其他数据库模块由接入方添加。
12. 多路径模式下不修改 `spring.flyway.enabled`（有 source 时靠 Flyway Bean 触发 Boot 退让）；只有“无 source 且非原生模式”时才强制设为 false。
13. 数据源选择顺序与 Boot 原生保持一致，支持 `@FlywayDataSource`、`FlywayConnectionDetails` 和 `spring.flyway.url`。
14. 不引入自定义的 `@ItsukaFlywaySource` 注解；配置即契约。
15. starter 自己注册 `SchemaManagementProvider`，保证 `spring.flyway.enabled=false` 时 JPA 仍把 Flyway 管理的 DataSource 视为 `MANAGED`。
16. 完整复制 Boot 4.0.3 的属性映射，避免少见属性静默失效；升级 Boot 时以 Boot 的 `configureProperties` 为基准回归。

## 11. 后续可做

- 每个 source 的完整 Flyway 配置覆盖。
- 并行迁移与依赖 DAG。
- 路径自动发现（扫描固定前缀下的子目录）。
- 多数据源路由。
- 迁移进度/耗时指标。
- 与 log starter 的建表脚本统一管理。
- Oracle/PostgreSQL/SQL Server 扩展属性映射。

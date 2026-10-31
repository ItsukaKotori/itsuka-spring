# itsuka-log-spring-boot-starter 设计

- 日期：2026-10-31
- 状态：待评审
- 参考：[如何优雅地记录操作日志？](https://tech.meituan.com/2021/09/16/operational-logbook.html)

## 1. 目标

为 itsuka-spring 增加一个通用操作日志 starter：

- 接入方加依赖、配置数据源，在业务方法上加 `@LogRecord` 即可记录操作日志。
- 默认存储到 MySQL；启动时自动建表，不依赖 Flyway。
- 内置按业务标识分页查询操作历史的能力。
- 操作人、存储、查询、模板函数都可以用 Bean 覆盖或配置调整。
- 日志组件自身的任何异常都不影响业务方法的返回值和异常。

成功标准：

1. 在 Spring Boot 4 + MySQL 的集成测试（或示例应用）中，只加依赖和配置，启动后表自动出现。
2. 调用带 `@LogRecord` 的方法后，能通过 `OperationLogQueryService` 按 `bizNo` 分页查到记录。
3. 业务方法抛异常时记录失败日志，异常原样抛给调用方。
4. 把 `LogRecordStore` 换成自定义 Bean 后，默认 MySQL 实现自动失效。
5. 关闭 `itsuka.log.enabled` 后，不注册切面、存储、查询等任何 Bean。

## 2. 范围

### 2.1 做

- `@LogRecord` 注解 + Spring AOP 拦截。
- SpEL 动态文案、`LogRecordContext`、自定义函数。
- 默认 MySQL 存储（JdbcTemplate），自动建表。
- 可选的 SLF4J 存储（不落库，只打独立 logger）。
- 可选的异步写入。
- 内置分页查询 + 按 id 查询。
- 操作人获取 SPI、存储 SPI、查询 SPI、自定义函数 SPI。
- 请求 IP、User-Agent、参数快照（默认关闭参数快照）。
- 自动配置、配置元数据、单元/集成测试。

### 2.2 不做

- 不做前端页面、Controller、权限校验。
- 不做日志清理、归档、保留天数。
- 不做多租户。
- 不做 MySQL 之外的方言。
- 不做 Flyway/Liquibase 集成。
- 不做对象 diff（`{DIFF{...}}`）、`@DiffLogIgnore` 等扩展。
- 不记录方法返回值全文，除非调用方自己把返回值放进模板。
- 不做跨线程上下文传递（不引入 TTL）。

## 3. 对外契约

### 3.1 模块与启用

- 模块：`common/itsuka-log-spring-boot-starter`
- 包名：`org.itsuka.log`
- 自动配置：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 默认启用：`itsuka.log.enabled=true`（`matchIfMissing=true`）
- 依赖：`spring-boot-starter-aspectj`、`spring-boot-starter-jdbc`、`itsuka-spring-boot-starter`
- 可选编译依赖：`spring-boot-starter-webmvc`（请求信息）、`spring-security-core`（操作人）、`spring-boot-starter-jackson`（参数序列化）
- MySQL 驱动由接入方提供；测试用 H2/Testcontainers。

### 3.2 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRecord {

    /** 成功时的日志文案模板，必填 */
    String success();

    /** 失败时的日志文案模板；为空时回退到 success，并另外记录 errorMsg */
    String fail() default "";

    /** 操作人 SpEL 模板；为空时使用 LogOperatorProvider */
    String operator() default "";

    /** 业务对象标识 SpEL 模板，必填，查询主键 */
    String bizNo();

    /** 业务类型，例如 ORDER，静态值 */
    String type() default "";

    /** 业务子类型/分类，例如 CUSTOMER、MANAGER，静态值 */
    String category() default "";

    /** 扩展详情 SpEL 模板，存 detail 字段 */
    String detail() default "";

    /** 记录条件，SpEL 布尔表达式；false 时不记录 */
    String condition() default "";
}
```

示例：

```java
@LogRecord(
    success = "{{#request.userName}}修改了订单配送地址：从「{{#oldAddress}}」改为「{{#request.address}}」",
    fail = "修改订单配送地址失败：{{#_errorMsg}}",
    bizNo = "{{#request.orderNo}}",
    type = "ORDER",
    category = "CUSTOMER",
    detail = "{{#request.toString()}}"
)
public void updateAddress(UpdateAddressRequest request) {
    LogRecordContext.putVariable("oldAddress", orderService.queryAddress(request.getOrderNo()));
    orderService.updateAddress(request);
}
```

### 3.3 模板语法

与参考实现 mzt-biz-log 一致：

- 普通 SpEL：`{{#表达式}}`
  - 方法参数：`{{#request.address}}`
  - 上下文变量：`{{#oldAddress}}`
  - 返回值：`{{#_ret}}`
  - 异常信息：`{{#_errorMsg}}`
- 自定义函数：`{FUNC{{#表达式}}}`
  - 函数名大小写敏感，建议全大写。
  - 函数参数是 SpEL 求值后的对象，不再强制转 String。
- 不匹配上述模式的 `{`、`}` 原样保留。
- 表达式内部不支持嵌套 `}`；复杂表达式请放进上下文变量或自定义函数。

`condition` 是原始 SpEL（可写成 `#request.amount > 0`，也接受外层 `{{ }}`），不使用函数语法。

`type`、`category` 是静态字符串，不做模板解析。

### 3.4 上下文

```java
public final class LogRecordContext {

    public static void putVariable(String name, Object value);

    public static Map<String, Object> getVariables();

    public static void pushSpan();       // 框架内部使用

    public static void popSpan();        // 框架内部使用

    public static void clear();          // 框架内部使用
}
```

- 每个带 `@LogRecord` 的方法执行前压入一个空 Map，执行后弹出。
- 嵌套调用互不覆盖；业务方法通过 `putVariable` 放入的值只在当前方法内可见。
- 底层使用 `InheritableThreadLocal<Deque<Map<String, Object>>>`。
- 必须在被 `@LogRecord` 标注的方法内部调用 `putVariable`；否则变量没有生命周期归属。

### 3.5 自定义函数

```java
public interface LogRecordFunction {

    /** 模板中使用的函数名 */
    String functionName();

    /** true：在业务方法执行前求值；false：在方法执行后求值 */
    default boolean executeBefore() {
        return false;
    }

    /** value 是 SpEL 求值结果 */
    String apply(Object value);
}
```

- 接入方把实现类注册为 Spring Bean 即可，starter 自动收集。
- `executeBefore=true` 的函数不能引用 `_ret`、`_errorMsg`；引用了会跳过。
- 方法执行前会缓存 before 函数的返回值，方法执行后直接用缓存，保证能拿到修改前的旧值。
- 函数名重复时启动失败，并指出重复的函数名。
- 模板引用了不存在的函数时，原样返回 SpEL 结果并打 WARN，不影响业务。

### 3.6 操作人

```java
public record LogOperator(String id, String name) {}

public interface LogOperatorProvider {
    LogOperator getOperator();
}
```

默认实现：

1. 存在 Spring Security 且 `SecurityContextHolder` 有已认证用户：
   - principal 是 `LoginUser`：id 取 `String.valueOf(loginUser.getId())`，name 取 `nickname`，为空取 `username`。
   - 其他 principal：id/name 都取 `authentication.getName()`。
2. 没有认证信息：使用配置 `itsuka.log.operator.default-id` / `default-name`。
3. 整个过程不抛异常；拿不到就返回默认值。

注解 `operator` 非空时：

- SpEL 结果是 `LogOperator`：完全采用结果的 id/name。
- 其他类型：作为 operatorId；operatorName 只在 id 与当前登录用户一致时取登录用户名，否则留空。

### 3.7 存储 SPI

```java
public interface LogRecordStore {
    void save(OperationLog log);
}
```

内置实现：

- `JdbcLogRecordStore`：默认，写 MySQL。
- `Slf4jLogRecordStore`：写 logger `itsuka.operation.log`，不落库。
- `AsyncLogRecordStore`：装饰器，有界线程池异步写入。

接入方声明自己的 `LogRecordStore` Bean 后：

- 内置的 `JdbcLogRecordStore` / `Slf4jLogRecordStore` 失效。
- 默认的建表初始化器、`JdbcOperationLogQueryService` 一起失效：自定义存储不一定使用本 starter 的表，不能替接入方执行 DDL，也不能假设查询能命中。
- `AsyncLogRecordStore` 不再包装自定义存储；异步策略由接入方自己决定。
- 如果接入方仍想使用内置查询，可以自己声明 `OperationLogQueryService` Bean（例如 `new JdbcOperationLogQueryService(...)`），starter 不会阻止。

### 3.8 查询

```java
public interface OperationLogQueryService {

    OperationLogPage page(OperationLogQuery query);

    Optional<OperationLog> findById(Long id);
}
```

```java
@Data
public class OperationLogQuery {
    private String type;
    private String category;
    private String bizNo;
    private String operatorId;
    private Boolean success;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long current;
    private Long size;
}

@Data
public class OperationLogPage {
    private List<OperationLog> records;
    private long current;
    private long size;
    private long total;
    private long pages;
}
```

- 默认实现 `JdbcOperationLogQueryService`，按 `created_at DESC, id DESC` 排序。
- 所有过滤条件可选；`bizNo` 是常用条件但不是必填。
- `current` 为 null 或小于 1 时按 1 处理。
- `size` 为 null 时使用 `query.default-page-size`；超过 `query.max-page-size` 时截断到上限。
- 这样 `query.default-page-size` 是有效配置，而不是被 DTO 的字段初始值覆盖。
- 只提供 Service，不提供 Controller。
- `storage=slf4j` 时默认查询 Bean 不注册；接入方可以提供自定义查询 Bean。

## 4. 数据模型

表名默认 `itsuka_operation_log`，可通过 `itsuka.log.table-name` 修改。列名固定。

```sql
CREATE TABLE IF NOT EXISTS `${tableName}` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`           VARCHAR(64)  DEFAULT NULL COMMENT '业务类型',
    `category`       VARCHAR(64)  DEFAULT NULL COMMENT '业务子类型',
    `biz_no`         VARCHAR(128) NOT NULL COMMENT '业务对象标识',
    `success`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否成功',
    `content`        VARCHAR(2000) NOT NULL COMMENT '操作内容',
    `detail`         TEXT         COMMENT '扩展详情',
    `operator_id`    VARCHAR(64)  DEFAULT NULL COMMENT '操作人ID',
    `operator_name`  VARCHAR(128) DEFAULT NULL COMMENT '操作人名称',
    `error_msg`      VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
    `method`         VARCHAR(255) DEFAULT NULL COMMENT '执行方法',
    `request_ip`     VARCHAR(64)  DEFAULT NULL COMMENT '请求IP',
    `user_agent`     VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    `request_params` TEXT         COMMENT '请求参数快照',
    `created_at`     DATETIME(3)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_biz_no_created_at` (`biz_no`, `created_at`),
    KEY `idx_type_category_created_at` (`type`, `category`, `created_at`),
    KEY `idx_operator_created_at` (`operator_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

字段说明：

- `content`：成功/失败模板渲染后的文本，超长截断并追加 `...`。
- `detail`：`detail` 模板渲染结果，超长截断。
- `error_msg`：异常 message，超长截断。
- `method`：`类名#方法名`。
- `request_params`：参数 JSON 或 `toString`，默认不记录。
- `created_at`：应用侧 `LocalDateTime.now()` 赋值，避免数据库时区差异。

截断规则：

- 所有字符串字段写入前都按列宽截断，省略号计入长度；截断本身不抛异常。
- `content` / `detail` / `error_msg` / `request_params` 使用配置的长度上限。
- 其余字段使用列宽：`bizNo` 128、`type`/`category` 64、`operatorId` 64、`operatorName` 128、`method` 255、`requestIp` 64、`userAgent` 512。
- 这样即使 UA、bizNo、操作人名称异常长，也不会因为超列宽导致整条日志丢失。

## 5. 配置

前缀 `itsuka.log`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `storage` | `jdbc` | `jdbc` / `slf4j` |
| `table-name` | `itsuka_operation_log` | 表名，只允许字母、数字、下划线 |
| `auto-init-schema` | `true` | 启动时执行建表脚本 |
| `schema-location` | `classpath:META-INF/itsuka/log/schema-mysql.sql` | 建表脚本，支持 `${tableName}` 占位符 |
| `content-max-length` | `2000` | content 截断长度 |
| `detail-max-length` | `4000` | detail 截断长度 |
| `error-max-length` | `2000` | errorMsg 截断长度 |
| `aspect-order` | `2147483646` | 切面顺序，默认在事务切面外层 |
| `operator.default-id` | `system` | 取不到登录人时的操作人 ID |
| `operator.default-name` | `系统` | 取不到登录人时的操作人名称 |
| `request.enabled` | `true` | 是否记录 IP/UA |
| `request.trust-forwarded-headers` | `false` | 是否信任 `X-Forwarded-For` / `X-Real-IP` |
| `request.params-enabled` | `false` | 是否记录参数快照 |
| `request.params-max-length` | `4000` | 参数快照截断长度 |
| `async.enabled` | `false` | 是否异步写入 |
| `async.core-pool-size` | `1` | 核心线程数 |
| `async.max-pool-size` | `4` | 最大线程数 |
| `async.queue-capacity` | `1024` | 队列容量 |
| `async.keep-alive` | `60s` | 空闲线程存活时间 |
| `async.thread-name-prefix` | `itsuka-log-` | 线程名前缀 |
| `async.rejection-policy` | `caller-runs` | `caller-runs` / `abort` / `discard` / `discard-oldest` |
| `async.await-termination` | `5s` | 关闭时等待任务完成时间 |
| `transaction.requires-new` | `true` | JDBC 写入是否使用独立事务 |
| `query.enabled` | `true` | 是否注册默认查询服务 |
| `query.default-page-size` | `20` | 默认每页大小 |
| `query.max-page-size` | `200` | 最大每页大小 |

示例：

```yaml
itsuka:
  log:
    enabled: true
    storage: jdbc
    table-name: itsuka_operation_log
    auto-init-schema: true
    async:
      enabled: true
      max-pool-size: 8
    request:
      params-enabled: true
    query:
      max-page-size: 100
```

## 6. 组件与包结构

```text
org.itsuka.log
├── annotation/LogRecord.java
├── context/LogRecordContext.java
├── core/
│   ├── LogRecordAspect.java
│   ├── LogRecordOperation.java
│   ├── LogRecordOperationParser.java
│   ├── LogRecordExpressionEvaluator.java
│   ├── LogRecordValueParser.java
│   └── MethodExecuteResult.java
├── domain/
│   ├── OperationLog.java
│   ├── OperationLogQuery.java
│   ├── OperationLogPage.java
│   └── LogOperator.java
├── function/
│   ├── LogRecordFunction.java
│   ├── LogRecordFunctionFactory.java
│   └── LogRecordFunctionParser.java
├── properties/LogRecordProperties.java
├── query/
│   ├── OperationLogQueryService.java
│   └── JdbcOperationLogQueryService.java
├── spi/
│   ├── LogOperatorProvider.java
│   ├── LogRecordStore.java
│   └── LogRequestResolver.java
├── store/
│   ├── JdbcLogRecordStore.java
│   ├── Slf4jLogRecordStore.java
│   ├── AsyncLogRecordStore.java
│   └── LogSchemaInitializer.java
├── support/
│   ├── DefaultLogOperatorProvider.java
│   ├── SecurityLogOperatorProvider.java
│   ├── NoopLogRequestResolver.java
│   ├── ServletLogRequestResolver.java
│   ├── LogParameterSerializer.java
│   ├── JacksonLogParameterSerializer.java
│   ├── ToStringLogParameterSerializer.java
│   └── LogRecordAsyncExecutor.java
├── LogRecordAutoConfiguration.java
├── LogRecordSecurityAutoConfiguration.java
├── LogRecordWebAutoConfiguration.java
├── LogRecordJdbcAutoConfiguration.java
├── LogRecordSlf4jAutoConfiguration.java
└── LogRecordStorageValidator.java
```

## 7. 运行时行为

### 7.1 切面流程

1. 解析方法上的 `@LogRecord`，缓存为 `LogRecordOperation`。
2. `LogRecordContext.pushSpan()`。
3. 扫描模板中的 before 函数，在业务方法执行前求值并缓存。
4. `proceed()`；捕获 `Throwable`。
5. 方法结束后：
   - 解析 `condition`，false 则跳过。
   - 根据成功/失败选择 `success` 或 `fail` 模板。
   - 渲染 `bizNo`、`content`、`detail`、`operator`。
   - 解析操作人、IP/UA、参数快照。
   - 组装 `OperationLog`。
   - 调用 `LogRecordStore.save()`。
6. `finally` 中 `LogRecordContext.popSpan()`。
7. 原样返回结果或抛出业务异常。

日志记录步骤全部包在 `try/catch` 中：

- 解析失败、模板错误、存储失败：打 WARN，不影响业务。
- `bizNo` 渲染后为空：打 WARN 并跳过本条。
- 存储失败：打 WARN，继续原业务结果。

### 7.2 切面顺序

- 默认 `aspect-order = Ordered.LOWEST_PRECEDENCE - 1`，位于 `@Transactional` 切面外层。
- 业务事务先提交/回滚，然后才记录操作日志。
- JDBC 写入默认用 `REQUIRES_NEW` 独立事务；没有事务管理器时直接执行。

### 7.3 异步

- 渲染、操作人、请求信息都在调用线程完成，线程池里只执行 `save`。
- 默认 `caller-runs`：队列满时由调用线程执行，保证不丢日志。
- 关闭时 `waitForTasksToCompleteOnShutdown=true`，等待 `await-termination`。
- 异步写入失败只打 ERROR，不影响业务。
- 只包装内置的 JDBC/SLF4J 存储；接入方自定义 `LogRecordStore` 时不会被自动包装。

### 7.4 请求信息

- 有 `RequestContextHolder` 时取当前请求。
- IP：`trust-forwarded-headers=true` 时依次看 `X-Forwarded-For` 第一段、`X-Real-IP`，否则 `getRemoteAddr`。
- UA：`User-Agent`。
- 参数快照：只在 `params-enabled=true` 时记录；跳过 `HttpServletRequest`、`HttpServletResponse`、`MultipartFile` 等不可序列化参数；通过 `LogParameterSerializer` SPI 选择实现：默认 `ToStringLogParameterSerializer`，类路径有 Jackson 3 时用 `JacksonLogParameterSerializer`；序列化失败回退 `String.valueOf`。

### 7.5 上下文与线程池

- `LogRecordContext` 使用 `InheritableThreadLocal`，普通子线程能继承。
- 线程池复用的线程不会自动清理；如果业务在 `@LogRecord` 方法里再丢任务到线程池，需要自己清理。
- 异步落库不受影响，因为渲染已在调用线程完成。

## 8. 自动配置

### 8.1 类与顺序

- `LogRecordSecurityAutoConfiguration`
  - `@AutoConfiguration(before = LogRecordAutoConfiguration.class)`
  - `@ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")`
  - `@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)`
  - 注册 `SecurityLogOperatorProvider`。
- `LogRecordWebAutoConfiguration`
  - `@AutoConfiguration(before = LogRecordAutoConfiguration.class)`
  - `@ConditionalOnClass(name = "org.springframework.web.context.request.RequestContextHolder")`
  - `@ConditionalOnWebApplication(type = SERVLET)`
  - `@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)`
  - 注册 `ServletLogRequestResolver`。
  - 嵌套 Jackson 配置：`@ConditionalOnClass(name = "tools.jackson.databind.json.JsonMapper")` 时注册 `JacksonLogParameterSerializer`；否则使用默认 `ToStringLogParameterSerializer`。
- `LogRecordAutoConfiguration`
  - `@EnableConfigurationProperties(LogRecordProperties.class)`
  - `@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)`
  - 注册切面、表达式解析器、模板解析器、函数工厂、默认操作人、默认请求解析器、异步线程池、存储校验器。
- `LogRecordJdbcAutoConfiguration`
  - `@AutoConfiguration(afterName = {
      "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
      "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration"
    })`
  - `@ConditionalOnClass(JdbcTemplate.class)`
  - `@ConditionalOnProperty(name = "itsuka.log.storage", havingValue = "jdbc", matchIfMissing = true)`
  - `@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)`
  - `@ConditionalOnSingleCandidate(DataSource.class)`
  - 类级 `@ConditionalOnMissingBean(LogRecordStore.class)`：接入方自定义存储时，默认存储、建表、默认查询一起退让。
  - 注册 `JdbcLogRecordStore`、`LogSchemaInitializer`、`JdbcOperationLogQueryService`。
- `LogRecordSlf4jAutoConfiguration`
  - `@ConditionalOnProperty(name = "itsuka.log.storage", havingValue = "slf4j")`
  - `@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)`
  - 类级 `@ConditionalOnMissingBean(LogRecordStore.class)`。
  - 注册 `Slf4jLogRecordStore`。

所有内置实现都带 `@ConditionalOnMissingBean`，接入方 Bean 优先；存储相关的类级条件保证自定义 `LogRecordStore` 时不会误建表、误注册默认查询。

### 8.2 存储校验

- `LogRecordStorageValidator` 在启动时检查：`enabled=true` 时必须存在至少一个 `LogRecordStore`（内置或自定义均可）。
- 不存在时抛出 `IllegalStateException`，消息包含：
  - 当前 `storage` 值
  - 常见原因（没有 DataSource、没有引入 JDBC starter、自定义 Bean 未注册）
- 目的：避免日志静默丢失。
- 存在自定义 `LogRecordStore` 时校验直接通过，内置建表/查询已按类级条件退让。

### 8.3 跨模块条件

- 安全、Web、Jackson 都是 `compileOnly`，通过 `@ConditionalOnClass(name = "...")` 隔离，不能因为缺少可选依赖导致 starter 加载失败。
- JDBC 是 `api` 依赖，默认存储可用。

## 9. 建表

- 脚本：`src/main/resources/META-INF/itsuka/log/schema-mysql.sql`
- 内容使用 `${tableName}` 占位符。
- `LogSchemaInitializer`：
  1. 校验表名 `^[A-Za-z_][A-Za-z0-9_]*$`，长度不超过 64。
  2. 读取 `schema-location`，替换 `${tableName}`。
  3. 用 `ResourceDatabasePopulator` + `DatabasePopulatorUtils` 执行。
  4. `CREATE TABLE IF NOT EXISTS` 保证幂等。
  5. 失败直接抛异常，启动失败。
- `auto-init-schema=false` 时完全跳过，接入方自己保证表存在。
- v1 不自动升级已存在的表结构；后续加列需要手工 `ALTER TABLE` 或提供升级脚本。

## 10. 构建改动

1. `settings.gradle.kts` 增加：
   ```kotlin
   include("common:itsuka-log-spring-boot-starter")
   ```
2. 根 `build.gradle.kts` 的 `moduleDescriptions` 增加模块描述。
3. 新模块 `build.gradle.kts`：
   ```kotlin
   dependencies {
       api(project(":common:itsuka-spring-boot-starter"))
       api("org.springframework.boot:spring-boot-starter-aspectj")
       api("org.springframework.boot:spring-boot-starter-jdbc")

       compileOnly("org.springframework.boot:spring-boot-starter-webmvc")
       compileOnly("org.springframework.security:spring-security-core")
       compileOnly("org.springframework.boot:spring-boot-starter-jackson")

       annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

       testImplementation("org.springframework.boot:spring-boot-starter-test")
       testImplementation("com.h2database:h2")
       testImplementation("org.testcontainers:mysql")
       testImplementation("org.testcontainers:junit-jupiter")
       testRuntimeOnly("com.mysql:mysql-connector-j")
   }

   tasks.withType<JavaCompile> {
       options.compilerArgs.add("-parameters")
   }
   ```
4. 新模块自动进入现有发布配置（模块名以 `itsuka-` 开头）。
5. 不改动其他模块。

## 11. 测试

### 11.1 单元测试

- `LogRecordContextTest`：压栈/弹栈、嵌套方法变量隔离。
- `LogRecordValueParserTest`：
  - `{{#arg}}`、`{{#request.address}}`
  - `{FUNC{{#arg}}}` 前后函数
  - `_ret`、`_errorMsg`
  - 未知函数
  - 不匹配的 `{` 原样保留
- `LogRecordOperationParserTest`：注解解析、缓存。
- `LogRecordFunctionFactoryTest`：重复函数名启动失败。
- `LogRecordPropertiesTest`：表名校验、分页上限。

### 11.2 集成测试

- `LogRecordAspectTest`：Spring 上下文 + 测试服务 + 内存 `LogRecordStore`，验证：
  - 成功记录 success
  - 异常记录 fail 且异常原样抛出
  - 存储抛异常不影响业务
  - `condition=false` 不记录
  - 嵌套 `@LogRecord` 上下文隔离
- `JdbcLogRecordStoreTest`：H2 MySQL 模式 + 测试建表脚本，验证插入、截断、独立事务。
- `JdbcOperationLogQueryServiceTest`：分页、过滤、排序、页码上限。
- `LogRecordAutoConfigurationTest`：`ApplicationContextRunner` 验证开关、storage 切换、自定义 Bean 覆盖、缺少存储时启动失败。

### 11.3 MySQL 方言测试

- `MySqlSchemaIT`：Testcontainers MySQL 8，验证生产建表脚本能执行、JdbcTemplate 读写正常。
- 无 Docker 时通过 `@Testcontainers(disabledWithoutDocker = true)` 跳过。

### 11.4 验证命令

```bash
./gradlew :common:itsuka-log-spring-boot-starter:test
./gradlew :common:itsuka-log-spring-boot-starter:build
```

## 12. 已知限制

- Spring AOP 只拦截 Spring Bean 的外部调用；同类自调用、private/final 方法不生效。
- 接口方法上的注解做尽力支持；推荐把 `@LogRecord` 标在实现类方法上。
- 参数名依赖 `-parameters` 编译参数；没有时可用 `{{#a0}}`、`{{#p0}}` 形式。
- 模板表达式内部不能包含 `}`。
- `InheritableThreadLocal` 不解决线程池复用问题。
- 异步写入不保证严格顺序；进程崩溃可能丢队列中的日志。
- 只支持 MySQL；表结构不会自动升级。
- 不提供多租户、清理、UI、对象 diff。
- 参数快照可能包含敏感信息，默认关闭；开启前需要业务自己脱敏。
- `storage=slf4j` 没有内置查询能力。
- 查询服务只做数据读取，不负责数据权限。

## 13. 我的决定（可推翻）

1. 默认存储用 `JdbcTemplate`，不引入 MyBatis-Plus，保持 starter 轻量。
2. 查询分页 DTO 自带，不复用 `org.itsuka.web.dto.PageResult`，避免强制依赖 Web starter。
3. 模板语法采用 `{{#expr}}` / `{FUNC{{#expr}}}`，与参考实现一致，而不是文章里简写的 `#expr`。
4. 除 JDBC 外附带 `slf4j` 存储，作为“配置其他存储方式”的开箱示例。
5. 异步默认关闭，优先保证日志不丢。
6. `bizNo` 必填；渲染后为空则跳过并 WARN。
7. 同时提供 `type` 和 `category` 两个分类字段。
8. JDBC starter 作为 `api` 依赖；MySQL 驱动由接入方提供。
9. 建表失败、缺少存储 Bean 时启动失败，不静默降级。
10. 切面默认在事务切面外层，JDBC 写入默认 `REQUIRES_NEW`。
11. 自动建表默认开启；关闭后不做任何 DDL。
12. 自定义 `LogRecordStore` 时，内置存储、建表、默认查询、异步包装全部退让；需要内置查询的接入方自己声明查询 Bean。
13. 分页 DTO 的 `current` / `size` 用可空 `Long`，默认值由 `query.default-page-size` 决定，避免配置失效。
14. 所有字符串字段按列宽截断，避免超长 UA、bizNo、操作人名导致整条日志写入失败。

## 14. 后续可做

- PostgreSQL/达梦等方言。
- 表结构自动升级（内部版本表）。
- 日志保留与清理任务。
- 多租户字段与租户 SPI。
- 对象 diff、`@DiffLogIgnore`。
- 查询 Controller / 管理页面。
- TTL 上下文传递。
- 批量写入、消息队列存储。

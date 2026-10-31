# 操作日志 Starter 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `itsuka-log-spring-boot-starter`，提供注解式操作日志、MySQL 自动建表、分页查询和可替换 SPI。

**Architecture:** 新 Gradle 子模块 `common/itsuka-log-spring-boot-starter`。AOP 切面解析 `@LogRecord`，用 SpEL + 自定义函数渲染文案，通过 `LogRecordStore` 落库；默认 `JdbcTemplate` 存储，启动时执行幂等建表；自动配置按 `storage` 和环境条件装配 JDBC/SLF4J 存储、查询、操作人和请求信息。

**Tech Stack:** Java 21、Spring Boot 4.0.3、Spring AOP（`spring-boot-starter-aspectj`）、JdbcTemplate、JUnit 5、AssertJ、H2、Testcontainers MySQL。

**Spec:** `docs/superpowers/specs/2026-10-31-operation-log-starter-design.md`

## Global Constraints

- Java 21，Spring Boot 4.0.3，Jakarta API。
- 新模块路径：`common/itsuka-log-spring-boot-starter`；包名：`org.itsuka.log`。
- 不引入 MyBatis-Plus、Flyway、Liquibase。
- 默认存储只支持 MySQL 8+；默认表名 `itsuka_operation_log`。
- 默认 Bean 一律带 `@ConditionalOnMissingBean`；可选依赖用 `@ConditionalOnClass(name = "...")` 隔离。
- 日志解析、操作人、请求信息、持久化失败的异常一律只 WARN/ERROR，不影响业务返回值和业务异常。
- 模板语法固定为：普通 SpEL `{{#expr}}`，自定义函数 `{FUNC{#expr}}`；`condition` 是原始 SpEL。
- 所有字符串字段写入前按列宽截断；content/detail/error 使用配置长度。
- 表名必须匹配 `^[A-Za-z_][A-Za-z0-9_]*$` 且不超过 64 字符。
- 新模块的 Java 编译必须带 `-parameters`。
- 不改动其他模块的源码；只允许改 `settings.gradle.kts`、根 `build.gradle.kts` 的模块描述。
- 测试使用 JUnit 5 + AssertJ；每个 Task 结束前必须跑对应测试并提交。

## Review Focus

1. 业务方法抛异常时，原始异常类型和 message 必须原样保留，同时写入失败日志。
2. 接入方自定义 `LogRecordStore` 时，不能执行建表、不能注册默认查询、不能自动包异步。
3. 超长 UA / bizNo / type / 操作人名称不能导致整条日志插入失败，必须按列宽截断。
4. 查询分页传入 null 或超大 size 时，必须使用默认值并截断到上限。
5. `storage=jdbc` 但应用没有 DataSource 时，必须启动失败并给出可操作的信息，不能静默丢日志。

---

### Task 1: 模块骨架、注解、领域模型、配置属性

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `common/itsuka-log-spring-boot-starter/build.gradle.kts`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/annotation/LogRecord.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/domain/LogOperator.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/domain/OperationLog.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/domain/OperationLogQuery.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/domain/OperationLogPage.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/properties/LogRecordProperties.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/properties/LogRecordPropertiesTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/domain/OperationLogPageTest.java`

**Interfaces:**
- Consumes: 无。
- Produces:
  - `org.itsuka.log.annotation.LogRecord`，元素：`success`, `fail`, `operator`, `bizNo`, `type`, `category`, `detail`, `condition`。
  - `OperationLog`：`Long id`, `String type`, `String category`, `String bizNo`, `boolean success`, `String content`, `String detail`, `String operatorId`, `String operatorName`, `String errorMsg`, `String method`, `String requestIp`, `String userAgent`, `String requestParams`, `LocalDateTime createdAt`。
  - `OperationLogQuery`：`String type/category/bizNo/operatorId`, `Boolean success`, `LocalDateTime startTime/endTime`, `Long current/size`。
  - `OperationLogPage`：`List<OperationLog> records`, `long current/size/total/pages`，静态工厂 `of(records, current, size, total)`。
  - `LogOperator`：`record LogOperator(String id, String name)`。
  - `LogRecordProperties`：前缀 `itsuka.log`，含 `Storage` 枚举（`JDBC`, `SLF4J`）、`RejectionPolicy` 枚举（`CALLER_RUNS`, `ABORT`, `DISCARD`, `DISCARD_OLDEST`），嵌套 `Async`、`Operator`、`Request`、`Transaction`、`Query`，方法 `validate()`。

- [ ] **Step 1: 写失败测试**

`LogRecordPropertiesTest`：

```java
package org.itsuka.log.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LogRecordPropertiesTest {

    @Test
    void defaultsAreUsable() {
        LogRecordProperties props = new LogRecordProperties();
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getStorage()).isEqualTo(LogRecordProperties.Storage.JDBC);
        assertThat(props.getTableName()).isEqualTo("itsuka_operation_log");
        assertThat(props.isAutoInitSchema()).isTrue();
        assertThat(props.getQuery().getDefaultPageSize()).isEqualTo(20);
        assertThat(props.getQuery().getMaxPageSize()).isEqualTo(200);
        assertThat(props.getAsync().getRejectionPolicy())
            .isEqualTo(LogRecordProperties.RejectionPolicy.CALLER_RUNS);
        props.validate();
    }

    @Test
    void rejectsInvalidTableName() {
        LogRecordProperties props = new LogRecordProperties();
        props.setTableName("bad-name;drop");
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tableName");
    }

    @Test
    void rejectsInvalidPoolSizes() {
        LogRecordProperties props = new LogRecordProperties();
        props.getAsync().setCorePoolSize(4);
        props.getAsync().setMaxPoolSize(2);
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPoolSize");
    }
}
```

`OperationLogPageTest`：

```java
package org.itsuka.log.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OperationLogPageTest {

    @Test
    void computesPages() {
        OperationLogPage page = OperationLogPage.of(List.of(), 1, 20, 41);
        assertThat(page.getPages()).isEqualTo(3);
        assertThat(page.getTotal()).isEqualTo(41);
    }

    @Test
    void zeroSizeHasZeroPages() {
        assertThat(OperationLogPage.of(List.of(), 1, 0, 10).getPages()).isZero();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.properties.LogRecordPropertiesTest"`
Expected: FAIL，模块不存在或类不存在。

- [ ] **Step 3: 创建模块骨架和实现**

`settings.gradle.kts` 增加：

```kotlin
include("common:itsuka-log-spring-boot-starter")
```

根 `build.gradle.kts` 的 `moduleDescriptions` 增加：

```kotlin
"itsuka-log-spring-boot-starter" to "操作日志：注解式记录、SpEL 模板、MySQL 存储与分页查询"
```

`common/itsuka-log-spring-boot-starter/build.gradle.kts`：

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
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

实现注解：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRecord {
    String success();
    String fail() default "";
    String operator() default "";
    String bizNo();
    String type() default "";
    String category() default "";
    String detail() default "";
    String condition() default "";
}
```

实现四个 domain 类型和 `LogRecordProperties`。`OperationLog` 使用 Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`；`OperationLogQuery` 使用 `@Data`；`OperationLogPage` 使用 `@Data @NoArgsConstructor @AllArgsConstructor` 并提供 `of`。

`LogRecordProperties` 关键字段与默认值：

```java
private boolean enabled = true;
private Storage storage = Storage.JDBC;
private String tableName = "itsuka_operation_log";
private boolean autoInitSchema = true;
private String schemaLocation = "classpath:META-INF/itsuka/log/schema-mysql.sql";
private int contentMaxLength = 2000;
private int detailMaxLength = 4000;
private int errorMaxLength = 2000;
private int aspectOrder = Integer.MAX_VALUE - 1;
private Async async = new Async();
private Operator operator = new Operator();
private Request request = new Request();
private Transaction transaction = new Transaction();
private Query query = new Query();
```

嵌套默认值：

- `Async`: `enabled=false`, `corePoolSize=1`, `maxPoolSize=4`, `queueCapacity=1024`, `keepAlive=Duration.ofSeconds(60)`, `threadNamePrefix="itsuka-log-"`, `rejectionPolicy=CALLER_RUNS`, `awaitTermination=Duration.ofSeconds(5)`。
- `Operator`: `defaultId="system"`, `defaultName="系统"`。
- `Request`: `enabled=true`, `trustForwardedHeaders=false`, `paramsEnabled=false`, `paramsMaxLength=4000`。
- `Transaction`: `requiresNew=true`。
- `Query`: `enabled=true`, `defaultPageSize=20`, `maxPageSize=200`。

`validate()` 至少校验：表名正则/长度、`contentMaxLength > 0`、`detailMaxLength > 0`、`errorMaxLength > 0`、`async.corePoolSize >= 1`、`async.maxPoolSize >= async.corePoolSize`、`async.queueCapacity >= 0`、`query.defaultPageSize >= 1`、`query.maxPageSize >= query.defaultPageSize`。非法时抛 `IllegalArgumentException`，消息包含字段名。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.properties.LogRecordPropertiesTest" --tests "org.itsuka.log.domain.OperationLogPageTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add settings.gradle.kts build.gradle.kts common/itsuka-log-spring-boot-starter
git commit -m "feat(log): add module skeleton, annotation and domain model"
```

---

### Task 2: LogRecordContext

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/context/LogRecordContext.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/context/LogRecordContextTest.java`

**Interfaces:**
- Consumes: 无。
- Produces：
  - `LogRecordContext.pushSpan()`、`popSpan()`、`clear()`
  - `LogRecordContext.putVariable(String name, Object value)`
  - `LogRecordContext.getVariables()` 返回当前 span 的 `Map<String,Object>`，无 span 时返回空 Map。

- [ ] **Step 1: 写失败测试**

```java
package org.itsuka.log.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogRecordContextTest {

    @AfterEach
    void tearDown() {
        LogRecordContext.clear();
    }

    @Test
    void putsAndReadsVariable() {
        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("oldAddress", "A");
        assertThat(LogRecordContext.getVariables())
            .containsEntry("oldAddress", "A");
    }

    @Test
    void nestedSpansDoNotOverwriteEachOther() {
        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("value", 1);

        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("value", 2);
        assertThat(LogRecordContext.getVariables()).containsEntry("value", 2);
        LogRecordContext.popSpan();

        assertThat(LogRecordContext.getVariables()).containsEntry("value", 1);
        LogRecordContext.popSpan();
        assertThat(LogRecordContext.getVariables()).isEmpty();
    }

    @Test
    void popWithoutPushIsSafe() {
        LogRecordContext.popSpan();
        assertThat(LogRecordContext.getVariables()).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.context.LogRecordContextTest"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

使用 `InheritableThreadLocal<Deque<Map<String, Object>>>`，`ArrayDeque` 作为栈。`getVariables()` 在没有 span 时返回 `Map.of()`。`putVariable` 在没有 span 时自动 `pushSpan()`。`popSpan` 空栈时 no-op。`clear()` 移除 ThreadLocal。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.context.LogRecordContextTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/context common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/context
git commit -m "feat(log): add LogRecordContext with nested spans"
```

---

### Task 3: SpEL 表达式求值

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordExpressionEvaluator.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordExpressionEvaluatorTest.java`

**Interfaces:**
- Consumes: `LogRecordContext.getVariables()`。
- Produces：
  - `EvaluationContext createContext(Method method, Object[] args, Class<?> targetClass, Object result, String errorMsg, BeanFactory beanFactory)`
  - `Object evaluate(String expression, AnnotatedElementKey key, EvaluationContext context)`

- [ ] **Step 1: 写失败测试**

```java
package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.itsuka.log.context.LogRecordContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

class LogRecordExpressionEvaluatorTest {

    record Address(String city) {}
    record Param(String orderNo, Address address) {}

    static class SampleService {
        public String update(Param param, int count) {
            return "NO1";
        }
    }

    private final LogRecordExpressionEvaluator evaluator = new LogRecordExpressionEvaluator();

    @AfterEach
    void tearDown() {
        LogRecordContext.clear();
    }

    private EvaluationContext context(Method method, Object result, String error) {
        return evaluator.createContext(
            method,
            new Object[]{new Param("NO1", new Address("上海")), 3},
            SampleService.class,
            result,
            error,
            null
        );
    }

    @Test
    void evaluatesParametersReturnAndError() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        EvaluationContext ctx = context(method, "OK", "boom");

        assertThat(evaluator.evaluate("#param.orderNo", key, ctx)).isEqualTo("NO1");
        assertThat(evaluator.evaluate("#param.address.city", key, ctx)).isEqualTo("上海");
        assertThat(evaluator.evaluate("#count > 2", key, ctx)).isEqualTo(true);
        assertThat(evaluator.evaluate("#_ret", key, ctx)).isEqualTo("OK");
        assertThat(evaluator.evaluate("#_errorMsg", key, ctx)).isEqualTo("boom");
    }

    @Test
    void evaluatesContextVariable() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("oldAddress", "北京");

        assertThat(evaluator.evaluate("#oldAddress", key, context(method, null, null)))
            .isEqualTo("北京");
    }

    @Test
    void fallsBackToPositionalNames() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        EvaluationContext ctx = context(method, null, null);

        assertThat(evaluator.evaluate("#a0.orderNo", key, ctx)).isEqualTo("NO1");
        assertThat(evaluator.evaluate("#p1", key, ctx)).isEqualTo(3);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.core.LogRecordExpressionEvaluatorTest"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

`LogRecordExpressionEvaluator extends CachedExpressionEvaluator`：

- `createContext` 创建 `LogRecordEvaluationContext`（继承 `MethodBasedEvaluationContext`），参数用 `DefaultParameterNameDiscoverer`。
- 把 `LogRecordContext.getVariables()` 逐个 `setVariable`。
- 设置 `_ret`、`_errorMsg`。
- `beanFactory != null` 时设置 `BeanFactoryResolver`。
- `evaluate` 用 `getExpression(expressionCache, key, expression).getValue(context)`。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.core.LogRecordExpressionEvaluatorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordExpressionEvaluator.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core
git commit -m "feat(log): add SpEL expression evaluator"
```

---

### Task 4: 自定义函数与模板解析

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/function/LogRecordFunction.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/function/LogRecordFunctionFactory.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordValueParser.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/function/LogRecordFunctionFactoryTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordValueParserTest.java`

**Interfaces:**
- Consumes: `LogRecordExpressionEvaluator`。
- Produces：
  - `LogRecordFunction`：`functionName()`, `executeBefore()`, `apply(Object)`。
  - `LogRecordFunctionFactory(List<LogRecordFunction>)`，`LogRecordFunction get(String name)`（不存在返回 null）。
  - `LogRecordValueParser`：
    - `String parse(String template, EvaluationContext context, AnnotatedElementKey key, Map<String, String> beforeValues)`
    - `Map<String, String> parseBeforeFunctions(Collection<String> templates, EvaluationContext context, AnnotatedElementKey key)`
    - `boolean evaluateCondition(String condition, EvaluationContext context, AnnotatedElementKey key)`
    - `Object parseSingleExpression(String template, EvaluationContext context, AnnotatedElementKey key)`

- [ ] **Step 1: 写失败测试**

`LogRecordFunctionFactoryTest`：

```java
package org.itsuka.log.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogRecordFunctionFactoryTest {

    private LogRecordFunction fn(String name) {
        return new LogRecordFunction() {
            @Override public String functionName() { return name; }
            @Override public String apply(Object value) { return name + ":" + value; }
        };
    }

    @Test
    void findsFunctionByName() {
        LogRecordFunctionFactory factory = new LogRecordFunctionFactory(List.of(fn("ORDER")));
        assertThat(factory.get("ORDER").apply("1")).isEqualTo("ORDER:1");
        assertThat(factory.get("MISSING")).isNull();
    }

    @Test
    void duplicateNamesFailFast() {
        assertThatThrownBy(() -> new LogRecordFunctionFactory(List.of(fn("ORDER"), fn("ORDER"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ORDER");
    }
}
```

`LogRecordValueParserTest`：

```java
package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.itsuka.log.function.LogRecordFunction;
import org.itsuka.log.function.LogRecordFunctionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

class LogRecordValueParserTest {

    record Request(String address) {}
    record Param(String orderNo, Request request, String user) {}

    static class SampleService {
        public String update(Param param) { return "OK"; }
    }

    private final LogRecordExpressionEvaluator evaluator = new LogRecordExpressionEvaluator();

    private EvaluationContext context(Method method, Object result, String error) {
        return evaluator.createContext(
            method,
            new Object[]{new Param("NO1", new Request("A"), "张三")},
            SampleService.class, result, error, null
        );
    }

    private LogRecordValueParser parser(List<LogRecordFunction> functions) {
        return new LogRecordValueParser(evaluator, new LogRecordFunctionFactory(functions));
    }

    @Test
    void rendersPlainExpressions() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        LogRecordValueParser parser = parser(List.of());

        assertThat(parser.parse("订单{{#param.orderNo}} 地址{{#param.request.address}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("订单NO1 地址A");
    }

    @Test
    void appliesFunctionAfterAndBefore() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        LogRecordFunction upper = new LogRecordFunction() {
            @Override public String functionName() { return "UPPER"; }
            @Override public String apply(Object value) { return value.toString().toUpperCase(); }
        };
        LogRecordFunction old = new LogRecordFunction() {
            @Override public String functionName() { return "OLD"; }
            @Override public boolean executeBefore() { return true; }
            @Override public String apply(Object value) { return "OLD:" + value; }
        };
        LogRecordValueParser parser = parser(List.of(upper, old));
        EvaluationContext ctx = evaluator.createContext(
            method,
            new Object[]{new Param("no1", new Request("A"), "张三")},
            SampleService.class, null, null, null);

        assertThat(parser.parse("{UPPER{#param.orderNo}}", ctx, key, Map.of()))
            .isEqualTo("NO1");
        assertThat(parser.parse("{OLD{#param.orderNo}}", ctx, key, Map.of("{OLD{#param.orderNo}}", "旧值")))
            .isEqualTo("旧值");

        Map<String, String> before = parser.parseBeforeFunctions(
            List.of("{OLD{#param.orderNo}}"), ctx, key);
        assertThat(before).containsEntry("{OLD{#param.orderNo}}", "OLD:no1");
    }

    @Test
    void keepsLiteralBraceAfterExpression() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).parse("结果{{#param.orderNo}}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("结果NO1}");
    }

    @Test
    void nullExpressionRendersEmpty() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).parse("值={{#missing}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("值=");
    }

    @Test
    void unknownFunctionReturnsValue() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).parse("{MISSING{#param.orderNo}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("NO1");
    }

    @Test
    void evaluatesCondition() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).evaluateCondition("#param.orderNo != null",
            context(method, null, null), key)).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.function.LogRecordFunctionFactoryTest" --tests "org.itsuka.log.core.LogRecordValueParserTest"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

- 正则：`\{\s*(\w*)\s*\{([^{}]*?)}}`。
- `parse`：
  - 对每个匹配，`name = group(1)`，`expr = group(2)`。
  - `name` 为空：`evaluator.evaluate(expr, key, context)`；结果为 null 时输出空字符串。
  - `beforeValues` 包含完整匹配文本：直接用缓存值。
  - 否则 `value = evaluator.evaluate(expr, key, context)`，查函数；存在则 `apply(value)`，不存在则 `String.valueOf(value)` 并 `log.warn`；null 结果统一输出空字符串。
  - 用 `Matcher.appendReplacement` + `Matcher.quoteReplacement` 替换。
- `parseBeforeFunctions`：遍历模板，只处理 `name` 非空、`factory.get(name) != null`、`executeBefore() == true`、且 expr 不含 `#_ret` / `#_errorMsg` 的匹配；缓存 key 用完整匹配文本，value 用 `function.apply(evaluated)`。
- `evaluateCondition`：去掉外层 `{{ }}`（若存在），用 `evaluator.evaluate` 并 `Boolean.TRUE.equals(...)`。
- `parseSingleExpression`：若整个模板是一个匹配，返回原始求值对象（函数则返回 `apply` 结果）；否则返回 `parse` 的字符串。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.function.LogRecordFunctionFactoryTest" --tests "org.itsuka.log.core.LogRecordValueParserTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/function common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordValueParser.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/function common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordValueParserTest.java
git commit -m "feat(log): add function SPI and template parser"
```

---

### Task 5: SPI 接口、默认实现与注解解析

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/spi/LogRecordStore.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/spi/LogOperatorProvider.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/spi/LogRequestResolver.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/LogParameterSerializer.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/DefaultLogOperatorProvider.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/NoopLogRequestResolver.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/ToStringLogParameterSerializer.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/LogRecordTextUtils.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordOperation.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordOperationParser.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordOperationParserTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support/LogRecordTextUtilsTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support/DefaultLogOperatorProviderTest.java`

**Interfaces:**
- Consumes: `OperationLog`, `LogOperator`, `LogRecordProperties`。
- Produces：
  - `LogRecordStore`：`void save(OperationLog log)`。
  - `LogOperatorProvider`：`LogOperator getOperator()`。
  - `LogRequestResolver`：`String getClientIp()`, `String getUserAgent()`。
  - `LogParameterSerializer`：`String serialize(Method method, Object[] args)`。
  - `LogRecordTextUtils.truncate(String value, int maxLength)`。
  - `LogRecordOperation` record：`success`, `fail`, `operator`, `bizNo`, `type`, `category`, `detail`, `condition`。
  - `LogRecordOperationParser.parse(Method method)` 返回 `Optional<LogRecordOperation>`。

- [ ] **Step 1: 写失败测试**

`LogRecordOperationParserTest`：

```java
package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.itsuka.log.annotation.LogRecord;
import org.junit.jupiter.api.Test;

class LogRecordOperationParserTest {

    static class SampleService {
        @LogRecord(success = "ok", fail = "fail", operator = "{{#user}}",
            bizNo = "{{#id}}", type = "ORDER", category = "CUSTOMER",
            detail = "{{#detail}}", condition = "#id != null")
        public void annotated(String id) {}
        public void plain(String id) {}
    }

    private final LogRecordOperationParser parser = new LogRecordOperationParser();

    @Test
    void parsesAnnotation() throws Exception {
        Method method = SampleService.class.getMethod("annotated", String.class);
        LogRecordOperation op = parser.parse(method).orElseThrow();
        assertThat(op.success()).isEqualTo("ok");
        assertThat(op.fail()).isEqualTo("fail");
        assertThat(op.bizNo()).isEqualTo("{{#id}}");
        assertThat(op.type()).isEqualTo("ORDER");
        assertThat(op.condition()).isEqualTo("#id != null");
    }

    @Test
    void returnsEmptyWhenMissing() throws Exception {
        Method method = SampleService.class.getMethod("plain", String.class);
        assertThat(parser.parse(method)).isEmpty();
    }
}
```

`LogRecordTextUtilsTest`：

```java
package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRecordTextUtilsTest {

    @Test
    void truncatesWithEllipsis() {
        assertThat(LogRecordTextUtils.truncate("abcdef", 4)).isEqualTo("a...");
        assertThat(LogRecordTextUtils.truncate("abc", 4)).isEqualTo("abc");
        assertThat(LogRecordTextUtils.truncate(null, 4)).isNull();
    }
}
```

`DefaultLogOperatorProviderTest`：

```java
package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.Test;

class DefaultLogOperatorProviderTest {

    @Test
    void returnsConfiguredDefaults() {
        LogRecordProperties props = new LogRecordProperties();
        props.getOperator().setDefaultId("system");
        props.getOperator().setDefaultName("系统");
        LogOperator operator = new DefaultLogOperatorProvider(props).getOperator();
        assertThat(operator.id()).isEqualTo("system");
        assertThat(operator.name()).isEqualTo("系统");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.core.LogRecordOperationParserTest" --tests "org.itsuka.log.support.*"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

- `LogRecordTextUtils.truncate`：`maxLength <= 0` 返回空串；长度超过时 `substring(0, maxLength - 3) + "..."`；`maxLength < 4` 时 `substring(0, maxLength)`。
- `DefaultLogOperatorProvider`：返回配置的默认 id/name；空白时回退 `system` / `系统`。
- `NoopLogRequestResolver`：`getClientIp()` / `getUserAgent()` 返回 null。
- `ToStringLogParameterSerializer`：`Arrays.deepToString(args)`。
- `LogRecordOperationParser`：`AnnotatedElementUtils.findMergedAnnotation` + `ConcurrentHashMap<Method, Optional<LogRecordOperation>>` 缓存。
- `LogRecordOperation` 用 record，字段顺序按上面 Produces。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.core.LogRecordOperationParserTest" --tests "org.itsuka.log.support.*"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/spi common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordOperation.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordOperationParser.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordOperationParserTest.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support
git commit -m "feat(log): add SPI contracts, defaults and annotation parser"
```

---

### Task 6: 切面与失败隔离

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/MethodExecuteResult.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordAspect.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordAspectTest.java`

**Interfaces:**
- Consumes: `LogRecordOperationParser`, `LogRecordExpressionEvaluator`, `LogRecordValueParser`, `LogOperatorProvider`, `LogRequestResolver`, `LogParameterSerializer`, `LogRecordStore`, `LogRecordProperties`, `LogRecordContext`。
- Produces：
  - `LogRecordAspect(LogRecordProperties, LogRecordOperationParser, LogRecordExpressionEvaluator, LogRecordValueParser, LogOperatorProvider, LogRequestResolver, LogParameterSerializer, LogRecordStore)`，实现 `Ordered`。
  - `@Around("@annotation(org.itsuka.log.annotation.LogRecord)")`。

- [ ] **Step 1: 写失败测试**

```java
package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.itsuka.log.annotation.LogRecord;
import org.itsuka.log.context.LogRecordContext;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.support.DefaultLogOperatorProvider;
import org.itsuka.log.support.NoopLogRequestResolver;
import org.itsuka.log.support.ToStringLogParameterSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class LogRecordAspectTest {

    static class SampleService {
        @LogRecord(
            success = "订单{{#orderNo}}创建成功",
            fail = "订单{{#orderNo}}创建失败：{{#_errorMsg}}",
            bizNo = "{{#orderNo}}",
            type = "ORDER"
        )
        public String create(String orderNo) {
            return "OK";
        }

        @LogRecord(success = "不会记录", bizNo = "{{#orderNo}}", condition = "#orderNo == null")
        public void conditionFalse(String orderNo) {
        }

        @LogRecord(success = "空业务号", bizNo = "{{#missing}}")
        public void blankBizNo() {
        }
    }

    static class FailingService {
        @LogRecord(
            success = "订单{{#orderNo}}创建成功",
            fail = "订单{{#orderNo}}创建失败：{{#_errorMsg}}",
            bizNo = "{{#orderNo}}",
            type = "ORDER"
        )
        public String create(String orderNo) {
            throw new IllegalStateException("boom");
        }
    }

    private final List<OperationLog> saved = new ArrayList<>();

    private <T> T proxy(T target, LogRecordProperties props, LogRecordStore store) {
        LogRecordAspect aspect = new LogRecordAspect(
            props,
            new LogRecordOperationParser(),
            new LogRecordExpressionEvaluator(),
            new LogRecordValueParser(new LogRecordExpressionEvaluator(),
                new org.itsuka.log.function.LogRecordFunctionFactory(List.of())),
            new DefaultLogOperatorProvider(props),
            new NoopLogRequestResolver(),
            new ToStringLogParameterSerializer(),
            store
        );
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @AfterEach
    void tearDown() {
        LogRecordContext.clear();
    }

    @Test
    void recordsSuccess() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), saved::add);
        assertThat(service.create("NO1")).isEqualTo("OK");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isSuccess()).isTrue();
        assertThat(saved.get(0).getBizNo()).isEqualTo("NO1");
        assertThat(saved.get(0).getContent()).isEqualTo("订单NO1创建成功");
        assertThat(saved.get(0).getType()).isEqualTo("ORDER");
    }

    @Test
    void recordsFailureAndRethrowsOriginalException() {
        FailingService failing = proxy(new FailingService(), new LogRecordProperties(), saved::add);

        assertThatThrownBy(() -> failing.create("NO1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isSuccess()).isFalse();
        assertThat(saved.get(0).getErrorMsg()).isEqualTo("boom");
        assertThat(saved.get(0).getContent()).isEqualTo("订单NO1创建失败：boom");
    }

    @Test
    void storageFailureDoesNotBreakBusiness() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), log -> {
            throw new IllegalStateException("db down");
        });
        assertThat(service.create("NO1")).isEqualTo("OK");
    }

    @Test
    void conditionFalseSkipsRecord() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), saved::add);
        service.conditionFalse("NO1");
        assertThat(saved).isEmpty();
    }

    @Test
    void blankBizNoSkipsRecord() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), saved::add);
        service.blankBizNo();
        assertThat(saved).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.core.LogRecordAspectTest"`
Expected: FAIL，`LogRecordAspect` 不存在。

- [ ] **Step 3: 实现**

- `MethodExecuteResult` record：`boolean success`, `Object result`, `Throwable throwable`。
- `LogRecordAspect` 流程：
  1. `MethodSignature` + `AopUtils.getMostSpecificMethod`，`operationParser.parse(method)`；无注解直接 `proceed()`。
  2. `LogRecordContext.pushSpan()`。
  3. `EvaluationContext`（`expressionEvaluator.createContext`）。
  4. `beforeValues = valueParser.parseBeforeFunctions(List.of(success, fail, bizNo, detail, operator), ctx, key)`。
  5. `proceed()`，捕获 `Throwable`。
  6. `condition` 为空或 `evaluateCondition` true 才继续。
  7. 选模板：成功 `success`；失败 `fail` 为空则 `success`。
  8. 渲染 `content`、`bizNo`、`detail`、`operator`；`bizNo` 空白则 WARN + skip。
  9. 解析操作人：
     - `operator` 非空时 `valueParser.parseSingleExpression`；`LogOperator` 直接用；其他非空对象作为 id，name 只在 id 等于 provider id 时用 provider name，否则空。
     - `operator` 为空时用 provider；provider 抛异常则用 `DefaultLogOperatorProvider`。
  10. 请求信息：`request.enabled` 时取 IP/UA；`params.enabled` 时 `parameterSerializer.serialize`。
  11. 截断 content/detail/error。
  12. 组装 `OperationLog`（`method = targetClass.getSimpleName() + "#" + method.getName()`，`createdAt = LocalDateTime.now()`）。
  13. `store.save(log)`。
  14. 日志步骤全部 `try/catch(Exception)` + WARN；业务 `Throwable` 在 finally 后原样抛出。
  15. `finally { LogRecordContext.popSpan(); }`。
- `getOrder()` 返回 `properties.getAspectOrder()`。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.core.LogRecordAspectTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/MethodExecuteResult.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/core/LogRecordAspect.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/core/LogRecordAspectTest.java
git commit -m "feat(log): add aspect with success/failure isolation"
```

---

### Task 7: JDBC 存储与自动建表

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store/JdbcLogRecordStore.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store/LogSchemaInitializer.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/resources/META-INF/itsuka/log/schema-mysql.sql`
- Test resource: `common/itsuka-log-spring-boot-starter/src/test/resources/schema-h2.sql`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/store/JdbcLogRecordStoreTest.java`

**Interfaces:**
- Consumes: `OperationLog`, `LogRecordProperties`, `LogRecordTextUtils`。
- Produces：
  - `JdbcLogRecordStore(JdbcTemplate jdbcTemplate, LogRecordProperties properties, @Nullable TransactionTemplate transactionTemplate)`，实现 `LogRecordStore`。
  - `LogSchemaInitializer(DataSource dataSource, LogRecordProperties properties, ResourceLoader resourceLoader)`，实现 `InitializingBean`。

- [ ] **Step 1: 写失败测试**

```java
package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcLogRecordStoreTest {

    private JdbcTemplate jdbc;
    private JdbcLogRecordStore store;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:logstore;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        LogRecordProperties props = new LogRecordProperties();
        props.setSchemaLocation("classpath:schema-h2.sql");
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();
        store = new JdbcLogRecordStore(jdbc, props, null);
    }

    @Test
    void savesAndTruncatesLongValues() {
        OperationLog log = OperationLog.builder()
            .bizNo("B".repeat(200))
            .type("T".repeat(100))
            .success(true)
            .content("C".repeat(3000))
            .detail("D".repeat(5000))
            .operatorId("O".repeat(100))
            .operatorName("N".repeat(200))
            .method("M".repeat(300))
            .requestIp("I".repeat(100))
            .userAgent("U".repeat(600))
            .requestParams("P".repeat(5000))
            .createdAt(LocalDateTime.now())
            .build();

        store.save(log);

        Integer count = jdbc.queryForObject("select count(*) from itsuka_operation_log", Integer.class);
        assertThat(count).isEqualTo(1);
        String bizNo = jdbc.queryForObject("select biz_no from itsuka_operation_log", String.class);
        assertThat(bizNo).hasSize(128);
    }

    @Test
    void autoInitCanBeDisabled() {
        LogRecordProperties props = new LogRecordProperties();
        props.setAutoInitSchema(false);
        props.setSchemaLocation("classpath:schema-h2.sql");
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:nodll;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();
        Integer count = new JdbcTemplate(ds)
            .queryForObject("select count(*) from information_schema.tables where table_name = 'itsuka_operation_log'", Integer.class);
        assertThat(count).isZero();
    }
}
```

`schema-h2.sql` 与生产 DDL 字段一致，但去掉 `ENGINE=InnoDB`、`COLLATE`，使用 H2 MySQL 模式可执行的 `BIGINT AUTO_INCREMENT`、`VARCHAR`、`TEXT`、`TIMESTAMP(3)`。

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.store.JdbcLogRecordStoreTest"`
Expected: FAIL，类/脚本不存在。

- [ ] **Step 3: 实现**

- `schema-mysql.sql`：按 spec 第 4 节 DDL，表名 `${tableName}`。
- `LogSchemaInitializer`：`validate()`；读 `schemaLocation`（`Resource` 或 `String`，按 properties 类型）；替换 `${tableName}`；`ResourceDatabasePopulator` + `DatabasePopulatorUtils.execute`；`continueOnError=false`；异常包装成 `IllegalStateException`。
- `JdbcLogRecordStore`：
  - INSERT 语句参数化，列顺序固定。
  - 所有字符串写入前截断：`bizNo` 128、`type`/`category` 64、`content` 配置、`detail` 配置、`operatorId` 64、`operatorName` 128、`errorMsg` 配置、`method` 255、`requestIp` 64、`userAgent` 512、`requestParams` 配置。
  - `transactionTemplate != null` 时在 `execute` 内 update，否则直接 update。
  - 不吞异常；异常交给切面处理。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.store.JdbcLogRecordStoreTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store common/itsuka-log-spring-boot-starter/src/main/resources/META-INF/itsuka/log common/itsuka-log-spring-boot-starter/src/test/resources/schema-h2.sql common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/store
git commit -m "feat(log): add JDBC store and idempotent schema initializer"
```

---

### Task 8: 内置分页查询

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/query/OperationLogQueryService.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/query/JdbcOperationLogQueryService.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/query/JdbcOperationLogQueryServiceTest.java`

**Interfaces:**
- Consumes: `OperationLog`, `OperationLogQuery`, `OperationLogPage`, `LogRecordProperties`。
- Produces：
  - `OperationLogQueryService`：`OperationLogPage page(OperationLogQuery query)`, `Optional<OperationLog> findById(Long id)`。
  - `JdbcOperationLogQueryService(NamedParameterJdbcTemplate jdbcTemplate, LogRecordProperties properties)`。

- [ ] **Step 1: 写失败测试**

```java
package org.itsuka.log.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.domain.OperationLogPage;
import org.itsuka.log.domain.OperationLogQuery;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcOperationLogQueryServiceTest {

    private JdbcLogRecordStore store;
    private JdbcOperationLogQueryService queryService;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:logquery;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP ALL OBJECTS");
        LogRecordProperties props = new LogRecordProperties();
        props.setSchemaLocation("classpath:schema-h2.sql");
        props.getQuery().setDefaultPageSize(2);
        props.getQuery().setMaxPageSize(5);
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();
        store = new JdbcLogRecordStore(jdbc, props, null);
        queryService = new JdbcOperationLogQueryService(new NamedParameterJdbcTemplate(ds), props);

        store.save(log("NO1", "ORDER", true, LocalDateTime.now().minusMinutes(3)));
        store.save(log("NO1", "ORDER", false, LocalDateTime.now().minusMinutes(2)));
        store.save(log("NO2", "USER", true, LocalDateTime.now().minusMinutes(1)));
    }

    private OperationLog log(String bizNo, String type, boolean success, LocalDateTime time) {
        return OperationLog.builder().bizNo(bizNo).type(type).success(success)
            .content("content").operatorId("u1").createdAt(time).build();
    }

    @Test
    void pagesByBizNoWithDefaultSize() {
        OperationLogQuery query = new OperationLogQuery();
        query.setBizNo("NO1");
        OperationLogPage page = queryService.page(query);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void clampsOversizedPage() {
        OperationLogQuery query = new OperationLogQuery();
        query.setSize(100L);
        assertThat(queryService.page(query).getSize()).isEqualTo(5);
    }

    @Test
    void nullCurrentAndSizeUseDefaults() {
        OperationLogQuery query = new OperationLogQuery();
        query.setCurrent(null);
        query.setSize(null);
        OperationLogPage page = queryService.page(query);
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void findsById() {
        OperationLogQuery query = new OperationLogQuery();
        query.setBizNo("NO2");
        Long id = queryService.page(query).getRecords().get(0).getId();
        assertThat(queryService.findById(id)).isPresent();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.query.JdbcOperationLogQueryServiceTest"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

- `page`：
  - `current = query.current == null || < 1 ? 1 : current`。
  - `size = query.size == null ? props.query.defaultPageSize : query.size`；`size = min(size, maxPageSize)`；`size < 1` 用 1。
  - 动态 WHERE：`type`, `category`, `bizNo`, `operatorId`, `success`, `created_at >= startTime`, `created_at <= endTime`。
  - `COUNT(*)`；`SELECT ... ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset`。
  - `OperationLogPage.of(records, current, size, total)`。
- `findById`：`SELECT ... WHERE id = :id`，返回 `Optional`。
- 表名从 properties 取，校验后拼进 SQL；其余全部命名参数。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.query.JdbcOperationLogQueryServiceTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/query common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/query
git commit -m "feat(log): add paginated operation log query"
```

---

### Task 9: SLF4J 存储与异步装饰器

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store/Slf4jLogRecordStore.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store/AsyncLogRecordStore.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/LogRecordAsyncExecutor.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/store/Slf4jLogRecordStoreTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/store/AsyncLogRecordStoreTest.java`

**Interfaces:**
- Consumes: `LogRecordStore`, `OperationLog`, `LogRecordProperties.Async`。
- Produces：
  - `Slf4jLogRecordStore`，实现 `LogRecordStore`。
  - `AsyncLogRecordStore(LogRecordStore delegate, AsyncTaskExecutor executor)`，实现 `LogRecordStore`。
  - `LogRecordAsyncExecutor.create(LogRecordProperties.Async properties)` 返回 `ThreadPoolTaskExecutor`。

- [ ] **Step 1: 写失败测试**

`Slf4jLogRecordStoreTest`：

```java
package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import org.itsuka.log.domain.OperationLog;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class Slf4jLogRecordStoreTest {

    @Test
    void writesToDedicatedLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger("itsuka.operation.log");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        new Slf4jLogRecordStore().save(OperationLog.builder()
            .bizNo("NO1").success(true).content("订单创建").createdAt(LocalDateTime.now()).build());

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("NO1", "订单创建");
        logger.detachAppender(appender);
    }
}
```

`AsyncLogRecordStoreTest`：

```java
package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncLogRecordStoreTest {

    @Test
    void delegatesOnExecutorThread() throws Exception {
        AtomicReference<String> thread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        LogRecordStore delegate = log -> {
            thread.set(Thread.currentThread().getName());
            latch.countDown();
        };
        LogRecordProperties.Async props = new LogRecordProperties.Async();
        props.setThreadNamePrefix("itsuka-log-test-");
        ThreadPoolTaskExecutor executor = LogRecordAsyncExecutor.create(props);
        executor.initialize();
        try {
            new AsyncLogRecordStore(delegate, executor).save(OperationLog.builder()
                .bizNo("NO1").success(true).content("x").createdAt(LocalDateTime.now()).build());
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(thread.get()).startsWith("itsuka-log-test-");
        } finally {
            executor.shutdown();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.store.Slf4jLogRecordStoreTest" --tests "org.itsuka.log.store.AsyncLogRecordStoreTest"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

- `Slf4jLogRecordStore`：`LoggerFactory.getLogger("itsuka.operation.log")`，INFO 输出 bizNo/type/category/success/operator/content/detail/error/method。
- `AsyncLogRecordStore`：`executor.execute(() -> delegate.save(log))`；捕获 `TaskRejectedException` 和 `RuntimeException`，ERROR 日志。
- `LogRecordAsyncExecutor.create`：设置 core/max/queue/keepAlive/threadNamePrefix；`setWaitForTasksToCompleteOnShutdown(true)`；`setAwaitTerminationSeconds`；拒绝策略映射；返回未初始化的 executor（由调用方或 `afterPropertiesSet` 初始化）。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.store.Slf4jLogRecordStoreTest" --tests "org.itsuka.log.store.AsyncLogRecordStoreTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store/Slf4jLogRecordStore.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/store/AsyncLogRecordStore.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/LogRecordAsyncExecutor.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/store
git commit -m "feat(log): add SLF4J and async stores"
```

---

### Task 10: 安全、Web 与 Jackson 适配器

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/SecurityLogOperatorProvider.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/ServletLogRequestResolver.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/JacksonLogParameterSerializer.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support/SecurityLogOperatorProviderTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support/ServletLogRequestResolverTest.java`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support/JacksonLogParameterSerializerTest.java`

**Interfaces:**
- Consumes: `LogOperatorProvider`, `LogOperator`, `LogRequestResolver`, `LogParameterSerializer`, `LogRecordProperties`, `LoginUser`。
- Produces：
  - `SecurityLogOperatorProvider(LogRecordProperties)`，实现 `LogOperatorProvider`。
  - `ServletLogRequestResolver(LogRecordProperties)`，实现 `LogRequestResolver`。
  - `JacksonLogParameterSerializer(JsonMapper, LogRecordProperties)`，实现 `LogParameterSerializer`。

- [ ] **Step 1: 写失败测试**

`SecurityLogOperatorProviderTest`：

```java
package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.core.domain.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityLogOperatorProviderTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsLoginUser() {
        LoginUser user = new LoginUser();
        user.setId(42L);
        user.setUsername("itsuka");
        user.setNickname("Itsuka");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));

        LogOperator operator = new SecurityLogOperatorProvider(new LogRecordProperties()).getOperator();
        assertThat(operator.id()).isEqualTo("42");
        assertThat(operator.name()).isEqualTo("Itsuka");
    }

    @Test
    void fallsBackWhenNoAuthentication() {
        LogOperator operator = new SecurityLogOperatorProvider(new LogRecordProperties()).getOperator();
        assertThat(operator.id()).isEqualTo("system");
    }
}
```

`ServletLogRequestResolverTest`：

```java
package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ServletLogRequestResolverTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void readsRemoteAddrAndUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ServletLogRequestResolver resolver = new ServletLogRequestResolver(new LogRecordProperties());
        assertThat(resolver.getClientIp()).isEqualTo("10.0.0.1");
        assertThat(resolver.getUserAgent()).isEqualTo("JUnit");
    }

    @Test
    void trustsForwardedHeadersWhenEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LogRecordProperties props = new LogRecordProperties();
        props.getRequest().setTrustForwardedHeaders(true);
        assertThat(new ServletLogRequestResolver(props).getClientIp()).isEqualTo("1.2.3.4");
    }
}
```

`JacksonLogParameterSerializerTest`：

```java
package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JacksonLogParameterSerializerTest {

    record Param(String orderNo, int count) {}
    static class SampleService {
        public void update(Param param, int count) {}
    }

    @Test
    void serializesArguments() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        JacksonLogParameterSerializer serializer =
            new JacksonLogParameterSerializer(JsonMapper.builder().build(), new LogRecordProperties());
        String json = serializer.serialize(method, new Object[]{new Param("NO1", 2), 2});
        assertThat(json).contains("NO1").contains("2");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.support.SecurityLogOperatorProviderTest" --tests "org.itsuka.log.support.ServletLogRequestResolverTest" --tests "org.itsuka.log.support.JacksonLogParameterSerializerTest"`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现**

- `SecurityLogOperatorProvider`：从 `SecurityContextHolder` 取 `Authentication`；未认证/为空返回默认；principal 是 `LoginUser` 时 id/name 按 spec；其他用 `authentication.getName()`；异常捕获后返回默认。
- `ServletLogRequestResolver`：`RequestContextHolder` + `ServletRequestAttributes`；无请求返回 null；forwarded 头按 spec；UA 截断 512。
- `JacksonLogParameterSerializer`：用 `JsonMapper.writeValueAsString` 序列化参数数组；跳过 `HttpServletRequest`/`HttpServletResponse`/`MultipartFile`；异常回退 `Arrays.deepToString`；截断到 `request.paramsMaxLength`。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.support.SecurityLogOperatorProviderTest" --tests "org.itsuka.log.support.ServletLogRequestResolverTest" --tests "org.itsuka.log.support.JacksonLogParameterSerializerTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/SecurityLogOperatorProvider.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/ServletLogRequestResolver.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/support/JacksonLogParameterSerializer.java common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/support
git commit -m "feat(log): add security, servlet and Jackson adapters"
```

---

### Task 11: 自动配置与条件装配

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordAutoConfiguration.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordSecurityAutoConfiguration.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordWebAutoConfiguration.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordJdbcAutoConfiguration.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordSlf4jAutoConfiguration.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordStorageValidator.java`
- Create: `common/itsuka-log-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/LogRecordAutoConfigurationTest.java`

**Interfaces:**
- Consumes: 前面所有组件。
- Produces：可被 Spring Boot 自动加载的 5 个自动配置类。

- [ ] **Step 1: 写失败测试**

```java
package org.itsuka.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsuka.log.core.LogRecordAspect;
import org.itsuka.log.query.OperationLogQueryService;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class LogRecordAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            LogRecordSecurityAutoConfiguration.class,
            LogRecordWebAutoConfiguration.class,
            LogRecordJdbcAutoConfiguration.class,
            LogRecordSlf4jAutoConfiguration.class,
            LogRecordAutoConfiguration.class
        ));

    @Test
    void disabledRegistersNothing() {
        runner.withPropertyValues("itsuka.log.enabled=false")
            .run(ctx -> {
                assertThat(ctx).doesNotHaveBean(LogRecordAspect.class);
                assertThat(ctx).doesNotHaveBean(LogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
            });
    }

    @Test
    void slf4jStorageDoesNotRegisterJdbcBeans() {
        runner.withPropertyValues("itsuka.log.storage=slf4j")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(LogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(JdbcLogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(LogSchemaInitializer.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
            });
    }

    @Test
    void customStoreBacksOffBuiltIns() {
        runner.withBean(LogRecordStore.class, () -> log -> {})
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(LogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(JdbcLogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(LogSchemaInitializer.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
            });
    }

    @Test
    void missingDataSourceFailsFast() {
        runner.withPropertyValues("itsuka.log.storage=jdbc")
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure())
                    .hasStackTraceContaining("LogRecordStore");
            });
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.LogRecordAutoConfigurationTest"`
Expected: FAIL，自动配置类不存在。

- [ ] **Step 3: 实现**

- `LogRecordAutoConfiguration`：
  - `@AutoConfiguration(after = {LogRecordSecurityAutoConfiguration.class, LogRecordWebAutoConfiguration.class})`
  - `@ConditionalOnProperty(prefix="itsuka.log", name="enabled", havingValue="true", matchIfMissing=true)`
  - `@EnableConfigurationProperties(LogRecordProperties.class)`
  - 所有注入 `LogRecordProperties` 的自动配置类都加 `@EnableConfigurationProperties(LogRecordProperties.class)`（重复注册无害，避免自动配置处理顺序问题）。
  - 初始化时调用 `LogRecordProperties.validate()`（`@PostConstruct` 或构造器均可）。
  - Beans：`LogRecordOperationParser`, `LogRecordExpressionEvaluator`, `LogRecordValueParser`, `LogRecordFunctionFactory`（注入 `List<LogRecordFunction>`）, `DefaultLogOperatorProvider`（`@ConditionalOnMissingBean`）, `NoopLogRequestResolver`（`@ConditionalOnMissingBean`）, `ToStringLogParameterSerializer`（`@ConditionalOnMissingBean`）, `LogRecordAspect`（store 必须存在，否则抛 `IllegalStateException`；消息含当前 storage 与排查建议）。
  - `async.enabled=true` 时注册 `ThreadPoolTaskExecutor` Bean，name `itsukaLogExecutor`。
- `LogRecordSecurityAutoConfiguration`：`before=LogRecordAutoConfiguration`，`@ConditionalOnClass(name="org.springframework.security.core.context.SecurityContextHolder")`，`enabled` 条件，`@ConditionalOnMissingBean(LogOperatorProvider.class)` 注册 `SecurityLogOperatorProvider`。
- `LogRecordWebAutoConfiguration`：`before`，`@ConditionalOnClass(name="org.springframework.web.context.request.RequestContextHolder")`，`@ConditionalOnWebApplication(SERVLET)`，`enabled` 条件；注册 `ServletLogRequestResolver`；嵌套 Jackson 配置注册 `JacksonLogParameterSerializer`。
- `LogRecordJdbcAutoConfiguration`：
  - `afterName = {"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration"}`
  - `@ConditionalOnClass(JdbcTemplate.class)`
  - `@ConditionalOnProperty(name="itsuka.log.storage", havingValue="jdbc", matchIfMissing=true)`
  - `@ConditionalOnProperty(prefix="itsuka.log", name="enabled", havingValue="true", matchIfMissing=true)`
  - `@ConditionalOnSingleCandidate(DataSource.class)`
  - 类级 `@ConditionalOnMissingBean(LogRecordStore.class)`
  - 存在 `PlatformTransactionManager` 且 `transaction.requiresNew=true` 时，构造 `TransactionTemplate`（`PROPAGATION_REQUIRES_NEW`）传给 `JdbcLogRecordStore`；否则传 null。
  - 注册 `JdbcLogRecordStore`（async 时用 `AsyncLogRecordStore` 包装）、`LogSchemaInitializer`（`auto-init-schema=true`）、`JdbcOperationLogQueryService`（`query.enabled=true` + `@ConditionalOnMissingBean(OperationLogQueryService.class)`）。
- `LogRecordSlf4jAutoConfiguration`：`storage=slf4j`，`@ConditionalOnProperty(prefix="itsuka.log", name="enabled", havingValue="true", matchIfMissing=true)`，类级 `@ConditionalOnMissingBean(LogRecordStore.class)`，注册 `Slf4jLogRecordStore`（async 时包装）。
- `LogRecordStorageValidator`：静态方法 `require(ObjectProvider<LogRecordStore>, LogRecordProperties)`，无 store 时抛 `IllegalStateException`，消息包含 `itsuka.log.storage`、`DataSource`、`spring-boot-starter-jdbc`。
- `AutoConfiguration.imports` 按顺序写入 5 个类。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.LogRecordAutoConfigurationTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecord*AutoConfiguration.java common/itsuka-log-spring-boot-starter/src/main/java/org/itsuka/log/LogRecordStorageValidator.java common/itsuka-log-spring-boot-starter/src/main/resources/META-INF/spring common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/LogRecordAutoConfigurationTest.java
git commit -m "feat(log): add auto-configuration and conditional beans"
```

---

### Task 12: MySQL 集成测试与全量构建

**Files:**
- Create: `common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/MySqlSchemaIT.java`
- Modify: 无（只新增测试）。

**Interfaces:**
- Consumes: `LogSchemaInitializer`, `JdbcLogRecordStore`, `JdbcOperationLogQueryService`。
- Produces: 生产 DDL 在 MySQL 8 上的验证。

- [ ] **Step 1: 写测试**

```java
package org.itsuka.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.domain.OperationLogPage;
import org.itsuka.log.domain.OperationLogQuery;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.query.JdbcOperationLogQueryService;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MySqlSchemaIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test
    void createsSchemaSavesAndQueries() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        LogRecordProperties props = new LogRecordProperties();
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();

        JdbcLogRecordStore store = new JdbcLogRecordStore(jdbc, props, null);
        store.save(OperationLog.builder()
            .bizNo("NO1").type("ORDER").success(true)
            .content("订单创建").createdAt(LocalDateTime.now()).build());

        JdbcOperationLogQueryService query = new JdbcOperationLogQueryService(
            new NamedParameterJdbcTemplate(ds), props);
        OperationLogQuery q = new OperationLogQuery();
        q.setBizNo("NO1");
        OperationLogPage page = query.page(q);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords().get(0).getContent()).isEqualTo("订单创建");
    }
}
```

- [ ] **Step 2: 运行 MySQL 集成测试**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test --tests "org.itsuka.log.MySqlSchemaIT"`
Expected: Docker 可用时 PASS；不可用时 SKIP。

- [ ] **Step 3: 运行模块全量测试**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:test`
Expected: PASS。

- [ ] **Step 4: 运行构建**

Run: `./gradlew :common:itsuka-log-spring-boot-starter:build`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: 提交**

```bash
git add common/itsuka-log-spring-boot-starter/src/test/java/org/itsuka/log/MySqlSchemaIT.java
git commit -m "test(log): verify MySQL schema and query with Testcontainers"
```

---

## 自检记录

- 规格覆盖：模块/注解/上下文/SpEL/函数/AOP/操作人/请求信息/存储/查询/建表/配置/自动配置/测试均有对应 Task。
- 类型一致性：`LogRecordOperation`、`LogRecordValueParser`、`LogRecordStore`、`OperationLogQueryService` 的签名在各 Task 的 Interfaces 中保持一致。
- Review Focus 对应：异常保留 → Task 6；自定义存储退让 → Task 11；超长字段截断 → Task 7；分页 null/超限 → Task 8；缺 DataSource 快速失败 → Task 11。
- 计划长度：按任务拆分，避免把实现体写成代码转录；测试和关键签名保留。

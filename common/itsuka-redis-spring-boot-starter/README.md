# Itsuka Redis Spring Boot Starter

`Itsuka Redis Spring Boot Starter` 提供了对 `Redis` 的自动化配置和便捷操作，集成了 `Redisson` 作为分布式锁的实现。

## 功能概览
- **自动化配置**: 自动配置 `RedisTemplate` 和 `RedissonClient`。
- **便捷操作**: 封装了常用的 `Redis` 操作，如 `set`, `get`, `delete` 等。
- **分布式锁**: 基于 `Redisson` 实现的分布式锁，保证分布式环境下的数据一致性。

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-redis-spring-boot-starter')
}
```

## 配置说明

在 `application.yml` 中配置 `Redis`：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your-password
```

## 使用示例

**1. 注入 RedisTemplate**

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void set(String key, Object value) {
    redisTemplate.opsForValue().set(key, value);
}

public Object get(String key) {
    return redisTemplate.opsForValue().get(key);
}
```

**2. 使用分布式锁**

```java
@Autowired
private RedissonClient redissonClient;

public void doSomethingWithLock() {
    RLock lock = redissonClient.getLock("myLock");
    try {
        lock.lock();
        // do something
    } finally {
        lock.unlock();
    }
}
```

## 注意事项
- 确保 `Redis` 服务已正确安装并正在运行。
- 分布式锁使用完毕后，务必在 `finally` 块中释放锁。
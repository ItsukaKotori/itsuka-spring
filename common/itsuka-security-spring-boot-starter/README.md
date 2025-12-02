# itsuka-security-spring-boot-starter

安全框架启动器，提供 JWT 令牌管理和访问控制功能。

## 功能特性

- JWT 令牌生成和验证
- 令牌黑名单管理（支持本地存储和 Redis 分布式存储）
- 访问控制和权限管理
- 密码编码和验证

## 令牌黑名单配置

### 本地存储（默认）

```yaml
itsuka:
  security:
    jwt:
      blacklist:
        enabled: true
        storage-type: LOCAL
        redis-key-prefix: "itsuka:token:blacklist:"
```

### Redis 分布式存储

```yaml
itsuka:
  security:
    jwt:
      blacklist:
        enabled: true
        storage-type: REDIS
        redis-key-prefix: "itsuka:token:blacklist:"

spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## 分布式环境支持

在分布式环境中，建议使用 Redis 存储模式，具有以下优势：

1. **数据一致性**：所有应用实例共享同一个黑名单
2. **自动过期**：利用 Redis TTL 机制自动清理过期令牌
3. **高性能**：Redis 内存存储，读写速度快
4. **可扩展性**：支持水平扩展，无需额外配置

## 使用示例

```java
@Autowired
private TokenBlacklistManager tokenBlacklistManager;

// 添加令牌到黑名单
tokenBlacklistManager.addToBlacklist(token, expirationTime);

// 检查令牌是否在黑名单中
boolean isBlacklisted = tokenBlacklistManager.isBlacklisted(token);

// 从黑名单移除令牌
tokenBlacklistManager.removeFromBlacklist(token);

// 获取黑名单大小
int size = tokenBlacklistManager.getBlacklistSize();

// 清空黑名单
tokenBlacklistManager.clearBlacklist();
```

## 配置参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| itsuka.security.jwt.blacklist.enabled | true | 是否启用黑名单功能 |
| itsuka.security.jwt.blacklist.storage-type | LOCAL | 存储类型：LOCAL 或 REDIS |
| itsuka.security.jwt.blacklist.redis-key-prefix | itsuka:token:blacklist: | Redis 键前缀 |

## 迁移指南

从本地存储迁移到 Redis 分布式存储：

1. 在配置文件中设置 `storage-type: REDIS`
2. 配置 Redis 连接参数
3. 重启应用

系统会自动检测 Redis 可用性，如果 Redis 不可用会自动回退到本地存储模式。
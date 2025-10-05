# itsuka-redis-spring-boot-starter

为Spring Boot应用提供完整的Redis支持，包括缓存、数据库切换、响应式编程等高级功能。

## 🎯 核心功能

### 📋 Redis模板配置
- 自动配置`RedisTemplate`和`StringRedisTemplate`
- 支持JSON序列化的值存储
- 优化的键存储格式

### 💾 智能缓存管理
- 自动配置`CacheManager`
- 支持TTL随机化，防止缓存雪崩
- 缓存穿透保护(`@Cacheable(sync = true)`)

### 🔀 数据库动态切换
- 支持通过`@DbSwitch`注解切换Redis数据库
- 兼容Lettuce和Jedis客户端
- 支持单机、哨兵模式（集群不支持）

### ⚡ 响应式编程支持
- 自动配置`ReactiveRedisTemplate`
- 支持非阻塞Redis操作

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'org.itsuka:itsuka-redis-spring-boot-starter:1.0.0'
}
```

### 2. 配置说明

```yaml
spring:
  cache:
    type: redis  # 启用Redis缓存
  redis:
    host: localhost
    port: 6379
    password: your-password
    database: 0
    
itsuka:
  cache:
    enable: true
    ttl: 3600  # 默认TTL（秒）
    random-ttl:
      enable: true  # 启用TTL随机化
      base-ttl: 3600
      float-ttl: 300  # 随机波动范围
```

### 3. 使用示例

#### 基本使用

```java
@Service
public class UserService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    public void saveUser(User user) {
        redisTemplate.opsForValue().set("user:" + user.getId(), user, Duration.ofMinutes(30));
    }
    
    public User getUser(Long id) {
        return (User) redisTemplate.opsForValue().get("user:" + id);
    }
    
    public void deleteUser(Long id) {
        redisTemplate.delete("user:" + id);
    }
}
```

#### 缓存注解使用

```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id", sync = true)  // 缓存穿透保护
    public Product getProduct(Long id) {
        // 从数据库加载
        return productRepository.findById(id);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
    
    @CachePut(value = "products", key = "#result.id")
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
}
```

#### 数据库切换

```java
@Service
public class SessionService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 切换到数据库1存储会话信息
    @DbSwitch(database = 1)
    public void saveSession(String sessionId, Object sessionData) {
        redisTemplate.opsForValue().set("session:" + sessionId, sessionData, Duration.ofHours(2));
    }
    
    @DbSwitch(database = 1)
    public Object getSession(String sessionId) {
        return redisTemplate.opsForValue().get("session:" + sessionId);
    }
    
    // 切换到数据库2存储用户状态
    @DbSwitch(database = 2)
    public void saveUserStatus(Long userId, String status) {
        stringRedisTemplate.opsForValue().set("user:status:" + userId, status);
    }
}
```

#### 响应式编程

```java
@Service
public class ReactiveUserService {
    
    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    public Mono<Void> saveUserAsync(User user) {
        return reactiveRedisTemplate.opsForValue()
                .set("user:" + user.getId(), user, Duration.ofMinutes(30))
                .then();
    }
    
    public Mono<User> getUserAsync(Long id) {
        return reactiveRedisTemplate.opsForValue()
                .get("user:" + id)
                .cast(User.class);
    }
    
    public Flux<String> getAllUserKeys() {
        return reactiveRedisTemplate.keys("user:*");
    }
}
```

## 🔧 高级功能

### TTL随机化

防止缓存雪崩，通过随机化TTL值避免大量缓存同时过期：

```yaml
itsuka:
  cache:
    random-ttl:
      enable: true
      base-ttl: 3600      # 基础TTL：3600秒
      float-ttl: 300      # 波动范围：±300秒
      # 实际TTL为：3300-3900秒间的随机值
```

### 数据库切换原理

通过AOP拦截`@DbSwitch`注解，动态创建指定数据库的连接工厂：

```java
@DbSwitch(database = 2)
public void doSomething() {
    // 此方法内的所有Redis操作都会使用数据库2
    redisTemplate.opsForValue().set("key", "value");
}
```

### 序列化配置

自动配置优化的序列化方式：
- **Key序列化**：使用`StringRedisSerializer`
- **Value序列化**：使用`GenericJackson2JsonRedisSerializer`
- **Hash序列化**：与Value保持一致

## 📚 依赖和兼容性

### 依赖模块

- `spring-boot-starter-data-redis` - Redis基础支持
- `spring-boot-starter-cache` - 缓存抽象
- `spring-boot-starter-aop` - AOP支持
- `itsuka-web-spring-boot-starter` - Web工具类

### 支持的Redis客户端

- **Lettuce** (推荐)：支持所有功能
- **Jedis**：支持数据库切换功能

### 支持的Redis模式

- ✅ **单机模式**：支持所有功能
- ✅ **哨兵模式**：支持数据库切换
- ❌ **集群模式**：不支持数据库切换

## ⚠️ 注意事项

1. **数据库切换限制**：
   - Redis集群模式不支持数据库切换
   - `@DbSwitch`注解只在方法级别生效

2. **性能考虑**：
   - TTL随机化会轻微增加CPU开销
   - 数据库切换会创建新的连接工厂

3. **缓存策略**：
   - 使用`sync = true`防止缓存穿透
   - 缓存空值默认被禁用

4. **安全考虑**：
   - 生产环境建议设置Redis密码
   - 考虑使用SSL/TLS加密连接
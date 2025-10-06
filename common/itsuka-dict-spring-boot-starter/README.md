# itsuka-dict-spring-boot-starter

为Spring Boot应用提供智能字典管理功能，支持自动加载、缓存管理和响应处理中的字典值转换。

## 🎯 核心功能

### 📊 智能字典转换
- 通过`@DictField`注解自动转换字典值
- 支持覆盖原值或增加新字段
- 支持默认值配置
- 支持字典前缀配置

### 📋 多种缓存策略
- **本地缓存**：基于ConcurrentHashMap的线程安全缓存
- **Redis缓存**：支持分布式缓存，多实例共享
- 缓存自动管理和清理

### 📏 数据源自动加载
- 支持从数据库自动加载字典数据
- 支持MySQL、Oracle、PostgreSQL等数据库
- 自动创建字典表（如果不存在）
- 支持自定义数据库方言

### ⚡ 响应拦截处理
- 自动拦截所有HTTP响应
- 支持复杂对象、集合、Map等类型
- 递归处理嵌套对象

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'org.itsuka:itsuka-dict-spring-boot-starter:1.0.0'
}
```

### 2. 配置说明

```yaml
itsuka:
  dict:
    enable: true  # 启用字典功能
    type: REDIS  # 缓存类型：LOCAL或REDIS
    auto-load: true  # 是否自动从数据库加载字典
    database-type: mysql  # 数据库类型
    datasource:  # 字典数据库配置（可选）
      url: jdbc:mysql://localhost:3306/dict_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
```

### 3. 使用示例

#### 基础字典转换

```java
// 实体类定义
@DictModel(prefix = "user")  // 字典前缀
@Data
public class User {
    private Long id;
    
    private String username;
    
    // 状态字典转换
    @DictField(key = "status", defaultValue = "未知")
    private Integer status;
    
    // 类型字典转换，不覆盖原值
    @DictField(key = "type", override = false)
    private String userType;
    
    // 部门字典转换
    @DictField(key = "dept")
    private Long deptId;
}

// Controller使用
@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public Response<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        // 返回时会自动转换字典值
        return Response.success(user);
    }
    
    @GetMapping("/users")
    public Response<List<User>> getUsers() {
        List<User> users = userService.list();
        // 支持集合类型的字典转换
        return Response.success(users);
    }
}
```

#### 响应结果示例

```json
// 原始数据
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "status": 1,
    "userType": "admin",
    "deptId": 10
  }
}

// 字典转换后
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "status": "正常",  // 转换后的状态文本
    "userType": "admin",
    "userTypeText": "管理员",  // 新增的文本字段
    "deptId": "技术部"  // 转换后的部门名称
  }
}
```

#### 缓存管理

```java
@Service
public class DictService {
    
    @Autowired
    private DictCacheManager cacheManager;
    
    /**
     * 手动设置字典值
     */
    public void setDictValue(String key, String value) {
        cacheManager.put(key, value);
    }
    
    /**
     * 获取字典值
     */
    public String getDictValue(String key) {
        return cacheManager.get(key, String.class);
    }
    
    /**
     * 清空所有字典缓存
     */
    public void clearCache() {
        cacheManager.clear();
    }
}
```

## 🔧 高级功能

### 字典数据模型

如果启用了自动加载，系统会自动创建以下表结构：

```sql
-- 字典类型表
CREATE TABLE dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    status CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) COMMENT '备注'
);

-- 字典数据表
CREATE TABLE dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_sort INT DEFAULT 0 COMMENT '字典排序',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    css_class VARCHAR(100) COMMENT '样式属性',
    list_class VARCHAR(100) COMMENT '表格样式',
    is_default CHAR(1) DEFAULT 'N' COMMENT '是否默认',
    status CHAR(1) DEFAULT '0' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) COMMENT '备注'
);
```

### 缓存键格式

字典缓存键的格式为：`{prefix}:{dictKey}:{dictCode}`

```java
// 示例：
// @DictModel(prefix = "user") + @DictField(key = "status") + 字段值1
// 生成的缓存键：user:status:1

// 无前缀的情况：
// @DictField(key = "gender") + 字段值"M"
// 生成的缓存键：gender:M
```

### 自定义数据库方言

```java
// 自定义数据库方言实现
public class CustomDialect implements DatabaseDialect {
    
    @Override
    public String getCreateDictTypeTableSql() {
        return "CREATE TABLE IF NOT EXISTS dict_type (...)";
    }
    
    @Override
    public String getCreateDictDataTableSql() {
        return "CREATE TABLE IF NOT EXISTS dict_data (...)";
    }
    
    @Override
    public String getQueryDictDataSql() {
        return "SELECT dict_type, dict_value, dict_label FROM dict_data WHERE status = '0'";
    }
}

// 配置使用
itsuka:
  dict:
    dialect-class: com.example.CustomDialect
```

### 多类型支持

```java
// 支持复杂对象的字典转换
public class UserDetailVO {
    private User user;  // 嵌套对象
    private List<Role> roles;  // 集合对象
    private Map<String, Object> extra;  // Map对象
}

// 所有层级的@DictField注解都会被处理
@DictModel(prefix = "role")
public class Role {
    @DictField(key = "type")
    private String roleType;
}
```

## 📋 配置参数详解

### 主配置

| 参数 | 默认值 | 说明 |
|------|---------|------|
| `itsuka.dict.enable` | `true` | 是否启用字典功能 |
| `itsuka.dict.type` | `LOCAL` | 缓存类型：LOCAL/REDIS |
| `itsuka.dict.auto-load` | `false` | 是否自动从数据库加载 |
| `itsuka.dict.database-type` | - | 数据库类型：mysql/oracle/postgresql |
| `itsuka.dict.dialect-class` | - | 自定义数据库方言类全名 |

### 数据源配置

| 参数 | 说明 |
|------|---------|
| `itsuka.dict.datasource.url` | 数据库连接URL |
| `itsuka.dict.datasource.username` | 数据库用户名 |
| `itsuka.dict.datasource.password` | 数据库密码 |
| `itsuka.dict.datasource.driver-class-name` | 数据库驱动类名 |

## 📚 依赖模块

- `itsuka-spring-boot-starter` - 核心工具类
- `spring-boot-starter-web` - Web支持
- `spring-boot-starter-data-redis` - Redis支持（可选）

## ⚠️ 注意事项

1. **注解使用**：
   - `@DictModel`只能标注在类上，用于设置字典前缀
   - `@DictField`只能标注在字段上，用于指定字典键

2. **数据库配置**：
   - 如果不配置独立数据源，会使用主数据源
   - 数据库表会自动创建，但需要手动初始化数据

3. **性能考虑**：
   - Redis缓存适合多实例部署
   - 本地缓存性能更好，但不能跨实例共享

4. **字典管理**：
   - 建议通过管理后台统一管理字典数据
   - 字典变更后需要清空缓存才能生效

5. **错误处理**：
   - 字典值找不到时会使用默认值
   - 异常情况下不会影响正常业务流程
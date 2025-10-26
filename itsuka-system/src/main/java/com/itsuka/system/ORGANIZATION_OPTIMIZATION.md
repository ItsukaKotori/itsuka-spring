# 组织机构模块MyBatis-Plus优化说明

## 优化目标
根据项目要求，尽量使用MyBatis-Plus BaseMapper提供的方法，减少自定义SQL。

## 优化内容

### 1. Mapper层优化
**优化前**：OrganizationMapper包含多个自定义方法
- `selectByParentId()` - 根据父级ID查询子节点
- `selectAllForTree()` - 查询所有组织机构
- `selectByType()` - 根据类型查询组织机构
- `countByParentId()` - 检查是否存在子节点
- `selectByLeaderId()` - 根据负责人ID查询组织机构

**优化后**：只保留无法通过BaseMapper实现的方法
- `countByCode()` - 检查编码是否存在（需要排除特定ID的自定义SQL）

### 2. Service层优化
所有查询操作都改为使用MyBatis-Plus BaseMapper提供的方法：

#### 2.1 分页查询
```java
// 优化前
List<Organization> allOrganizations = organizationMapper.selectAllForTree();

// 优化后
LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
        .orderByAsc(Organization::getSortOrder)
        .orderByAsc(Organization::getId);
List<Organization> allOrganizations = list(queryWrapper);
```

#### 2.2 条件查询
```java
// 优化前
return organizationMapper.selectByParentId(parentId);

// 优化后
LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
        .eq(Organization::getParentId, parentId)
        .orderByAsc(Organization::getSortOrder)
        .orderByAsc(Organization::getId);
return list(queryWrapper);
```

#### 2.3 计数查询
```java
// 优化前
int childCount = organizationMapper.countByParentId(id);

// 优化后
LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
        .eq(Organization::getParentId, id);
long childCount = count(queryWrapper);
```

#### 2.4 类型查询
```java
// 优化前
return organizationMapper.selectByType(type);

// 优化后
LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
        .eq(Organization::getType, type)
        .orderByAsc(Organization::getSortOrder)
        .orderByAsc(Organization::getId);
return list(queryWrapper);
```

#### 2.5 单条查询
```java
// 优化前
Organization parent = organizationMapper.selectById(parentId);

// 优化后
Organization parent = getById(parentId);
```

#### 2.6 循环依赖解决
**优化前**：使用ApplicationContext延迟注入UserService
```java
private ApplicationContext applicationContext;
private UserService userService;

@PostConstruct
public void init() {
    this.userService = applicationContext.getBean(UserService.class);
}
```

**优化后**：直接注入UserMapper避免循环依赖
```java
private final UserMapper userMapper;

// 使用UserMapper直接查询
LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
        .eq(User::getOrganizationId, organizationId);
return userMapper.selectList(queryWrapper);
```

### 3. XML映射文件优化
**优化前**：包含多个自定义SQL查询语句
**优化后**：只保留`countByCode`方法的SQL，其他查询都通过BaseMapper实现

## 优化效果

### 优点
1. **代码一致性**：统一使用MyBatis-Plus的查询方式
2. **维护性**：减少自定义SQL，降低维护成本
3. **类型安全**：使用Lambda表达式，编译时检查字段名
4. **性能**：利用MyBatis-Plus的优化机制

### 保留的自定义方法
只有`countByCode`方法保留自定义SQL，因为需要动态排除特定ID的复杂逻辑。

## 使用示例

### 基本查询
```java
// 查询所有
List<Organization> all = list();

// 根据ID查询
Organization org = getById(1L);

// 条件查询
LambdaQueryWrapper<Organization> wrapper = Wrappers.lambdaQuery(Organization.class)
    .eq(Organization::getType, "DEPARTMENT")
    .like(Organization::getName, "技术")
    .orderByAsc(Organization::getSortOrder);
List<Organization> departments = list(wrapper);
```

### 分页查询
```java
Page<Organization> page = page(new Page<>(1, 10), wrapper);
```

### 计数查询
```java
long count = count(wrapper);
```

### 循环依赖解决
通过直接注入Mapper而不是Service来避免循环依赖，这是更简洁的解决方案。

这种优化方式充分利用了MyBatis-Plus BaseMapper的强大功能，同时保持了代码的简洁性和可维护性。
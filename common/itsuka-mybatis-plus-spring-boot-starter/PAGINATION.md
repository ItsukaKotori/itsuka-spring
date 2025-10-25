# 自定义分页对象使用说明

## 概述

为了简化前后端交互，我们创建了自定义的分页对象 `PageParam` 和 `PageResult`，用于替换 MyBatis Plus 的 `Page` 对象在接口层的暴露。

## 架构设计

```
前端请求 → Controller (PageParam/PageResult) → Service (PageParam/PageResult) → ServiceImpl (内部使用MyBatis Plus Page) → 数据库
```

- **Controller/Service 接口层**: 使用自定义的 PageParam 和 PageResult
- **ServiceImpl 实现层**: 内部使用 MyBatis Plus Page，通过 PageUtil 转换

## 对象说明

### PageParam - 分页参数

```java
@Data
public class PageParam {
    private Long current = 1L;  // 当前页码，从1开始
    private Long size = 10L;    // 每页大小
}
```

### PageResult - 分页结果

```java
@Data
public class PageResult<T> {
    private List<T> records;  // 数据列表
    private Long current;     // 当前页码
    private Long size;        // 每页大小
    private Long total;       // 总记录数
    private Long pages;       // 总页数
}
```

## 使用方法

### 1. Controller 层

```java
@RestController
public class UserController {
    
    @GetMapping("/page")
    public Response<PageResult<User>> page(PageParam pageParam, UserQueryCriteria criteria) {
        return Response.success(userService.pageUser(pageParam, criteria));
    }
}
```

### 2. Service 接口层

```java
public interface UserService {
    PageResult<User> pageUser(PageParam pageParam, UserQueryCriteria criteria);
}
```

### 3. ServiceImpl 实现层

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public PageResult<User> pageUser(PageParam pageParam, UserQueryCriteria criteria) {
        // 转换为 MyBatis Plus 的 Page 对象
        Page<User> page = page(PageUtil.toPage(pageParam), getWrapper(criteria));
        // 转换回 PageResult
        return PageUtil.toResult(page);
    }
}
```

## 工具类 PageUtil

```java
// 基础转换
Page<User> mybatisPage = PageUtil.toPage(pageParam);

// 带排序的转换
Page<User> mybatisPage = PageUtil.toPage(pageParam, "createTime", false);

// 将 MyBatis Plus Page 转换为 PageResult
PageResult<User> result = PageUtil.toResult(mybatisPage);
```

## 前端交互格式

### 请求参数
```json
{
  "current": 1,
  "size": 10
}
```

### 响应结果
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "current": 1,
    "size": 10,
    "total": 100,
    "pages": 10
  }
}
```

## 已完成的替换

### Controllers
- ✅ UserController.java
- ✅ RoleController.java  
- ✅ ResourceController.java

### Services
- ✅ UserService.java
- ✅ RoleService.java
- ✅ ResourceService.java

### ServiceImpls
- ✅ UserServiceImpl.java
- ✅ RoleServiceImpl.java
- ✅ ResourceServiceImpl.java

## 优势

1. **简化交互**: 只包含前后端需要的必要字段
2. **统一规范**: 所有分页接口使用相同的分页对象
3. **易于扩展**: 可以根据需要添加额外的分页相关字段
4. **向后兼容**: 内部仍使用 MyBatis Plus Page，保持性能
5. **清晰分层**: 接口层使用自定义对象，实现层使用原生对象

## 迁移指南

1. 将 Controller 中的 `Page<Entity>` 替换为 `PageParam` 参数和 `PageResult<Entity>` 返回值
2. 更新 Service 接口和实现类
3. 使用 `PageUtil` 工具类进行对象转换
4. 更新相关的测试用例
5. ServiceImpl 中保留 MyBatis Plus Page 的导入，因为内部仍需要使用

## 注意事项

- ServiceImpl 实现类中仍需要导入 `com.baomidou.mybatisplus.extension.plugins.pagination.Page`，因为需要使用 MyBatis Plus 的 `page()` 方法
- 转换逻辑集中在 PageUtil 工具类中，便于维护和统一修改
- 自定义分页对象放置在 mybatis-plus-starter 中，便于其他模块复用
# Itsuka Core

核心工具与通用模型模块，为整个项目提供基础能力支撑。

## 功能概览
- **通用工具类**: 提供字符串处理、日期转换、集合操作等常用工具。
- **公共模型**: 定义了项目统一的 `Response` 返回对象、分页模型 `PageResult` 等。
- **统一异常处理**: 包含了基础的 `ServiceException` 和全局异常处理机制的初步定义。

## 安装步骤
在需要使用此模块的 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-core')
}
```

## 使用示例

**统一返回对象**

```java
import org.itsuka.core.domain.Response;

// 成功返回
Response.success("操作成功");

// 失败返回
Response.fail("操作失败");
```

## 注意事项
- `itsuka-core` 是项目的基础，几乎所有其他模块都依赖于它。
- 请勿在此模块中添加任何与业务相关的代码。
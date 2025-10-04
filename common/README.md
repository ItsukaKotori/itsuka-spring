# Common 模块

聚合父模块，用于组织与管理所有通用子模块，不包含任何业务代码或可直接运行的产物。

## 功能概览
- **统一依赖管理**: 在此模块中统一管理所有 `common` 子模块的依赖版本。
- **模块聚合**: 作为所有通用 `Starter` 和 `core` 模块的父级，简化构建流程。

## 子模块列表
- `itsuka-core`: 核心工具与通用模型。
- `itsuka-dict-spring-boot-starter`: 数据字典 `Starter`。
- `itsuka-file-spring-boot-starter`: 文件操作 `Starter`。
- `itsuka-mqtt-spring-boot-starter`: MQTT `Starter`。
- `itsuka-mybatis-plus-spring-boot-starter`: MyBatis Plus `Starter`。
- `itsuka-redis-spring-boot-starter`: Redis `Starter`。
- `itsuka-security-spring-boot-starter`: 安全认证 `Starter`。
- `itsuka-web-spring-boot-starter`: Web 功能 `Starter`。

## 注意事项
- 本模块不生成任何 `JAR` 包，仅用于项目结构组织和统一构建。
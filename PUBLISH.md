# Nexus 发布配置说明

本文档说明如何将项目发布到自建的 Nexus 仓库。

## 配置文件

### nexus.properties

在项目根目录下创建`nexus.properties`文件，配置 Nexus 仓库信息：

```properties
# Nexus服务器配置
nexus.url=http://nexus.example.com:8081
nexus.repository.releases=http://nexus.example.com:8081/repository/maven-releases/
nexus.repository.snapshots=http://nexus.example.com:8081/repository/maven-snapshots/

# Nexus认证信息
nexus.username=admin
nexus.password=admin123
```

**注意**: `nexus.properties`文件已添加到`.gitignore`中，不会提交到版本控制系统，确保敏感信息安全。

## 发布操作

### 使用发布脚本（推荐）

```bash
# 发布单个模块
./publish.sh

# 批量发布所有starter模块
./publish-all.sh
```

### 使用 Gradle 命令

```bash
# 清理并构建项目
./gradlew clean build

# 发布到Nexus仓库
./gradlew publish

# 或者一步完成
./gradlew clean build publish
```

### 发布特定模块

```bash
# 发布单个starter模块
./gradlew :common:itsuka-spring-boot-starter:publish

# 发布多个模块
./gradlew :common:itsuka-spring-boot-starter:publish :common:itsuka-web-spring-boot-starter:publish

# 发布所有starter模块
./gradlew $(./gradlew projects | grep 'itsuka-.*-spring-boot-starter' | sed 's/^Project /:/' | sed 's/$/:publish/' | tr '\n' ' ')
```

### 测试发布配置

```bash
# 干运行（不实际发布，只验证配置）
./gradlew publish --dry-run

# 发布到本地Maven仓库（用于测试）
./gradlew publishToMavenLocal
```

## 版本管理

- 发布版本（非 SNAPSHOT）将发布到 releases 仓库
- 快照版本（以 SNAPSHOT 结尾）将发布到 snapshots 仓库

版本号在`build.gradle`和`gradle.properties`中定义：

```properties
version=1.0.0
```

### 发布 SNAPSHOT 版本

```bash
# 修改版本号为SNAPSHOT版本
sed -i.bak 's/version=1.0.0/version=1.0.1-SNAPSHOT/' gradle.properties

# 发布SNAPSHOT版本
./gradlew publish
```

### 发布正式版本

```bash
# 修改版本号为正式版本
sed -i.bak 's/version=1.0.1-SNAPSHOT/version=1.0.1/' gradle.properties

# 发布正式版本
./gradlew publish
```

## 发布内容

每个模块发布包含：

1. 主 jar 包
2. 源码 jar 包（sources）
3. Javadoc jar 包（javadoc）
4. POM 文件

## 常见问题

### 1. 认证失败

检查`nexus.properties`中的用户名和密码是否正确。

### 2. 权限不足

确保 Nexus 用户有发布到相应仓库的权限。

### 3. 网络连接问题

确保可以访问 Nexus 服务器地址。

### 4. 版本冲突

确保要发布的版本在 Nexus 仓库中不存在，或者使用 SNAPSHOT 版本进行开发。

### 5. Javadoc 生成失败

项目已配置忽略 Javadoc 警告和错误，如果仍有问题，可以尝试：

```bash
# 跳过Javadoc生成
./gradlew publish -x javadocJar
```

## Nexus 仓库配置

确保 Nexus 中已创建以下仓库：

1. maven-releases - 用于发布正式版本
2. maven-snapshots - 用于发布快照版本

仓库类型应为：

- Format: maven2
- Policy: Release (releases 仓库) 或 Snapshot (snapshots 仓库)

## 自动化发布

### CI/CD 集成

可以在 CI/CD 流水线中使用以下命令：

```bash
# 设置Nexus认证信息
echo "nexus.username=$NEXUS_USERNAME" >> nexus.properties
echo "nexus.password=$NEXUS_PASSWORD" >> nexus.properties

# 发布项目
./gradlew publish
```

### 批量发布所有 starter 模块

```bash
#!/bin/bash
# 批量发布脚本

# 获取所有starter模块
modules=$(./gradlew projects | grep 'itsuka-.*-spring-boot-starter' | sed 's/^Project /:/' | sed 's/$/:publish/')

# 发布所有模块
./gradlew $modules
```

## 发布验证

### 验证本地发布

```bash
# 检查本地Maven仓库
ls -la ~/.m2/repository/org/itsuka/
```

### 验证 Nexus 发布

登录 Nexus 管理界面，检查以下位置：

- Releases: http://nexus.example.com:8081/#browse/components:maven-releases
- Snapshots: http://nexus.example.com:8081/#browse/components:maven-snapshots

## 高级配置

### 签名发布（可选）

如果需要 GPG 签名发布，可以配置：

```gradle
// 在build.gradle中添加
signing {
    sign publishing.publications.mavenJava
}
```

### 发布条件控制

可以根据条件控制发布行为：

```gradle
// 只在非SNAPSHOT版本时签名
if (!version.endsWith('SNAPSHOT')) {
    signing {
        sign publishing.publications.mavenJava
    }
}
```

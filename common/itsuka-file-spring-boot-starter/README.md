# Itsuka File Spring Boot Starter

文件操作 `Starter`，封装了对主流对象存储服务（如 MinIO）的常用操作，提供统一的文件上传、下载和管理接口。

## 功能概览
- **统一文件操作**: 提供 `FileOperator` 接口，封装了文件上传、下载、删除、获取链接等常用方法。
- **支持多种存储**: 默认集成 MinIO，并可通过扩展支持其他对象存储服务。
- **自动配置**: 根据配置文件自动初始化并注入 `FileOperator` 实例。

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-file-spring-boot-starter')
}
```

## 配置说明

在 `application.yml` 中配置 MinIO 服务信息：

```yaml
itsuka:
  file:
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: itsuka
```

## 使用示例

```java
import org.itsuka.file.FileOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Autowired
    private FileOperator fileOperator;

    public String uploadFile(MultipartFile file) throws Exception {
        return fileOperator.upload(file);
    }
}
```

## 注意事项
- 使用前请确保 `application.yml` 中的对象存储配置正确无误。
- 默认的 `bucket` 需要在对象存储服务中提前创建。
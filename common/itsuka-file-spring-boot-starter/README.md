# itsuka-file-spring-boot-starter

基于MinIO的文件存储Starter，提供统一的文件上传、下载、管理接口，支持对象存储服务的常用操作。

## 🎯 核心功能

### 📁 文件存储管理
- 支持文件上传、下载、删除
- 自动创建存储桶（Bucket）
- 支持指定存储桶或使用默认配置
- 返回完整的文件访问路径

### 🔗 预签名链接
- 生成带有效期的预签名下载链接
- 支持自定义有效期时间
- 安全的文件访问控制

### 📄 对象管理
- 列出存储桶和对象
- 批量文件操作
- 支持文件元数据管理

### ⚙️ 自动配置
- 自动初始化MinIO客户端
- 参数校验和错误处理
- 灵活的配置选项

## 📦 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-file-spring-boot-starter')
}
```

## ⚙️ 配置说明

在 `application.yml` 中配置 MinIO 服务信息：

```yaml
itsuka:
  file:
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: itsuka
      # 可选配置
      region: us-east-1
      path-style-access: true
      auto-create-bucket: true
```

### 配置参数说明
- `endpoint`: MinIO 服务器地址
- `access-key`: 访问密钥
- `secret-key`: 秘密密钥  
- `bucket`: 默认存储桶名称
- `region`: 区域设置（默认 us-east-1）
- `path-style-access`: 是否使用路径风格访问（默认 true）
- `auto-create-bucket`: 是否自动创建存储桶（默认 true）

## 💻 使用示例

### 基础文件操作

```java
import org.itsuka.file.MinioStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Autowired
    private MinioStorageClient storageClient;

    // 上传文件
    public String uploadFile(MultipartFile file) throws Exception {
        return storageClient.upload(file.getInputStream(), 
                                  file.getOriginalFilename(), 
                                  file.getContentType());
    }
    
    // 下载文件
    public InputStream downloadFile(String fileName) throws Exception {
        return storageClient.download(fileName);
    }
    
    // 删除文件
    public void deleteFile(String fileName) throws Exception {
        storageClient.delete(fileName);
    }
    
    // 获取文件预览URL
    public String getPreviewUrl(String fileName) throws Exception {
        return storageClient.getPreSignedUrl(fileName, 3600); // 1小时有效
    }
}
```

### 批量文件操作

```java
@Service
public class BatchFileService {
    
    @Autowired
    private MinioStorageClient storageClient;
    
    // 批量上传
    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                   .map(file -> {
                       try {
                           return storageClient.upload(file.getInputStream(),
                                                     file.getOriginalFilename(),
                                                     file.getContentType());
                       } catch (Exception e) {
                           throw new RuntimeException("文件上传失败", e);
                       }
                   })
                   .collect(Collectors.toList());
    }
    
    // 批量删除
    public void deleteFiles(List<String> fileNames) {
        fileNames.forEach(fileName -> {
            try {
                storageClient.delete(fileName);
            } catch (Exception e) {
                log.error("删除文件失败: {}", fileName, e);
            }
        });
    }
}
```

## ⚠️ 注意事项

### 配置要求
- 使用前请确保 `application.yml` 中的对象存储配置正确无误
- 默认的 `bucket` 需要在对象存储服务中提前创建（或启用 `auto-create-bucket`）
- MinIO 服务器必须可访问且凭据正确

### 安全考虑
- 生产环境中应使用强密码的 access-key 和 secret-key
- 建议使用 HTTPS 连接 MinIO 服务
- 考虑设置适当的文件大小和类型限制

### 性能优化
- 大文件上传建议使用分片上传
- 频繁访问的文件可考虑使用 CDN 加速
- 预签名 URL 有效期应根据实际需求设置
package org.itsuka.file.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置属性类
 * 用于从配置文件中读取MinIO相关配置
 *
 * @author Itsuka
 * @since 2023/11/10
 */
@Data
@ConfigurationProperties(prefix = "itsuka.minio")
public class MinioProperties {
    /**
     * MinIO服务端点URL
     */
    private String url;

    /**
     * 访问密钥（类似于用户名）
     */
    private String accessKey;

    /**
     * 密钥（类似于密码）
     */
    private String secretKey;

    /**
     * 默认桶名称
     */
    private String bucketName;
}
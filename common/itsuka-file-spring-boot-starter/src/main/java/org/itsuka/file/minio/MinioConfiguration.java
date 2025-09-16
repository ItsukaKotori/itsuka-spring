package org.itsuka.file.minio;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * MinIO 自动配置类
 * 自动配置MinIO客户端实例
 *
 * @author Itsuka
 * @since 2023/11/10
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
public class MinioConfiguration {
    private final MinioProperties minioProperties;

    /**
     * 创建MinIO客户端Bean
     *
     * @return MinioClient实例
     */
    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    public MinioClient minioClient() {
        validateProperties();
        
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    /**
     * MinioStorageClient
     *
     * @return
     */
    @Bean
    public MinioStorageClient storageClient() {
        return new MinioStorageClient(minioClient(), minioProperties);
    }
    
    /**
     * 验证配置属性
     */
    private void validateProperties() {
        if (!StringUtils.hasText(minioProperties.getUrl())) {
            throw new IllegalArgumentException("MinIO URL must be provided");
        }
        if (!StringUtils.hasText(minioProperties.getAccessKey())) {
            throw new IllegalArgumentException("MinIO access key must be provided");
        }
        if (!StringUtils.hasText(minioProperties.getSecretKey())) {
            throw new IllegalArgumentException("MinIO secret key must be provided");
        }
    }
}
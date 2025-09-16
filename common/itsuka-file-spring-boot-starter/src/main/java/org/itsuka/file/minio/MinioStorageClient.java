package org.itsuka.file.minio;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 服务类
 * 封装MinIO的常用操作
 *
 * @author Itsuka
 * @since 2023/11/10
 */
@RequiredArgsConstructor
public class MinioStorageClient {
    private final MinioClient client;

    private final MinioProperties properties;

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    public void createBucket(String bucketName) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param file 文件
     * @return 文件路径
     */
    public String uploadFile(String bucketName, String objectName, MultipartFile file) throws Exception {
        createBucket(bucketName);
        
        client.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        
        return String.format("%s/%s/%s", properties.getUrl(), bucketName, objectName);
    }

    /**
     * 上传文件（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @param file 文件
     * @return 文件路径
     */
    public String uploadFile(String objectName, MultipartFile file) throws Exception {
        String bucketName = StringUtils.hasText(properties.getBucketName())
                ? properties.getBucketName()
                : "default";
        return uploadFile(bucketName, objectName, file);
    }

    /**
     * 获取文件流
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 文件流
     */
    public InputStream getObject(String bucketName, String objectName) throws Exception {
        return client.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 获取文件流（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @return 文件流
     */
    public InputStream getObject(String objectName) throws Exception {
        String bucketName = StringUtils.hasText(properties.getBucketName())
                ? properties.getBucketName()
                : "default";
        return getObject(bucketName, objectName);
    }

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    public void deleteObject(String bucketName, String objectName) throws Exception {
        client.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除文件（使用默认存储桶）
     *
     * @param objectName 对象名称
     */
    public void deleteObject(String objectName) throws Exception {
        String bucketName = StringUtils.hasText(properties.getBucketName())
                ? properties.getBucketName()
                : "default";
        deleteObject(bucketName, objectName);
    }

    /**
     * 获取存储桶列表
     *
     * @return 存储桶列表
     */
    public List<Bucket> listBuckets() throws Exception {
        return client.listBuckets();
    }

    /**
     * 获取存储桶中的对象列表
     *
     * @param bucketName 存储桶名称
     * @return 对象列表
     */
    public List<Item> listObjects(String bucketName) throws Exception {
        List<Item> items = new ArrayList<>();
        Iterable<Result<Item>> results = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build()
        );
        for (Result<Item> result : results) {
            items.add(result.get());
        }
        return items;
    }

    /**
     * 获取存储桶中的对象列表（使用默认存储桶）
     *
     * @return 对象列表
     */
    public List<Item> listObjects() throws Exception {
        String bucketName = StringUtils.hasText(properties.getBucketName())
                ? properties.getBucketName()
                : "default";
        return listObjects(bucketName);
    }

    /**
     * 生成预签名URL
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expires 过期时间
     * @param timeUnit 时间单位
     * @return 预签名URL
     */
    public String getPresignedUrl(String bucketName, String objectName, int expires, TimeUnit timeUnit) throws Exception {
        return client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expires, timeUnit)
                        .build()
        );
    }

    /**
     * 生成预签名URL（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @param expires 过期时间
     * @param timeUnit 时间单位
     * @return 预签名URL
     */
    public String getPresignedUrl(String objectName, int expires, TimeUnit timeUnit) throws Exception {
        String bucketName = StringUtils.hasText(properties.getBucketName())
                ? properties.getBucketName()
                : "default";
        return getPresignedUrl(bucketName, objectName, expires, timeUnit);
    }
}
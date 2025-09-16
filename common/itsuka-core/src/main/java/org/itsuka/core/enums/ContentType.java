package org.itsuka.core.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ContentType枚举类，定义了常见的MIME类型
 * 基于sun.net.www MIME content-types table生成
 */
public enum ContentType {
    // 未知类型
    UNKNOWN("content/unknown", ".unknown"),
    
    // 应用程序类型
    APPLICATION_OCTET_STREAM("application/octet-stream", ".saveme", ".dump", ".hqx", ".arc", ".obj", ".lib", ".bin", ".exe"),
    APPLICATION_PDF("application/pdf", ".pdf"),
    APPLICATION_POSTSCRIPT("application/postscript", ".eps", ".ai", ".ps"),
    APPLICATION_RTF("application/rtf", ".rtf"),
    APPLICATION_GZIP("application/gzip", ".gz"),
    APPLICATION_ZIP("application/zip", ".zip"),
    APPLICATION_X_7Z_COMPRESSED("application/x-7z-compressed", ".7z"),
    APPLICATION_RAR("application/vnd.rar", ".rar"),
    APPLICATION_BZ2("application/bz2", ".bz2"),
    APPLICATION_X_TAR("application/x-tar", ".tar"),
    APPLICATION_GTAR("application/x-gtar", ".gtar"),
    APPLICATION_X_USTAR("application/x-ustar", ".ustar"),
    
    // 文档类型
    TEXT_PLAIN("text/plain", ".text", ".c", ".cc", ".c++", ".h", ".pl", ".txt", ".java", ".el", ".php", ".adoc", ".py"),
    TEXT_HTML("text/html", ".htm", ".html"),
    TEXT_CSS("text/css", ".css"),
    TEXT_JAVASCRIPT("text/javascript", ".js"),
    TEXT_CSV("text/csv", ".csv"),
    TEXT_TSV("text/tab-separated-values", ".tsv"),
    TEXT_MARKDOWN("text/markdown", ".md", ".markdown"),
    
    // 图片类型
    IMAGE_JPEG("image/jpeg", ".jfif", ".jfif-tbnl", ".jpe", ".jpg", ".jpeg"),
    IMAGE_PNG("image/png", ".png"),
    IMAGE_GIF("image/gif", ".gif"),
    IMAGE_BMP("image/bmp", ".bmp"),
    IMAGE_TIFF("image/tiff", ".tif", ".tiff"),
    IMAGE_SVG("image/svg+xml", ".svg", ".svgz"),
    IMAGE_WEBP("image/webp", ".webp"),
    
    // 音频类型
    AUDIO_MPEG("audio/mpeg", ".mp2", ".mp3"),
    AUDIO_WAV("audio/x-wav", ".wav"),
    AUDIO_AAC("audio/aac", ".aac"),
    AUDIO_OGG("audio/ogg", ".oga", ".ogg", ".opus", ".spx"),
    AUDIO_FLAC("audio/flac", ".flac"),
    AUDIO_MP4("audio/mp4", ".m4a"),
    AUDIO_BASIC("audio/basic", ".snd", ".au"),
    
    // 视频类型
    VIDEO_MP4("video/mp4", ".m4v", ".mp4"),
    VIDEO_MPEG("video/mpeg", ".mpg", ".mpe", ".mpeg"),
    VIDEO_OGG("video/ogg", ".ogv"),
    VIDEO_WEBM("video/webm", ".webm"),
    VIDEO_QUICKTIME("video/quicktime", ".mov", ".qt"),
    VIDEO_AVI("application/x-troff-msvideo", ".avi"),
    
    // 办公文档类型
    APPLICATION_MS_WORD("application/msword", ".doc"),
    APPLICATION_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    APPLICATION_MS_EXCEL("application/vnd.ms-excel", ".xls"),
    APPLICATION_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    APPLICATION_MS_POWERPOINT("application/vnd.ms-powerpoint", ".ppt"),
    APPLICATION_PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
    
    // OpenDocument类型
    APPLICATION_ODT("application/vnd.oasis.opendocument.text", ".odt"),
    APPLICATION_ODS("application/vnd.oasis.opendocument.spreadsheet", ".ods"),
    APPLICATION_ODP("application/vnd.oasis.opendocument.presentation", ".odp"),
    
    // 数据交换类型
    APPLICATION_XML("application/xml", ".xml"),
    APPLICATION_JSON("application/json", ".json"),
    
    // Java相关类型
    APPLICATION_JAVA_ARCHIVE("application/java-archive", ".jar");
    
    private final String mimeType;
    private final Set<String> extensions;
    
    // 缓存扩展名到ContentType的映射，提高性能
    private static final Map<String, ContentType> EXTENSION_TO_TYPE_MAP = new HashMap<>();
    
    static {
        // 初始化缓存映射
        for (ContentType type : values()) {
            for (String ext : type.extensions) {
                EXTENSION_TO_TYPE_MAP.put(ext.toLowerCase(), type);
            }
        }
    }
    
    ContentType(String mimeType, String... extensions) {
        this.mimeType = mimeType;
        this.extensions = new HashSet<>(Arrays.asList(extensions));
    }
    
    /**
     * 获取MIME类型字符串
     * @return MIME类型字符串
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * 获取文件扩展名集合
     * @return 文件扩展名集合
     */
    public Set<String> getExtensions() {
        return Collections.unmodifiableSet(extensions);
    }
    
    /**
     * 检查指定扩展名是否匹配当前ContentType
     * @param extension 文件扩展名（包含点号）
     * @return 是否匹配
     */
    public boolean matchesExtension(String extension) {
        return extension != null && extensions.contains(extension.toLowerCase());
    }
    
    /**
     * 根据文件扩展名查找对应的ContentType
     * @param extension 文件扩展名（可以包含或不包含点号，支持空格处理）
     * @return 对应的ContentType，如果没有找到则返回UNKNOWN
     */
    public static ContentType fromExtension(String extension) {
        if (extension == null) {
            return UNKNOWN;
        }
        
        // 去除前后空格
        String trimmedExtension = extension.trim();
        if (trimmedExtension.isEmpty()) {
            return UNKNOWN;
        }
        
        // 统一转换为小写
        String normalizedExtension = trimmedExtension.toLowerCase();
        
        // 确保扩展名以点号开头
        if (!normalizedExtension.startsWith(".")) {
            normalizedExtension = "." + normalizedExtension;
        }
        
        // 使用缓存映射进行快速查找
        return EXTENSION_TO_TYPE_MAP.getOrDefault(normalizedExtension, UNKNOWN);
    }
    
    /**
     * 根据文件名查找对应的ContentType
     * @param filename 文件名
     * @return 对应的ContentType，如果没有找到则返回UNKNOWN
     */
    public static ContentType fromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return UNKNOWN;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return UNKNOWN;
        }
        
        String extension = filename.substring(lastDotIndex);
        return fromExtension(extension);
    }
    
    /**
     * 根据MIME类型字符串查找对应的ContentType
     * @param mimeType MIME类型字符串
     * @return 对应的ContentType，如果没有找到则返回UNKNOWN
     */
    public static ContentType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return UNKNOWN;
        }
        
        Optional<ContentType> result = Arrays.stream(values())
                .filter(type -> type.mimeType.equalsIgnoreCase(mimeType))
                .findFirst();
        
        return result.orElse(UNKNOWN);
    }
}
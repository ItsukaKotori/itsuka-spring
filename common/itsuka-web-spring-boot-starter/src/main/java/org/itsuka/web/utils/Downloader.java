package org.itsuka.web.utils;


import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.enums.ContentType;
import org.itsuka.core.exception.ServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 文件下载器
 * 提供多种文件类型的下载功能，支持与MinIO存储集成
 */
@Slf4j
public class Downloader {
    /**
     * 通用文件下载方法
     * 
     * @param file 文件信息
     * @param consumer 文件内容提供者
     * @param response HTTP响应对象
     */
    public static void download(File file,
            Consumer<HttpServletResponse> consumer, HttpServletResponse response) {
        try {
            response.setContentType(file.getContentType().getMimeType());
            response.setCharacterEncoding(file.getCharset().name());
            response.setHeader("Content-disposition", buildContentDisposition(file.getFileName(), file.getCharset()));
            consumer.accept(response);
        } catch (Exception exception) {
            log.error("文件下载失败: {}", exception.getMessage(), exception);
            throw ServiceException.error(exception);
        }
    }

    /**
     * Excel文件下载
     * 
     * @param excel Excel文件信息
     * @param consumer 文件内容提供者
     * @param response HTTP响应对象
     */
    public static void downloadExcel(Excel excel, Consumer<HttpServletResponse> consumer, 
                                     HttpServletResponse response) {
        download(excel, consumer, response);
    }

    /**
     * PDF文件下载
     * 
     * @param pdf PDF文件信息
     * @param consumer 文件内容提供者
     * @param response HTTP响应对象
     */
    public static void downloadPdf(Pdf pdf, Consumer<HttpServletResponse> consumer, 
                                  HttpServletResponse response) {
        download(pdf, consumer, response);
    }

    /**
     * 图片文件下载
     * 
     * @param image 图片文件信息
     * @param consumer 文件内容提供者
     * @param response HTTP响应对象
     */
    public static void downloadImage(Image image, Consumer<HttpServletResponse> consumer, 
                                    HttpServletResponse response) {
        download(image, consumer, response);
    }

    /**
     * 文本文件下载
     * 
     * @param text 文本文件信息
     * @param consumer 文件内容提供者
     * @param response HTTP响应对象
     */
    public static void downloadText(Text text, Consumer<HttpServletResponse> consumer, 
                                   HttpServletResponse response) {
        download(text, consumer, response);
    }

    /**
     * Word文档下载
     * 
     * @param word Word文件信息
     * @param consumer 文件内容提供者
     * @param response HTTP响应对象
     */
    public static void downloadWord(Word word, Consumer<HttpServletResponse> consumer, 
                                   HttpServletResponse response) {
        download(word, consumer, response);
    }

    /**
     * 构建Content-Disposition头信息，兼容不同浏览器
     * 
     * @param fileName 文件名
     * @param charset 字符集
     * @return Content-Disposition头信息
     * @throws Exception 编码异常
     */
    private static String buildContentDisposition(String fileName, Charset charset) throws Exception {
        // 对文件名进行URL编码，确保跨浏览器兼容性
        String encodedFileName = URLEncoder.encode(fileName, charset).replace("+", "%");
        // 构建Content-Disposition头
        return String.format("attachment; filename*=UTF-8''%s; filename=\"%s\"", encodedFileName, encodedFileName);
    }

    /**
     * Excel文件
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Excel extends File {
        public Excel(String fileName) {
            super(fileName, StandardCharsets.UTF_8, ContentType.APPLICATION_MS_EXCEL);
        }
    }

    /**
     * PDF文件
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Pdf extends File {
        public Pdf(String fileName) {
            super(fileName, StandardCharsets.UTF_8, ContentType.APPLICATION_PDF);
        }
    }

    /**
     * 图片文件
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Image extends File {
        public Image(String fileName) {
            super(fileName, StandardCharsets.UTF_8, ContentType.fromFilename(fileName));
        }
    }

    /**
     * 文本文件
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Text extends File {
        public Text(String fileName) {
            super(fileName, StandardCharsets.UTF_8, ContentType.TEXT_PLAIN);
        }
    }

    /**
     * Word文档
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Word extends File {
        public Word(String fileName) {
            super(fileName, StandardCharsets.UTF_8, fileName.toLowerCase().endsWith(".docx") 
                ? ContentType.APPLICATION_DOCX 
                : ContentType.APPLICATION_MS_WORD);
        }
    }

    /**
     * 通用文件
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class File {
        /**
         * 文件名
         */
        private String fileName;

        /**
         * 编码集
         */
        private Charset charset;

        /**
         * Content-Type
         */
        private ContentType contentType;
    }
}

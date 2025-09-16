package org.itsuka.file;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import org.itsuka.core.enums.ContentType;
import org.itsuka.core.exception.ServiceException;

/**
 * 下载器
 */
public class Downloader {
    public static void download(File file,
            Consumer<HttpServletResponse> consumer, HttpServletResponse response) {
        try {
            response.setContentType(file.getContentType().getMimeType());
            response.setCharacterEncoding(file.getCharset().name());
            String encodedFileName = URLEncoder.encode(file.getFileName(), file.getCharset());
            response.setHeader("Content-disposition", "attachment;filename=" + encodedFileName);
            consumer.accept(response);
        } catch (Exception exception) {
            throw ServiceException.error(exception);
        }
    }

    /**
     * Excel
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Excel extends File {
        public Excel(String fileName) {
            super(fileName, StandardCharsets.UTF_8, ContentType.APPLICATION_MS_EXCEL);
        }
    }

    /**
     * File
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

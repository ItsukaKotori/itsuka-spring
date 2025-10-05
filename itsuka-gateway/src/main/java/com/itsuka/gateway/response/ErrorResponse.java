package com.itsuka.gateway.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ErrorResponse extends BaseResponse {
    /**
     * 创建错误响应
     *
     * @param httpStatus HTTP状态码
     * @param message    错误消息
     * @param path       请求路径
     * @return ErrorResponse实例
     */
    public static ErrorResponse of(HttpStatus httpStatus, String message, String path) {
        ErrorResponse response = new ErrorResponse();
        response.setCode(httpStatus.value());
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        response.setPath(path);
        response.setStatus(httpStatus.getReasonPhrase());
        response.setHttpStatus(httpStatus);
        return response;
    }

    @Override
    public HttpStatus getHttpStatus() {
        HttpStatus httpStatus = super.getHttpStatus();
        return httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 获取响应头
     *
     * @return 响应头Map
     */
    @Override
    public Map<String, String> getResponseHeaders() {
        return new HashMap<>();
    }

    // ==================== 便捷静态方法 ====================

    /**
     * 创建并写入未授权响应（401）
     *
     * @param path       请求路径
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static ErrorResponse unauthorized(String path, String message) {
        return of(HttpStatus.UNAUTHORIZED, message, path);
    }

    /**
     * 创建并写入禁止访问响应（403）
     *
     * @param path       请求路径
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static ErrorResponse forbidden(String path, String message) {
        return of(HttpStatus.FORBIDDEN, message, path);
    }

    /**
     * 创建并写入内部服务器错误响应（500）
     *
     * @param path       请求路径
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static ErrorResponse internalServerError(String path, String message) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, message, path);
    }

    /**
     * 创建并写入自定义错误响应
     *
     * @param path       请求路径
     * @param httpStatus   HTTP状态码
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static ErrorResponse error(String path, HttpStatus httpStatus, String message) {
        return of(httpStatus, message, path);
    }
}

package org.itsuka.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitErrorResponse<T> extends ErrorResponse<T> {
    private static final String message = "Too many requests, please try again later";

    /**
     * 限流最大请求数
     */
    private int limit;

    /**
     * 限流剩余请求数
     */
    private int remaining;

    /**
     * 限流重置时间戳
     */
    private long reset;

    /**
     * 创建限流错误响应
     *
     * @param path        请求路径
     * @param maxRequests 最大请求数
     * @param resetTime   重置时间戳
     * @return RateLimitErrorResponse实例
     */
    public static <T> RateLimitErrorResponse<T> of(String path, int maxRequests, long resetTime) {
        RateLimitErrorResponse<T> response = new RateLimitErrorResponse<>();
        response.setCode(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        response.setPath(path);
        response.setHttpStatus(HttpStatus.TOO_MANY_REQUESTS);
        response.setLimit(maxRequests);
        response.setRemaining(0);
        response.setReset(resetTime);
        return response;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }

    /**
     * 获取限流相关响应头
     *
     * @return 响应头Map，包含X-RateLimit-*头
     */
    @Override
    public Map<String, String> getResponseHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-RateLimit-Limit", String.valueOf(limit));
        headers.put("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.put("X-RateLimit-Reset", String.valueOf(reset));
        return headers;
    }

    // ==================== 便捷静态方法 ====================

    /**
     * 创建并写入限流错误响应（429）
     *
     * @param path       请求路径
     * @param maxRequests  最大请求数
     * @param resetTime    重置时间戳
     * @return RateLimitErrorResponse实例
     */
    public static <T> RateLimitErrorResponse<T> tooManyRequests(String path, int maxRequests, long resetTime) {
        return of(path, maxRequests, resetTime);
    }
}

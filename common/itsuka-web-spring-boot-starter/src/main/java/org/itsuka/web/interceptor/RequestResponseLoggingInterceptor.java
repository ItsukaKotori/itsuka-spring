package org.itsuka.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 请求响应日志拦截器
 * 记录请求和响应的详细信息，用于调试和监控
 * 
 * @author Itsuka
 */
@Slf4j
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "REQUEST_START_TIME";
    private static final String REQUEST_ID = "REQUEST_ID";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * 需要过滤的敏感参数名
     */
    private static final List<String> SENSITIVE_PARAMS = Arrays.asList(
        "password", "pwd", "token", "secret", "key", "auth", "authorization",
        "credential", "credentials", "accessToken", "refreshToken"
    );
    
    /**
     * 需要记录请求体的Content-Type
     */
    private static final List<String> LOGGABLE_CONTENT_TYPES = Arrays.asList(
        "application/json", "application/xml", "text/xml", "text/plain"
    );

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler) {
        
        // 生成请求ID
        String requestId = generateRequestId();
        request.setAttribute(REQUEST_ID, requestId);
        response.setHeader("X-Request-ID", requestId);
        
        // 记录请求开始时间
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());
        
        // 记录请求信息
        logRequest(request, requestId);
        
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                              @NonNull HttpServletResponse response,
                              @NonNull Object handler,
                              Exception ex) {
        
        // 计算请求处理时间
        Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
        String requestId = (String) request.getAttribute(REQUEST_ID);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        // 记录响应信息
        logResponse(request, response, requestId, duration, ex);
    }

    /**
     * 记录请求信息
     */
    private void logRequest(HttpServletRequest request, String requestId) {
        try {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n==== HTTP Request ====")
                     .append("\nRequest ID: ").append(requestId)
                     .append("\nTimestamp: ").append(LocalDateTime.now().format(FORMATTER))
                     .append("\nMethod: ").append(request.getMethod())
                     .append("\nURI: ").append(request.getRequestURI());

            // 查询参数
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                logBuilder.append("\nQuery String: ").append(filterSensitiveParams(queryString));
            }

            // Headers
            logBuilder.append("\nHeaders:");
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                String headerValue = request.getHeader(headerName);
                if (isSensitiveHeader(headerName)) {
                    headerValue = "***";
                }
                logBuilder.append("\n  ").append(headerName).append(": ").append(headerValue);
            });

            // 客户端信息
            logBuilder.append("\nRemote Address: ").append(getClientIpAddress(request))
                     .append("\nUser Agent: ").append(request.getHeader("User-Agent"));

            // 请求体（如果是可记录的类型）
            if (shouldLogRequestBody(request)) {
                String requestBody = getRequestBody(request);
                if (requestBody != null && !requestBody.isEmpty()) {
                    logBuilder.append("\nRequest Body: ").append(requestBody);
                }
            }

            logBuilder.append("\n======================");
            log.info(logBuilder.toString());
            
        } catch (Exception e) {
            log.warn("Failed to log request for ID: {}", requestId, e);
        }
    }

    /**
     * 记录响应信息
     */
    private void logResponse(HttpServletRequest request, HttpServletResponse response, 
                           String requestId, long duration, Exception ex) {
        try {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n==== HTTP Response ====")
                     .append("\nRequest ID: ").append(requestId)
                     .append("\nTimestamp: ").append(LocalDateTime.now().format(FORMATTER))
                     .append("\nMethod: ").append(request.getMethod())
                     .append("\nURI: ").append(request.getRequestURI())
                     .append("\nStatus: ").append(response.getStatus())
                     .append("\nDuration: ").append(duration).append(" ms");

            // 响应Headers
            logBuilder.append("\nResponse Headers:");
            response.getHeaderNames().forEach(headerName -> {
                String headerValue = response.getHeader(headerName);
                logBuilder.append("\n  ").append(headerName).append(": ").append(headerValue);
            });

            // 异常信息
            if (ex != null) {
                logBuilder.append("\nException: ").append(ex.getClass().getSimpleName())
                          .append(" - ").append(ex.getMessage());
            }

            // 响应体（如果是ContentCachingResponseWrapper）
            if (response instanceof ContentCachingResponseWrapper wrapper) {
                String responseBody = getResponseBody(wrapper);
                if (responseBody != null && !responseBody.isEmpty()) {
                    logBuilder.append("\nResponse Body: ").append(responseBody);
                }
            }

            logBuilder.append("\n=======================");
            
            // 根据状态码和处理时间选择日志级别
            if (ex != null || response.getStatus() >= 500) {
                log.error(logBuilder.toString());
            } else if (response.getStatus() >= 400 || duration > 5000) {
                log.warn(logBuilder.toString());
            } else {
                log.info(logBuilder.toString());
            }
            
        } catch (Exception e) {
            log.warn("Failed to log response for ID: {}", requestId, e);
        }
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "X-Real-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果有多个IP，取第一个
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 判断是否应该记录请求体
     */
    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }

        return LOGGABLE_CONTENT_TYPES.stream()
                .anyMatch(type -> contentType.toLowerCase().contains(type));
    }

    /**
     * 获取请求体内容
     */
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * 获取响应体内容
     */
    private String getResponseBody(ContentCachingResponseWrapper wrapper) {
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length > 0) {
            String responseBody = new String(content, StandardCharsets.UTF_8);
            // 复制内容回响应流
            try {
                wrapper.copyBodyToResponse();
            } catch (Exception e) {
                log.warn("Failed to copy response body", e);
            }
            return responseBody;
        }
        return null;
    }

    /**
     * 过滤敏感参数
     */
    private String filterSensitiveParams(String queryString) {
        String[] params = queryString.split("&");
        StringBuilder filtered = new StringBuilder();
        
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = isSensitiveParam(key) ? "***" : keyValue[1];
                
                if (filtered.length() > 0) {
                    filtered.append("&");
                }
                filtered.append(key).append("=").append(value);
            }
        }
        
        return filtered.toString();
    }

    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        return SENSITIVE_PARAMS.stream()
                .anyMatch(sensitive -> paramName.toLowerCase().contains(sensitive.toLowerCase()));
    }

    /**
     * 判断是否为敏感Header
     */
    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_PARAMS.stream()
                .anyMatch(sensitive -> headerName.toLowerCase().contains(sensitive.toLowerCase()));
    }
}
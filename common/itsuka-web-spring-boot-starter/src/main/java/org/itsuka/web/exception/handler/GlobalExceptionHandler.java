package org.itsuka.web.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.Response;
import org.itsuka.core.exception.ServiceException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;


/**
 * 全局异常处理器
 * 统一处理应用程序中的各种异常
 *
 * @author Itsuka
 * @since 2023/3/1
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常 (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        log.warn("Validation failed for request: {}", request.getRequestURI(), exception);

        String errorMessage = exception.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));

        return Response.error(HttpStatus.BAD_REQUEST, "参数校验失败: " + errorMessage).withRequestId(getRequestId(request));
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleBindException(BindException exception, HttpServletRequest request) {
        log.warn("Parameter binding failed for request: {}", request.getRequestURI(), exception);

        String errorMessage = exception.getFieldErrors().stream().map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage()).collect(Collectors.joining(", "));

        return Response.error(HttpStatus.BAD_REQUEST, "参数绑定失败: " + errorMessage).withRequestId(getRequestId(request));
    }

    /**
     * 处理 Validated 产生的异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleConstraintViolationException(ConstraintViolationException exception, HttpServletRequest request) {
        log.warn("Constraint violation for request: {}", request.getRequestURI(), exception);

        String errorMessage = exception.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));

        return Response.error(HttpStatus.BAD_REQUEST, "请求参数不正确: " + errorMessage).withRequestId(getRequestId(request));
    }

    /**
     * 处理业务异常
     * 根据异常中携带的 HttpStatus 动态设置 HTTP 状态码。
     */
    @ExceptionHandler(ServiceException.class)
    public Response<?> handleServiceException(ServiceException serviceException, HttpServletRequest request, HttpServletResponse response) {
        log.error("Business exception for request: {} - code: {}, message: {}", request.getRequestURI(), serviceException.getStatus(), serviceException.getMessage(), serviceException);
        response.setStatus(serviceException.getStatus().value());
        return Response.error(serviceException).withRequestId(getRequestId(request));
    }

    /**
     * 处理 HTTP 请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Response<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.warn("Method not allowed for request: {} - method: {}", request.getRequestURI(), request.getMethod());
        return Response.error(HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方法: " + exception.getMethod()).withRequestId(getRequestId(request));
    }

    /**
     * 处理 HTTP 媒体类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Response<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        log.warn("Unsupported media type for request: {} - content-type: {}", request.getRequestURI(), request.getContentType());
        return Response.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的媒体类型: " + exception.getContentType()).withRequestId(getRequestId(request));
    }

    /**
     * 处理 HTTP 媒体类型不可接受异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public Response<?> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        log.warn("Not acceptable media type for request: {}", request.getRequestURI());
        return Response.error(HttpStatus.NOT_ACCEPTABLE, "不可接受的媒体类型").withRequestId(getRequestId(request));
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception, HttpServletRequest request) {
        log.warn("Missing request parameter for request: {} - parameter: {}", request.getRequestURI(), exception.getParameterName());
        return Response.error(HttpStatus.BAD_REQUEST, "缺少必需的请求参数: " + exception.getParameterName()).withRequestId(getRequestId(request));
    }

    /**
     * 处理缺少路径变量异常
     */
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleMissingPathVariableException(MissingPathVariableException exception, HttpServletRequest request) {
        log.warn("Missing path variable for request: {} - variable: {}", request.getRequestURI(), exception.getVariableName());
        return Response.error(HttpStatus.BAD_REQUEST, "缺少必需的路径变量: " + exception.getVariableName()).withRequestId(getRequestId(request));
    }

    /**
     * 处理方法参数类型不匹配异常
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleTypeMismatchException(TypeMismatchException exception, HttpServletRequest request) {
        log.warn("Type mismatch for request: {} - parameter: {}, required type: {}", request.getRequestURI(), exception.getPropertyName(), exception.getRequiredType());
        return Response.error(HttpStatus.BAD_REQUEST, "参数类型不匹配: " + exception.getPropertyName()).withRequestId(getRequestId(request));
    }

    /**
     * 处理 HTTP 消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("HTTP message not readable for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.BAD_REQUEST, "请求体格式错误或无法解析").withRequestId(getRequestId(request));
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Response<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        log.warn("Upload size exceeded for request: {} - max size: {}", request.getRequestURI(), exception.getMaxUploadSize());
        return Response.error(HttpStatus.PAYLOAD_TOO_LARGE, "上传文件大小超出限制").withRequestId(getRequestId(request));
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Response<?> handleNoHandlerFoundException(NoHandlerFoundException exception, HttpServletRequest request) {
        log.warn("No handler found for request: {} {}", exception.getHttpMethod(), exception.getRequestURL());
        return Response.error(HttpStatus.NOT_FOUND, "请求的资源不存在").withRequestId(getRequestId(request));
    }

    /**
     * 处理Spring Security认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Response<?> handleAuthenticationException(AuthenticationException exception, HttpServletRequest request) {
        log.warn("Authentication failed for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.UNAUTHORIZED, "认证失败: " + exception.getMessage()).withRequestId(getRequestId(request));
    }

    /**
     * 处理Spring Security授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Response<?> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("Access denied for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.FORBIDDEN, "访问被拒绝: " + exception.getMessage()).withRequestId(getRequestId(request));
    }

    /**
     * 处理IO异常
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> handleIOException(IOException exception, HttpServletRequest request) {
        log.error("IO error for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.INTERNAL_SERVER_ERROR, "文件操作失败").withRequestId(getRequestId(request));
    }

    /**
     * 处理SQL异常
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> handleSQLException(SQLException exception, HttpServletRequest request) {
        log.error("Database error for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.INTERNAL_SERVER_ERROR, "数据库操作失败").withRequestId(getRequestId(request));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        log.warn("Illegal argument for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.BAD_REQUEST, "参数错误: " + exception.getMessage()).withRequestId(getRequestId(request));
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> handleNullPointerException(NullPointerException exception, HttpServletRequest request) {
        log.error("Null pointer exception for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误").withRequestId(getRequestId(request));
    }

    /**
     * 兜底异常处理器
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> handleGenericException(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error for request: {}", request.getRequestURI(), exception);
        return Response.error(HttpStatus.INTERNAL_SERVER_ERROR, "系统错误").withRequestId(getRequestId(request)).withTimestamp(System.currentTimeMillis());
    }

    /**
     * 从请求中获取请求ID
     */
    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = request.getHeader("X-Trace-ID");
        }
        return requestId;
    }
}

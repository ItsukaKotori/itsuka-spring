package org.itsuka.core.domain;

import lombok.Data;
import org.itsuka.core.exception.ServiceException;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 业务结果包装类
 * 用于内部业务逻辑处理，支持函数式编程风格
 * 
 * @author Itsuka
 * @param <T> 数据类型
 */
@Data
public class Result<T> {
    
    /**
     * 是否成功
     */
    private final boolean success;
    
    /**
     * 数据
     */
    private final T data;
    
    /**
     * 错误信息
     */
    private final String errorMessage;
    
    /**
     * 错误码
     */
    private final Integer errorCode;
    
    /**
     * 异常信息
     */
    private final Throwable throwable;
    
    private Result(boolean success, T data, String errorMessage, Integer errorCode, Throwable throwable) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.throwable = throwable;
    }
    
    /**
     * 创建成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null, null, null);
    }
    
    /**
     * 创建成功结果（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(true, null, null, null, null);
    }
    
    /**
     * 创建失败结果
     */
    public static <T> Result<T> failure(String errorMessage) {
        return new Result<>(false, null, errorMessage, null, null);
    }
    
    /**
     * 创建失败结果（带错误码）
     */
    public static <T> Result<T> failure(Integer errorCode, String errorMessage) {
        return new Result<>(false, null, errorMessage, errorCode, null);
    }
    
    /**
     * 创建失败结果（带异常）
     */
    public static <T> Result<T> failure(Throwable throwable) {
        return new Result<>(false, null, throwable.getMessage(), null, throwable);
    }
    
    /**
     * 创建失败结果（带异常和错误码）
     */
    public static <T> Result<T> failure(Integer errorCode, Throwable throwable) {
        return new Result<>(false, null, throwable.getMessage(), errorCode, throwable);
    }
    
    /**
     * 从ServiceException创建失败结果
     */
    public static <T> Result<T> failure(ServiceException serviceException) {
        return new Result<>(false, null, serviceException.getMessage(), serviceException.getStatus().value(), serviceException);
    }
    
    /**
     * 安全执行操作
     */
    public static <T> Result<T> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * 安全执行操作（带错误码）
     */
    public static <T> Result<T> of(Supplier<T> supplier, Integer errorCode) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(errorCode, e);
        }
    }
    
    /**
     * 条件判断创建结果
     */
    public static <T> Result<T> condition(boolean condition, T data, String errorMessage) {
        return condition ? success(data) : failure(errorMessage);
    }
    
    /**
     * 条件判断创建结果（带错误码）
     */
    public static <T> Result<T> condition(boolean condition, T data, Integer errorCode, String errorMessage) {
        return condition ? success(data) : failure(errorCode, errorMessage);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 判断是否失败
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * 获取数据，如果失败则返回null
     */
    public T getDataOrNull() {
        return success ? data : null;
    }
    
    /**
     * 获取数据，如果失败则返回默认值
     */
    public T getDataOrDefault(T defaultValue) {
        return success ? data : defaultValue;
    }
    
    /**
     * 获取数据，如果失败则通过供应商获取
     */
    public T getDataOrGet(Supplier<T> supplier) {
        return success ? data : supplier.get();
    }
    
    /**
     * 获取数据的Optional包装
     */
    public Optional<T> getData() {
        return success ? Optional.ofNullable(data) : Optional.empty();
    }
    
    /**
     * 成功时执行操作
     */
    public Result<T> onSuccess(Consumer<T> consumer) {
        if (success) {
            consumer.accept(data);
        }
        return this;
    }
    
    /**
     * 失败时执行操作
     */
    public Result<T> onFailure(Consumer<String> consumer) {
        if (!success) {
            consumer.accept(errorMessage);
        }
        return this;
    }
    
    /**
     * 失败时执行操作（带异常）
     */
    public Result<T> onFailure(Runnable runnable) {
        if (!success) {
            runnable.run();
        }
        return this;
    }
    
    /**
     * 映射数据
     */
    public <U> Result<U> map(Function<T, U> mapper) {
        if (success) {
            try {
                return success(mapper.apply(data));
            } catch (Exception e) {
                return failure(e);
            }
        } else {
            return failure(errorCode, errorMessage);
        }
    }
    
    /**
     * 扁平映射
     */
    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (success) {
            try {
                return mapper.apply(data);
            } catch (Exception e) {
                return failure(e);
            }
        } else {
            return failure(errorCode, errorMessage);
        }
    }
    
    /**
     * 过滤数据
     */
    public Result<T> filter(Predicate<T> predicate, String errorMessage) {
        if (success && predicate.test(data)) {
            return this;
        } else if (success) {
            return failure(errorMessage);
        } else {
            return this;
        }
    }
    
    /**
     * 过滤数据（带错误码）
     */
    public Result<T> filter(Predicate<T> predicate, Integer errorCode, String errorMessage) {
        if (success && predicate.test(data)) {
            return this;
        } else if (success) {
            return failure(errorCode, errorMessage);
        } else {
            return this;
        }
    }
    
    /**
     * 恢复操作（失败时尝试恢复）
     */
    public Result<T> recover(Function<String, T> recovery) {
        if (success) {
            return this;
        } else {
            try {
                return success(recovery.apply(errorMessage));
            } catch (Exception e) {
                return failure(e);
            }
        }
    }
    
    /**
     * 恢复操作（失败时尝试恢复为Result）
     */
    public Result<T> recoverWith(Function<String, Result<T>> recovery) {
        if (success) {
            return this;
        } else {
            try {
                return recovery.apply(errorMessage);
            } catch (Exception e) {
                return failure(e);
            }
        }
    }
    
    /**
     * 转换为Response
     */
    @SuppressWarnings("unchecked")
    public Response<T> toResponse() {
        if (success) {
            return Response.success(data);
        } else {
            return errorCode != null 
                ? (Response<T>) Response.error(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage)
                : (Response<T>) Response.error(errorMessage);
        }
    }
    
    /**
     * 转换为Response（带请求ID）
     */
    @SuppressWarnings("unchecked")
    public Response<T> toResponse(String requestId) {
        if (success) {
            return Response.success(data, requestId);
        } else {
            return errorCode != null 
                ? (Response<T>) Response.error(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, requestId)
                : (Response<T>) Response.error(errorMessage).withRequestId(requestId);
        }
    }
    
    /**
     * 获取错误信息或默认值
     */
    public String getErrorMessageOrDefault(String defaultMessage) {
        return errorMessage != null ? errorMessage : defaultMessage;
    }
    
    /**
     * 抛出异常（如果失败）
     */
    public T getOrThrow() throws ServiceException {
        if (success) {
            return data;
        } else if (throwable instanceof ServiceException serviceException) {
            throw serviceException;
        } else {
            throw new ServiceException(throwable, HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
    }
    
    /**
     * 抛出自定义异常（如果失败）
     */
    public <E extends Exception> T getOrThrow(Function<String, E> exceptionMapper) throws E {
        if (success) {
            return data;
        } else {
            throw exceptionMapper.apply(errorMessage);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result<?> result = (Result<?>) o;
        return success == result.success &&
               Objects.equals(data, result.data) &&
               Objects.equals(errorMessage, result.errorMessage) &&
               Objects.equals(errorCode, result.errorCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, data, errorMessage, errorCode);
    }
    
    @Override
    public String toString() {
        if (success) {
            return "Result.success(" + data + ")";
        } else {
            return "Result.failure(" + errorCode + ", " + errorMessage + ")";
        }
    }
}
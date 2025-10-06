package org.itsuka.core.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

/**
 * 自定义异常信息
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceException extends RuntimeException {
    private HttpStatus status;

    private String message;

    public ServiceException(Throwable cause, HttpStatus status, String message) {
        super(cause);
        this.status = status;
        this.message = message;
    }

    public static ServiceException create(String message) {
        return create(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ServiceException error(Throwable cause) {
        return new ServiceException(cause, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    public static ServiceException create(HttpStatus status, String message) {
        return new ServiceException(null, status, message);
    }
}

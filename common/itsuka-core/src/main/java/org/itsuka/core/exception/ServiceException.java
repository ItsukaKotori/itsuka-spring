package org.itsuka.core.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static org.itsuka.core.constant.Constants.ERROR_CODE;

/**
 * 自定义异常信息
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceException extends RuntimeException {
    private Integer code;

    private String message;

    public ServiceException(Throwable cause, Integer code, String message) {
        super(cause);
        this.code = code;
        this.message = message;
    }

    public static ServiceException create(String message) {
        return create(ERROR_CODE, message);
    }

    public static ServiceException error(Throwable cause) {
        return new ServiceException(cause, ERROR_CODE, null);
    }

    public static ServiceException create(Integer code, String message) {
        return new ServiceException(null, code, message);
    }
}

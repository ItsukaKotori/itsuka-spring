package org.itsuka.core.exception;

import lombok.Data;

import static org.itsuka.core.constant.Constants.ERROR_CODE;

/**
 * 自定义异常信息
 *
 * @author Itsuka
 * @date 2023/2/26
 */
@Data
public class ServiceException extends RuntimeException {
    private Integer code;

    private String message;

    private ServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ServiceException create(String message) {
        return create(ERROR_CODE, message);
    }

    public static ServiceException create(Integer code, String message) {
        return new ServiceException(code, message);
    }
}

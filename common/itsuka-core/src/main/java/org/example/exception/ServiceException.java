package org.example.exception;

import static org.example.constant.Constants.ERROR_CODE;

/**
 * 自定义异常信息
 *
 * @author Itsuka
 * @date 2023/2/26
 */
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

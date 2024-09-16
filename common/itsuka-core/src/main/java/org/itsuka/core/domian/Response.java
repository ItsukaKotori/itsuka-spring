package org.itsuka.core.domian;

import lombok.Data;
import org.itsuka.core.exception.ServiceException;

import java.rmi.ServerException;

import static org.itsuka.core.constant.Constants.ERROR_CODE;
import static org.itsuka.core.constant.Constants.SUCCESS_CODE;

/**
 * 统一返回信息
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@Data
public class Response<T> {
    private Integer code;

    private boolean success;

    private String message;

    private T data;

    private Response() {
    }

    private Response(Integer code, boolean success, String message, T data) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> success(T data) {
        return new Response<T>(SUCCESS_CODE, true, null, data);
    }

    public static Response<?> error(String message) {
        return error(ERROR_CODE, message);
    }

    public static Response<?> error(Integer code, String message) {
        return new Response<>(code, false, message, null);
    }

    public static Response<?> error(ServiceException serviceException) {
        return error(serviceException.getMessage());
    }
}

package org.itsuka.web;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domian.Response;
import org.itsuka.core.exception.ServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import static org.itsuka.core.constant.Constants.BAD_REQUEST_CODE;


/**
 * 全局异常处理器
 *
 * @author Itsuka
 * @date 2023/3/1
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * Validated 产生的异常
     *
     * @param exception
     * @return
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public Response<?> constraintViolationExceptionHandler(ConstraintViolationException exception) {
        log.error("handle ConstraintViolationException", exception);
        ConstraintViolation<?> constraintViolation = exception.getConstraintViolations().iterator().next();
        return Response.error(BAD_REQUEST_CODE,
                String.format("请求参数不正确: %s", constraintViolation.getMessage()));
    }
    /**
     * 捕获ServiceException
     *
     * @param serviceException
     * @return
     */
    @ExceptionHandler(ServiceException.class)
    public Response<?> serviceException(ServiceException serviceException) {
        log.error("handle serviceException code: {}, message: {}",
                serviceException.getCode(), serviceException.getMessage());
        return Response.error(serviceException);
    }

    /**
     * 兜底 Exception
     *
     * @param exception
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Response<?> exception(Exception exception) {
        log.error("handle exception", exception);
        return Response.error("系统错误");
    }
}

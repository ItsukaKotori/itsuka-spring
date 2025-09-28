package org.itsuka.web.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.Response;
import org.itsuka.core.exception.ServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import static org.itsuka.core.constant.Constants.BAD_REQUEST_CODE;


/**
 * 全局异常处理器
 *
 * @author Itsuka
 * @since 2023/3/1
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
    public Response<?> constraintViolationException(ConstraintViolationException exception) {
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

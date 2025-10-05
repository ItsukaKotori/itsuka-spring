package org.itsuka.core.exception;

import org.springframework.http.HttpStatus;

/**
 * 认证异常
 *
 * @author Itsuka
 */
public class AuthenticationException extends ServiceException {
    
    public AuthenticationException(String message, Throwable throwable) {
        super(throwable, HttpStatus.UNAUTHORIZED, message);
    }

    public AuthenticationException(String message) {
        super(null, HttpStatus.UNAUTHORIZED, message);
    }
}
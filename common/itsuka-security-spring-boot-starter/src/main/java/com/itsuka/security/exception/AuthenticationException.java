package com.itsuka.security.exception;

/**
 * 认证异常
 *
 * @author Itsuka
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
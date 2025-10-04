package com.itsuka.security.exception;

/**
 * JWT异常基类
 *
 * @author Itsuka
 */
public class JwtException extends RuntimeException {
    
    public JwtException(String message) {
        super(message);
    }
    
    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
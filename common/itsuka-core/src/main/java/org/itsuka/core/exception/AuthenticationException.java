package org.itsuka.core.exception;

import static org.itsuka.core.constant.Constants.AUTHENTICATION_FAILURE_CODE;

/**
 * 认证异常
 *
 * @author Itsuka
 */
public class AuthenticationException extends ServiceException {
    
    public AuthenticationException(String message, Throwable throwable) {
        super(throwable, AUTHENTICATION_FAILURE_CODE, message);
    }

    public AuthenticationException(String message) {
        super(null, AUTHENTICATION_FAILURE_CODE, message);
    }
}
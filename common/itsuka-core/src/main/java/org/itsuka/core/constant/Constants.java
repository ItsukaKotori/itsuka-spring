package org.itsuka.core.constant;

import java.time.format.DateTimeFormatter;

/**
 * 常量类
 *
 * @author Itsuka
 * @since 2023/2/26
 */
public final class Constants {
    /**
     * 成功响应编码
     */
    public static final Integer SUCCESS_CODE = 200;

    /**
     * 错误相应编码
     */
    public static final Integer ERROR_CODE = 500;

    /**
     * 错误相应编码
     */
    public static final Integer BAD_REQUEST_CODE = 400;

    public static final Integer AUTHENTICATION_FAILURE_CODE = 401;

    /**
     * 日期时间Fomatter
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 日期Formatter
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final String LOGIN_HEADER = "login_user";

    private Constants() {
    }
}

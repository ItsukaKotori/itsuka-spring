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

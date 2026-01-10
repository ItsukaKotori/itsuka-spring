package org.itsuka.web.desensitization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段脱敏注解
 * 用于标记需要进行脱敏处理的字段
 *
 * @author Itsuka
 * @since 2023/12/7
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Desensitization {

    /**
     * 脱敏类型
     *
     * @return 脱敏类型
     */
    DesensitizationType type() default DesensitizationType.CUSTOM;

    /**
     * 自定义脱敏时保留的前缀长度
     * 仅当type为CUSTOM时有效
     *
     * @return 保留的前缀长度
     */
    int prefixKeep() default 0;

    /**
     * 自定义脱敏时保留的后缀长度
     * 仅当type为CUSTOM时有效
     *
     * @return 保留的后缀长度
     */
    int suffixKeep() default 0;

    /**
     * 脱敏替换字符
     * 默认为*
     *
     * @return 替换字符
     */
    char replacer() default '*';

    /**
     * 是否启用脱敏
     * 默认为true
     *
     * @return 是否启用脱敏
     */
    boolean enabled() default true;
}
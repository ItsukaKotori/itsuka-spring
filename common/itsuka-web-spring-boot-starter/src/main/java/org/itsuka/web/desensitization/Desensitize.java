package org.itsuka.web.desensitization;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脱敏注解
 * 用于标记需要进行脱敏处理的字段
 *
 * @author Itsuka
 * @since 2023/12/10
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    /**
     * 脱敏类型
     *
     * @return 脱敏类型
     */
    DesensitizeType type() default DesensitizeType.CUSTOM;

    /**
     * 自定义脱敏时，保留前几位
     *
     * @return 保留前几位
     */
    int prefixKeep() default 0;

    /**
     * 自定义脱敏时，保留后几位
     *
     * @return 保留后几位
     */
    int suffixKeep() default 0;

    /**
     * 自定义脱敏时，使用的替换字符
     *
     * @return 替换字符
     */
    char replacer() default '*';
}
package org.itsuka.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。
 * <p>
 * 标注在业务方法上，方法执行成功或失败后按模板渲染并记录一条操作日志。
 *
 * @author Itsuka
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRecord {

    /**
     * 成功时的日志文案模板，必填。
     */
    String success();

    /**
     * 失败时的日志文案模板；为空时回退到 {@link #success()}，并另外记录 errorMsg。
     */
    String fail() default "";

    /**
     * 操作人 SpEL 模板；为空时使用 LogOperatorProvider。
     */
    String operator() default "";

    /**
     * 业务对象标识 SpEL 模板，必填，查询主键。
     */
    String bizNo();

    /**
     * 业务类型，例如 ORDER，静态值。
     */
    String type() default "";

    /**
     * 业务子类型/分类，例如 CUSTOMER、MANAGER，静态值。
     */
    String category() default "";

    /**
     * 扩展详情 SpEL 模板，存 detail 字段。
     */
    String detail() default "";

    /**
     * 记录条件，SpEL 布尔表达式；false 时不记录。
     */
    String condition() default "";
}

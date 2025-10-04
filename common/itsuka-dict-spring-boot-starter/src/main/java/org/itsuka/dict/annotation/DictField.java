package org.itsuka.dict.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DictField {
    /**
     * 字典Key
     * 
     * @return 根据key查询字典值
     */
    String key();

    /**
     * 默认值
     *
     * @return
     */
    String defaultValue() default "";

    /**
     * 是否覆盖原值
     *
     * @return
     */
    boolean override() default true;
}

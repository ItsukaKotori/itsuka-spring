package org.itsuka.dict.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

    String defaultValue() default "";
}

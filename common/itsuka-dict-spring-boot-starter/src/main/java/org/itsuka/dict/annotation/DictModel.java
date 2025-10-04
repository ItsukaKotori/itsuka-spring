package org.itsuka.dict.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DictModel {
    /**
     * 字典前缀
     * 
     * @return 字典前缀
     */
    String prefix() default "";
}

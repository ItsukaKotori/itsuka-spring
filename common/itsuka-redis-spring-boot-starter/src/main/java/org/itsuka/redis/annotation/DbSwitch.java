package org.itsuka.redis.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbSwitch {
    /**
     * Redis DataBase
     *
     * @return Redis Database
     */
    int database() default 0;
}

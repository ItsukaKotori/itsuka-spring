package org.itsuka.web.rest.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rest {
    int connectTimeout() default 5000;

    int readTimeout() default 5000;

    RestProxy proxy();
}

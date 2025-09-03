package org.itsuka.web.rest.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RestProxy {
    boolean enable() default true;

    String hostname();

    int port();

    String username();

    String password();
}

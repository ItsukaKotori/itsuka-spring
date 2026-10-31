package org.itsuka.log.support;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Default {@link LogParameterSerializer} based on {@link Arrays#deepToString(Object[])}.
 */
public class ToStringLogParameterSerializer implements LogParameterSerializer {

    @Override
    public String serialize(Method method, Object[] args) {
        return Arrays.deepToString(args);
    }
}

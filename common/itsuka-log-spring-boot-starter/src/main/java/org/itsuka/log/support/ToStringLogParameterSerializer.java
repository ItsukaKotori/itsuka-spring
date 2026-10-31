package org.itsuka.log.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default {@link LogParameterSerializer} based on {@link Arrays#deepToString(Object[])}.
 *
 * <p>Servlet request/response and multipart arguments are skipped using
 * {@link LogArgumentFilter}, matching the Jackson-backed serializer's behavior, so the snapshot
 * stays useful in web applications.</p>
 */
public class ToStringLogParameterSerializer implements LogParameterSerializer {

    @Override
    public String serialize(Method method, Object[] args) {
        if (args == null) {
            return null;
        }
        List<Object> serializable = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (LogArgumentFilter.isIgnored(arg)) {
                continue;
            }
            serializable.add(arg);
        }
        if (serializable.isEmpty()) {
            return null;
        }
        return Arrays.deepToString(serializable.toArray());
    }
}

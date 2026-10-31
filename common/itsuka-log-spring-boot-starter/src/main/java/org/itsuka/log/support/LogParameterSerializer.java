package org.itsuka.log.support;

import java.lang.reflect.Method;

/**
 * SPI for serializing method arguments into a request parameter snapshot.
 */
public interface LogParameterSerializer {

    /**
     * Serializes the arguments of the given method.
     *
     * @param method the invoked method
     * @param args   the invocation arguments
     * @return the serialized snapshot, or {@code null} when unavailable
     */
    String serialize(Method method, Object[] args);
}

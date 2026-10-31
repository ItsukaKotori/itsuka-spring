package org.itsuka.log.support;

import org.itsuka.log.spi.LogRequestResolver;

/**
 * {@link LogRequestResolver} used when no web request information is available.
 */
public class NoopLogRequestResolver implements LogRequestResolver {

    @Override
    public String getClientIp() {
        return null;
    }

    @Override
    public String getUserAgent() {
        return null;
    }
}

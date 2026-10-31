package org.itsuka.log.spi;

/**
 * SPI for resolving request metadata of an operation log.
 */
public interface LogRequestResolver {

    /**
     * Returns the client IP address.
     *
     * @return the client IP, or {@code null} when unavailable
     */
    String getClientIp();

    /**
     * Returns the User-Agent header value.
     *
     * @return the User-Agent, or {@code null} when unavailable
     */
    String getUserAgent();
}

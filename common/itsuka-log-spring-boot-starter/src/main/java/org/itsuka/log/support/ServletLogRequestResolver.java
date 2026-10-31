package org.itsuka.log.support;

import jakarta.servlet.http.HttpServletRequest;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRequestResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link LogRequestResolver} reading request metadata from the current servlet request.
 *
 * <p>The client IP is taken from {@code getRemoteAddr()}; when forwarded headers are trusted
 * ({@code itsuka.log.request.trust-forwarded-headers=true}) the first value of
 * {@code X-Forwarded-For} is preferred, then {@code X-Real-IP}. The User-Agent is truncated to
 * 512 characters. Without a current request both methods return {@code null}.</p>
 */
public class ServletLogRequestResolver implements LogRequestResolver {

    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final LogRecordProperties properties;

    /**
     * Creates a resolver backed by the given properties.
     *
     * @param properties the operation log properties
     */
    public ServletLogRequestResolver(LogRecordProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        LogRecordProperties.Request requestProps = properties.getRequest();
        if (requestProps != null && requestProps.isTrustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(realIp)) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }

    @Override
    public String getUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return LogRecordTextUtils.truncate(request.getHeader("User-Agent"), USER_AGENT_MAX_LENGTH);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}

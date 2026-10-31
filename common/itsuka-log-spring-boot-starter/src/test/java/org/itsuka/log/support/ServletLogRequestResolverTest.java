package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ServletLogRequestResolverTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void readsRemoteAddrAndUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ServletLogRequestResolver resolver = new ServletLogRequestResolver(new LogRecordProperties());
        assertThat(resolver.getClientIp()).isEqualTo("10.0.0.1");
        assertThat(resolver.getUserAgent()).isEqualTo("JUnit");
    }

    @Test
    void trustsForwardedHeadersWhenEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LogRecordProperties props = new LogRecordProperties();
        props.getRequest().setTrustForwardedHeaders(true);
        assertThat(new ServletLogRequestResolver(props).getClientIp()).isEqualTo("1.2.3.4");
    }
}

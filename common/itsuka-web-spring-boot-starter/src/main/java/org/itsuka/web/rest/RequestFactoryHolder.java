package org.itsuka.web.rest;

import org.springframework.http.client.ClientHttpRequestFactory;

public class RequestFactoryHolder {
    private static final ThreadLocal<ClientHttpRequestFactory> REQUEST_FACTORY_THREAD_LOCAL = new ThreadLocal<>();

    public static ClientHttpRequestFactory get() {
        return REQUEST_FACTORY_THREAD_LOCAL.get();
    }

    public static void set(ClientHttpRequestFactory factory) {
        REQUEST_FACTORY_THREAD_LOCAL.set(factory);
    }

    public static void remove() {
        REQUEST_FACTORY_THREAD_LOCAL.remove();
    }
}

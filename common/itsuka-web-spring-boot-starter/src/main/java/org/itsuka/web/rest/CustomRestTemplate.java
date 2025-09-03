package org.itsuka.web.rest;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class CustomRestTemplate extends RestTemplate {
    @Override
    public ClientHttpRequestFactory getRequestFactory() {
        ClientHttpRequestFactory requestFactory = RequestFactoryHolder.get();
        if (requestFactory != null) {
            return requestFactory;
        }
        return super.getRequestFactory();
    }
}
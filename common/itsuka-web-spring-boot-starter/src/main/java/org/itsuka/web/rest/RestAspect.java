package org.itsuka.web.rest;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.itsuka.web.rest.annotation.Rest;
import org.itsuka.web.rest.annotation.RestProxy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

@Aspect
@RequiredArgsConstructor
public class RestAspect {
    private final RestTemplate restTemplate;

    @Pointcut("@annotation(org.itsuka.web.rest.annotation.Rest)")
    public void restPointcut() {}

    @Around("restPointcut()")
    public Object restWithProxy(ProceedingJoinPoint point) {
        Signature signature = point.getSignature();
        if (!(signature instanceof MethodSignature methodSignature)) {
            throw new AssertionError();
        }
        Rest rest = methodSignature.getMethod().getAnnotation(Rest.class);
        RequestFactoryHolder.set(requestFactory(rest));
        try {
            return point.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            RequestFactoryHolder.remove();
        }
    }

    private ClientHttpRequestFactory requestFactory(Rest rest) {
        RestProxy restProxy = rest.proxy();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(rest.connectTimeout());
        requestFactory.setReadTimeout(rest.readTimeout());
        if (!restProxy.enable()) {
            return getRequestFactory(requestFactory);
        }
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(restProxy.hostname(), restProxy.port()));
        requestFactory.setProxy(proxy);
        BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor(restProxy.username(), restProxy.password());
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add(interceptor);
        return new InterceptingClientHttpRequestFactory(requestFactory, interceptors);
    }

    public ClientHttpRequestFactory getRequestFactory(ClientHttpRequestFactory factory) {
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (!CollectionUtils.isEmpty(interceptors)) {
            return new InterceptingClientHttpRequestFactory(factory, interceptors);
        }
        else {
            return factory;
        }
    }
}

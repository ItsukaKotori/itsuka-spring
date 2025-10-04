package org.itsuka.dict.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 字典响应体处理
 * 用于在响应体写入HTTP响应之前对其进行处理，拦截JSON响应并处理其中的字典字段
 */
@ControllerAdvice
@RequiredArgsConstructor
public class DictResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final DictResponseHandler dictResponseHandler;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 对所有返回类型都进行处理
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 如果响应体为空，直接返回
        if (body == null) {
            return null;
        }

        // 处理响应体中的字典字段
        return dictResponseHandler.processDictFields(body);
    }
}
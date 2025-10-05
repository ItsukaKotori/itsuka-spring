package org.itsuka.core.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
@NoArgsConstructor
public abstract class BaseResponse<T> extends Response<T> {
    /**
     * 请求路径
     */
    private String path;

    public BaseResponse(HttpStatus httpStatus, String message, T data) {
        super(httpStatus, message, data);
    }

    /**
     * 获取响应头
     *
     * @return 响应头Map
     */
    public abstract Map<String, String> getResponseHeaders();
}

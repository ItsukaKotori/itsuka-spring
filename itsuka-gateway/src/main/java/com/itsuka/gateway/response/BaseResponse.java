package com.itsuka.gateway.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public abstract class BaseResponse {
    private int code;

    private String path;

    private String status;

    private String message;
    
    private long timestamp;

    private HttpStatus httpStatus;

    /**
     * 获取响应头
     *
     * @return 响应头Map
     */
    public abstract Map<String, String> getResponseHeaders();
}

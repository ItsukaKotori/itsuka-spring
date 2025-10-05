package org.itsuka.core.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class SuccessResponse<T> extends BaseResponse<T> {
    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param path 请求路径
     * @param <T>  数据类型
     * @return SuccessResponse实例
     */
    public static <T> SuccessResponse<T> of(T data, String path) {
        SuccessResponse<T> response = new SuccessResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setMessage("操作成功");
        response.setTimestamp(System.currentTimeMillis());
        response.setData(data);
        response.setPath(path); 
        response.setHttpStatus(HttpStatus.OK);
        return response;
    }

    @Override
    public HttpStatus getHttpStatus() {
        HttpStatus httpStatus = super.getHttpStatus();
        return httpStatus != null ? httpStatus : HttpStatus.OK;
    }

    /**
     * 获取响应头
     *
     * @return 响应头Map
     */
    @Override
    public Map<String, String> getResponseHeaders() {
        return new HashMap<>();
    }

    // ==================== 便捷静态方法 ====================

    /**
     * 创建并写入成功响应
     *
     * @param path         请求路径
     * @param data         响应数据
     * @param <T>          数据类型
     * @return SuccessResponse实例
     */
    public static <T> SuccessResponse<T> ok(String path, T data) {
        return SuccessResponse.of(data, path);
    }
}

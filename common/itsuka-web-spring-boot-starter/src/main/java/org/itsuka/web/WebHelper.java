package org.itsuka.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domian.LoginUser;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.itsuka.core.constant.Constants.LOGIN_HEADER;

/**
 * Web工具类
 *
 * @author Itsuka
 * @date 2023/2/26
 */
@Slf4j
public class WebHelper {
    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    public Optional<LoginUser> getLoginUser() {
        Optional<String> optional = getHeader(LOGIN_HEADER);
        if (optional.isEmpty() || ObjectUtils.isEmpty(optional.get())) {
            return Optional.empty();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Optional.ofNullable(mapper.readValue(optional.get(), LoginUser.class));
        } catch (JsonProcessingException e) {
            log.error("jackson read value error", e);
        }
        return Optional.empty();
    }

    /**
     * 获取请求头
     *
     * @param headerName 请求头Key
     * @return 请求头Value
     */
    public Optional<String> getHeader(String headerName) {
        HttpServletRequest request = getRequest();
        return Optional.ofNullable(request.getHeader(headerName));
    }

    /**
     * 获取请求
     *
     * @return 请求
     */
    public HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttr) {
            return servletAttr.getRequest();
        }
        throw new IllegalStateException();
    }
}

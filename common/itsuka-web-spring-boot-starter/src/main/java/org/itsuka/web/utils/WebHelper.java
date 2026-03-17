package org.itsuka.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.LoginUser;
import org.itsuka.core.exception.AuthenticationException;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.itsuka.core.constant.Constants.LOGIN_HEADER;

/**
 * Web工具类
 *
 * @author Itsuka
 * @since 2023/2/26
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
        JsonMapper mapper = new JsonMapper();
        return Optional.ofNullable(mapper.readValue(optional.get(), LoginUser.class));
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    public LoginUser mustGetLoginUser() {
        Optional<LoginUser> optional = getLoginUser();
        if (optional.isEmpty()) {
            throw new AuthenticationException("无法获取用户信息！");
        }
        return optional.get();
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

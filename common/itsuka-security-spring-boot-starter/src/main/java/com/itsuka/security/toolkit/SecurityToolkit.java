package com.itsuka.security.toolkit;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

/**
 * 认证工具类
 *
 * @author Itsuka
 */
@UtilityClass
public class SecurityToolkit {

    /**
     * 从Authorization头中提取JWT令牌
     *
     * @param authorizationHeader Authorization头的值
     * @return JWT令牌，如果格式不正确则返回null
     */
    public static String extractToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring(7);
    }
    
    /**
     * 检查路径是否在排除列表中
     *
     * @param path 请求路径
     * @param excludedPaths 排除路径列表
     * @return 是否排除
     */
    public static boolean isExcludedPath(String path, java.util.List<String> excludedPaths) {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            return false;
        }
        
        return excludedPaths.stream()
                .anyMatch(excludedPath -> {
                    // 支持通配符匹配
                    if (excludedPath.endsWith("/**")) {
                        String prefix = excludedPath.substring(0, excludedPath.length() - 3);
                        return path.startsWith(prefix);
                    } else if (excludedPath.endsWith("/*")) {
                        String prefix = excludedPath.substring(0, excludedPath.length() - 2);
                        return path.startsWith(prefix) && !path.substring(prefix.length()).contains("/");
                    } else {
                        return path.equals(excludedPath);
                    }
                });
    }
}
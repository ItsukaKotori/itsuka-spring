package com.itsuka.security.toolkit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;

/**
 * IP地址获取工具类
 * 提供从HTTP请求中获取客户端真实IP地址的方法
 *
 * @author itsuka
 */
@Slf4j
public class IpAddressToolkit {

    /**
     * 常见的代理头列表，按优先级排序
     */
    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 未知IP标识
     */
    private static final String UNKNOWN = "unknown";

    /**
     * 从WebFlux ServerHttpRequest获取客户端真实IP地址
     * 
     * @param request ServerHttpRequest对象
     * @return 客户端真实IP地址，获取失败时返回"unknown"
     */
    public static String getClientIp(ServerHttpRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        // 按优先级依次检查代理头
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeaders().getFirst(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For可能包含多个IP，取第一个
                if ("X-Forwarded-For".equals(header) && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.debug("Client IP obtained from header {}: {}", header, ip);
                return ip;
            }
        }

        // 最后从远程地址获取
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : UNKNOWN;
        log.debug("Client IP obtained from remote address: {}", ip);
        return ip;
    }

    /**
     * 验证IP地址是否有效
     * 
     * @param ip IP地址字符串
     * @return true表示有效，false表示无效
     */
    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) 
                && !UNKNOWN.equalsIgnoreCase(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip); // 排除IPv6本地地址
    }

    /**
     * 检查IP地址是否为内网地址
     * 
     * @param ip IP地址
     * @return true表示是内网地址，false表示是公网地址
     */
    public static boolean isInternalIp(String ip) {
        if (!StringUtils.hasText(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }

        // 本地回环地址
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            return true;
        }

        // IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }

        // 检查IPv4内网地址段
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            int firstOctet = Integer.parseInt(parts[0]);
            int secondOctet = Integer.parseInt(parts[1]);

            // 10.0.0.0/8
            if (firstOctet == 10) {
                return true;
            }

            // 172.16.0.0/12
            if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
                return true;
            }

            // 192.168.0.0/16
            if (firstOctet == 192 && secondOctet == 168) {
                return true;
            }

            // 169.254.0.0/16 (链路本地地址)
            if (firstOctet == 169 && secondOctet == 254) {
                return true;
            }

            return false;
        } catch (NumberFormatException e) {
            log.warn("Invalid IP address format: {}", ip);
            return false;
        }
    }

    /**
     * 格式化IP地址用于日志输出
     * 对于内网IP直接返回，对于公网IP进行脱敏处理
     * 
     * @param ip 原始IP地址
     * @return 格式化后的IP地址
     */
    public static String formatIpForLog(String ip) {
        if (!StringUtils.hasText(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        // 内网IP不需要脱敏
        if (isInternalIp(ip)) {
            return ip;
        }

        // 公网IP进行脱敏处理
        try {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***." + parts[3];
            }
        } catch (Exception e) {
            log.warn("Failed to format IP for log: {}", ip, e);
        }

        return ip;
    }
}
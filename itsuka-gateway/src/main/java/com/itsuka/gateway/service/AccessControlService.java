package com.itsuka.gateway.service;

import com.itsuka.gateway.config.AccessControlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 访问控制服务
 *
 * @author itsuka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AccessControlProperties accessControlProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 检查请求是否被允许访问
     *
     * @param path      请求路径
     * @param clientIp  客户端IP
     * @param userAgent User-Agent
     * @return true表示允许访问，false表示拒绝访问
     */
    public boolean isAccessAllowed(String path, String clientIp, String userAgent) {
        if (!accessControlProperties.isEnabled()) {
            return true;
        }

        // 先检查黑名单
        if (isBlacklisted(path, clientIp, userAgent)) {
            log.warn("Request blocked by blacklist - Path: {}, IP: {}, UserAgent: {}", path, clientIp, userAgent);
            return false;
        }

        // 再检查白名单
        if (accessControlProperties.getWhitelist().isEnabled()) {
            boolean whitelisted = isWhitelisted(path, clientIp, userAgent);
            if (!whitelisted) {
                log.warn("Request blocked by whitelist - Path: {}, IP: {}, UserAgent: {}", path, clientIp, userAgent);
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否在黑名单中
     */
    private boolean isBlacklisted(String path, String clientIp, String userAgent) {
        AccessControlProperties.Blacklist blacklist = accessControlProperties.getBlacklist();
        if (!blacklist.isEnabled()) {
            return false;
        }

        // 检查黑名单路径
        if (isPathMatched(path, blacklist.getPaths())) {
            return true;
        }

        // 检查黑名单IP
        if (isIpMatched(clientIp, blacklist.getIps())) {
            return true;
        }

        // 检查黑名单User-Agent
        if (isUserAgentMatched(userAgent, blacklist.getUserAgents())) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isWhitelisted(String path, String clientIp, String userAgent) {
        AccessControlProperties.Whitelist whitelist = accessControlProperties.getWhitelist();

        // 检查白名单路径
        if (isPathMatched(path, whitelist.getPaths())) {
            return true;
        }

        // 检查白名单IP
        if (isIpMatched(clientIp, whitelist.getIps())) {
            return true;
        }

        // 检查白名单User-Agent
        if (isUserAgentMatched(userAgent, whitelist.getUserAgents())) {
            return true;
        }

        return false;
    }

    /**
     * 检查路径是否匹配
     */
    private boolean isPathMatched(String path, List<String> patterns) {
        if (CollectionUtils.isEmpty(patterns) || !StringUtils.hasText(path)) {
            return false;
        }

        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查IP是否匹配
     */
    private boolean isIpMatched(String clientIp, List<String> ipPatterns) {
        if (CollectionUtils.isEmpty(ipPatterns) || !StringUtils.hasText(clientIp)) {
            return false;
        }

        for (String ipPattern : ipPatterns) {
            if (isIpInRange(clientIp, ipPattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查User-Agent是否匹配
     */
    private boolean isUserAgentMatched(String userAgent, List<String> userAgentPatterns) {
        if (CollectionUtils.isEmpty(userAgentPatterns) || !StringUtils.hasText(userAgent)) {
            return false;
        }

        for (String pattern : userAgentPatterns) {
            if (pathMatcher.match(pattern, userAgent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查IP是否在指定范围内
     * 支持单个IP、CIDR格式和通配符
     */
    private boolean isIpInRange(String clientIp, String ipPattern) {
        try {
            // 直接匹配
            if (clientIp.equals(ipPattern)) {
                return true;
            }

            // 通配符匹配
            if (pathMatcher.match(ipPattern, clientIp)) {
                return true;
            }

            // CIDR格式匹配
            if (ipPattern.contains("/")) {
                return isIpInCidr(clientIp, ipPattern);
            }

            return false;
        } catch (Exception e) {
            log.warn("Error checking IP range for {} against pattern {}: {}", clientIp, ipPattern, e.getMessage());
            return false;
        }
    }

    /**
     * 检查IP是否在CIDR范围内
     */
    private boolean isIpInCidr(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            InetAddress targetAddr = InetAddress.getByName(clientIp);
            InetAddress networkAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] targetBytes = targetAddr.getAddress();
            byte[] networkBytes = networkAddr.getAddress();

            if (targetBytes.length != networkBytes.length) {
                return false;
            }

            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;

            // 检查完整的字节
            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            // 检查剩余的位
            if (bitsToCheck > 0 && bytesToCheck < targetBytes.length) {
                int mask = 0xFF << (8 - bitsToCheck);
                return (targetBytes[bytesToCheck] & mask) == (networkBytes[bytesToCheck] & mask);
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Error parsing CIDR {}: {}", cidr, e.getMessage());
            return false;
        }
    }
}
package com.itsuka.gateway.service;

import com.itsuka.gateway.config.AccessControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 访问控制服务测试
 *
 * @author itsuka
 */
@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    private AccessControlService accessControlService;
    private AccessControlProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AccessControlProperties();
        accessControlService = new AccessControlService(properties);
    }

    @Test
    void testDisabledAccessControl() {
        // 禁用访问控制时应该始终允许访问
        properties.setEnabled(false);
        
        assertTrue(accessControlService.isAccessAllowed("/any/path", "192.168.1.1", "Chrome"));
    }

    @Test
    void testBlacklistPaths() {
        properties.setEnabled(true);
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setPaths(Arrays.asList("/api/admin/**", "/internal/*"));

        // 测试黑名单路径
        assertFalse(accessControlService.isAccessAllowed("/api/admin/users", "127.0.0.1", "Chrome"));
        assertFalse(accessControlService.isAccessAllowed("/internal/config", "127.0.0.1", "Chrome"));
        
        // 测试非黑名单路径
        assertTrue(accessControlService.isAccessAllowed("/api/public/users", "127.0.0.1", "Chrome"));
    }

    @Test
    void testBlacklistIps() {
        properties.setEnabled(true);
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setIps(Arrays.asList("192.168.1.100", "10.0.0.0/8"));

        // 测试黑名单IP
        assertFalse(accessControlService.isAccessAllowed("/api/test", "192.168.1.100", "Chrome"));
        assertFalse(accessControlService.isAccessAllowed("/api/test", "10.0.0.1", "Chrome"));
        
        // 测试非黑名单IP
        assertTrue(accessControlService.isAccessAllowed("/api/test", "192.168.1.101", "Chrome"));
        assertTrue(accessControlService.isAccessAllowed("/api/test", "172.16.0.1", "Chrome"));
    }

    @Test
    void testBlacklistUserAgents() {
        properties.setEnabled(true);
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setUserAgents(Arrays.asList("*bot*", "*spider*"));

        // 测试黑名单User-Agent
        assertFalse(accessControlService.isAccessAllowed("/api/test", "127.0.0.1", "Googlebot"));
        assertFalse(accessControlService.isAccessAllowed("/api/test", "127.0.0.1", "spider-crawler"));
        
        // 测试非黑名单User-Agent
        assertTrue(accessControlService.isAccessAllowed("/api/test", "127.0.0.1", "Chrome"));
    }

    @Test
    void testWhitelistPaths() {
        properties.setEnabled(true);
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().setPaths(Arrays.asList("/api/public/**", "/health"));

        // 测试白名单路径
        assertTrue(accessControlService.isAccessAllowed("/api/public/users", "127.0.0.1", "Chrome"));
        assertTrue(accessControlService.isAccessAllowed("/health", "127.0.0.1", "Chrome"));
        
        // 测试非白名单路径
        assertFalse(accessControlService.isAccessAllowed("/api/private/users", "127.0.0.1", "Chrome"));
    }

    @Test
    void testWhitelistIps() {
        properties.setEnabled(true);
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().setIps(Arrays.asList("127.0.0.1", "192.168.1.0/24"));

        // 测试白名单IP
        assertTrue(accessControlService.isAccessAllowed("/api/test", "127.0.0.1", "Chrome"));
        assertTrue(accessControlService.isAccessAllowed("/api/test", "192.168.1.100", "Chrome"));
        
        // 测试非白名单IP
        assertFalse(accessControlService.isAccessAllowed("/api/test", "10.0.0.1", "Chrome"));
    }

    @Test
    void testBlacklistOverridesWhitelist() {
        properties.setEnabled(true);
        
        // 启用白名单
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().setPaths(Arrays.asList("/api/**"));
        
        // 启用黑名单
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setPaths(Arrays.asList("/api/admin/**"));

        // 白名单包含/api/**，但黑名单排除/api/admin/**
        assertTrue(accessControlService.isAccessAllowed("/api/users", "127.0.0.1", "Chrome"));
        assertFalse(accessControlService.isAccessAllowed("/api/admin/users", "127.0.0.1", "Chrome"));
    }
}
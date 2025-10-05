package com.itsuka.security.manager;

import com.itsuka.security.autoconfigure.AccessControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 访问控制管理器测试
 *
 * @author itsuka
 */
@ExtendWith(MockitoExtension.class)
class AccessControlManagerTest {

    private AccessControlManager accessControlManager;
    private AccessControlProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AccessControlProperties();
        accessControlManager = new AccessControlManager(properties, new AntPathMatcher());
    }

    @Test
    void testDisabledAccessControl() {
        // 禁用访问控制时应该始终允许访问
        properties.setEnabled(false);
        
        assertTrue(accessControlManager.isAccessAllowed("/any/path", "192.168.1.1", "Chrome"));
    }

    @Test
    void testBlacklistPaths() {
        properties.setEnabled(true);
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setPaths(Arrays.asList("/api/admin/**", "/internal/*"));

        // 测试黑名单路径
        assertFalse(accessControlManager.isAccessAllowed("/api/admin/users", "127.0.0.1", "Chrome"));
        assertFalse(accessControlManager.isAccessAllowed("/internal/config", "127.0.0.1", "Chrome"));
        
        // 测试非黑名单路径
        assertTrue(accessControlManager.isAccessAllowed("/api/public/users", "127.0.0.1", "Chrome"));
    }

    @Test
    void testBlacklistIps() {
        properties.setEnabled(true);
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setIps(Arrays.asList("192.168.1.100", "10.0.0.0/8"));

        // 测试黑名单IP
        assertFalse(accessControlManager.isAccessAllowed("/api/test", "192.168.1.100", "Chrome"));
        assertFalse(accessControlManager.isAccessAllowed("/api/test", "10.0.0.1", "Chrome"));
        
        // 测试非黑名单IP
        assertTrue(accessControlManager.isAccessAllowed("/api/test", "192.168.1.101", "Chrome"));
        assertTrue(accessControlManager.isAccessAllowed("/api/test", "172.16.0.1", "Chrome"));
    }

    @Test
    void testBlacklistUserAgents() {
        properties.setEnabled(true);
        properties.getBlacklist().setEnabled(true);
        properties.getBlacklist().setUserAgents(Arrays.asList("*bot*", "*spider*"));

        // 测试黑名单User-Agent
        assertFalse(accessControlManager.isAccessAllowed("/api/test", "127.0.0.1", "Googlebot"));
        assertFalse(accessControlManager.isAccessAllowed("/api/test", "127.0.0.1", "spider-crawler"));
        
        // 测试非黑名单User-Agent
        assertTrue(accessControlManager.isAccessAllowed("/api/test", "127.0.0.1", "Chrome"));
    }

    @Test
    void testWhitelistPaths() {
        properties.setEnabled(true);
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().setPaths(Arrays.asList("/api/public/**", "/health"));

        // 测试白名单路径
        assertTrue(accessControlManager.isAccessAllowed("/api/public/users", "127.0.0.1", "Chrome"));
        assertTrue(accessControlManager.isAccessAllowed("/health", "127.0.0.1", "Chrome"));
        
        // 测试非白名单路径
        assertFalse(accessControlManager.isAccessAllowed("/api/private/users", "127.0.0.1", "Chrome"));
    }

    @Test
    void testWhitelistIps() {
        properties.setEnabled(true);
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().setIps(Arrays.asList("127.0.0.1", "192.168.1.0/24"));

        // 测试白名单IP
        assertTrue(accessControlManager.isAccessAllowed("/api/test", "127.0.0.1", "Chrome"));
        assertTrue(accessControlManager.isAccessAllowed("/api/test", "192.168.1.100", "Chrome"));
        
        // 测试非白名单IP
        assertFalse(accessControlManager.isAccessAllowed("/api/test", "10.0.0.1", "Chrome"));
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
        assertTrue(accessControlManager.isAccessAllowed("/api/users", "127.0.0.1", "Chrome"));
        assertFalse(accessControlManager.isAccessAllowed("/api/admin/users", "127.0.0.1", "Chrome"));
    }
}
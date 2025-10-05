package com.itsuka.security.toolkit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IP地址工具类测试
 *
 * @author itsuka
 */
class IpAddressToolkitTest {

    @Test
    void testGetClientIpFromXForwardedFor() {
        ServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1")
                .build();

        String clientIp = IpAddressToolkit.getClientIp(request);
        assertEquals("192.168.1.100", clientIp);
    }

    @Test
    void testGetClientIpFromXRealIp() {
        ServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Real-IP", "192.168.1.100")
                .build();

        String clientIp = IpAddressToolkit.getClientIp(request);
        assertEquals("192.168.1.100", clientIp);
    }

    @Test
    void testGetClientIpWithUnknownValues() {
        ServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "unknown")
                .header("X-Real-IP", "192.168.1.100")
                .build();

        String clientIp = IpAddressToolkit.getClientIp(request);
        assertEquals("192.168.1.100", clientIp);
    }

    @Test
    void testGetClientIpFromRemoteAddress() {
        ServerHttpRequest request = MockServerHttpRequest.get("/test")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build();

        String clientIp = IpAddressToolkit.getClientIp(request);
        assertEquals("192.168.1.100", clientIp);
    }

    @Test
    void testGetClientIpWithNullRequest() {
        String clientIp = IpAddressToolkit.getClientIp((ServerHttpRequest) null);
        assertEquals("unknown", clientIp);
    }

    @Test
    void testIsInternalIp() {
        // 测试本地回环地址
        assertTrue(IpAddressToolkit.isInternalIp("127.0.0.1"));
        assertTrue(IpAddressToolkit.isInternalIp("localhost"));

        // 测试IPv6本地地址
        assertTrue(IpAddressToolkit.isInternalIp("0:0:0:0:0:0:0:1"));
        assertTrue(IpAddressToolkit.isInternalIp("::1"));

        // 测试内网地址段
        assertTrue(IpAddressToolkit.isInternalIp("10.0.0.1"));
        assertTrue(IpAddressToolkit.isInternalIp("172.16.0.1"));
        assertTrue(IpAddressToolkit.isInternalIp("192.168.1.1"));
        assertTrue(IpAddressToolkit.isInternalIp("169.254.1.1"));

        // 测试公网地址
        assertFalse(IpAddressToolkit.isInternalIp("8.8.8.8"));
        assertFalse(IpAddressToolkit.isInternalIp("114.114.114.114"));

        // 测试边界情况
        assertFalse(IpAddressToolkit.isInternalIp("172.15.0.1")); // 不在172.16-31范围内
        assertFalse(IpAddressToolkit.isInternalIp("172.32.0.1")); // 不在172.16-31范围内

        // 测试无效输入
        assertFalse(IpAddressToolkit.isInternalIp(""));
        assertFalse(IpAddressToolkit.isInternalIp("unknown"));
        assertFalse(IpAddressToolkit.isInternalIp("invalid.ip"));
    }

    @Test
    void testFormatIpForLog() {
        // 内网IP不脱敏
        assertEquals("192.168.1.1", IpAddressToolkit.formatIpForLog("192.168.1.1"));
        assertEquals("10.0.0.1", IpAddressToolkit.formatIpForLog("10.0.0.1"));
        assertEquals("127.0.0.1", IpAddressToolkit.formatIpForLog("127.0.0.1"));

        // 公网IP脱敏
        assertEquals("8.8.***.8", IpAddressToolkit.formatIpForLog("8.8.8.8"));
        assertEquals("114.114.***.114", IpAddressToolkit.formatIpForLog("114.114.114.114"));

        // 特殊值
        assertEquals("unknown", IpAddressToolkit.formatIpForLog("unknown"));
        assertEquals("", IpAddressToolkit.formatIpForLog(""));
    }

    @Test
    void testHeaderPriority() {
        // 测试头部优先级：X-Forwarded-For > X-Real-IP > 其他
        ServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "1.1.1.1")
                .header("X-Real-IP", "2.2.2.2")
                .header("Proxy-Client-IP", "3.3.3.3")
                .build();

        String clientIp = IpAddressToolkit.getClientIp(request);
        assertEquals("1.1.1.1", clientIp);
    }

    @Test
    void testMultipleIpsInXForwardedFor() {
        ServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2, 3.3.3.3")
                .build();

        String clientIp = IpAddressToolkit.getClientIp(request);
        assertEquals("1.1.1.1", clientIp);
    }
}
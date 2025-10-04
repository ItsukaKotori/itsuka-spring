package com.itsuka.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 网关应用启动测试
 *
 * @author itsuka
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.gateway.enabled=false",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        // 验证Spring上下文能够正常加载
    }

}
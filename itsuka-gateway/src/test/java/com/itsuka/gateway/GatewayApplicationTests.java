package com.itsuka.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
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
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    RedisReactiveAutoConfiguration.class
})
@TestPropertySource(properties = {
    "spring.cloud.gateway.enabled=false",
    "spring.cloud.loadbalancer.ribbon.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
})
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        // 验证Spring上下文能够正常加载
    }

}
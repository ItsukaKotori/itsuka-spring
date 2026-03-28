plugins {
    id("java-library")
}

dependencies {
    // Spring Security Core (compileOnly - only used in LoginUser interface)
    compileOnly("org.springframework.security:spring-security-core")

    // Jackson (Jackson 3.x for Spring Boot 4.x)
    // JavaTimeModule 已内置到 jackson-databind 3.x，无需额外依赖
    api("org.springframework.boot:spring-boot-starter-jackson")

    // Spring Web
    implementation("org.springframework:spring-web")

    // Spring Boot AutoConfiguration
    api("org.springframework.boot:spring-boot-autoconfigure")
}

// 配置测试任务
tasks.test {
    useJUnitPlatform()
}

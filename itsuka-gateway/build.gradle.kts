plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
}

description = "itsuka-gateway"

dependencies {
    implementation(project(":common:itsuka-spring-boot-starter"))
    implementation(project(":common:itsuka-redis-spring-boot-starter"))

    // Spring Cloud Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    // Spring Security (for UserDetails interfaces)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // LoadBalancer for service discovery
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    // Itsuka Security Starter
    implementation(project(":common:itsuka-security-spring-boot-starter"))

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Swagger UI (WebFlux) for API文档聚合展示
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui")
}

tasks.test {
    useJUnitPlatform()
}

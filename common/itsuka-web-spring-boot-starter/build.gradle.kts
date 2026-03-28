plugins {
    id("java-library")
}

dependencies {
    // Jakarta Validation
    api("jakarta.validation:jakarta.validation-api")
    
    // Spring Boot Web Starter
    api("org.springframework.boot:spring-boot-starter-webmvc")
    
    api("org.springframework.boot:spring-boot-starter-jackson")

    // Spring Boot AOP
    api("org.springframework.boot:spring-boot-starter-aspectj")

    // Spring Security Core (for SecurityException in GlobalExceptionHandler)
    implementation("org.springframework.security:spring-security-core")
    
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
}

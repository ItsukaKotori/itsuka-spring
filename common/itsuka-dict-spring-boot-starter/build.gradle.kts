dependencies {
    // Spring Boot AOP
    api("org.springframework.boot:spring-boot-starter-aspectj")
    
    // Spring Boot Web (需要用于ResponseBodyAdvice等)
    api("org.springframework.boot:spring-boot-starter-webmvc")
    
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
    compileOnly(project(":common:itsuka-redis-spring-boot-starter"))
}

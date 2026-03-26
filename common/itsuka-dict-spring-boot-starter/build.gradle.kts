dependencies {
    // Spring Boot AOP
    api("org.springframework.boot:spring-boot-starter-aspectj")
    
    // Spring Boot Web (需要用于ResponseBodyAdvice等)
    api("org.springframework.boot:spring-boot-starter-webmvc")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
    
    // 可选依赖 - Redis (provided scope)
    compileOnly(project(":common:itsuka-redis-spring-boot-starter"))
    compileOnly("org.springframework.data:spring-data-redis")
}

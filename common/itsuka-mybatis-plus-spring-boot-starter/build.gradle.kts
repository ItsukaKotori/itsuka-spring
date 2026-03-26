dependencies {
    // MySQL 连接器
    api("com.mysql:mysql-connector-j")
    
    // Spring Boot Starter
    api("org.springframework.boot:spring-boot-starter")
    
    // Spring Security Core (for UserDetails)
    compileOnly("org.springframework.security:spring-security-core")
    
    // MyBatis Plus
    api("com.baomidou:mybatis-plus-spring-boot4-starter")
    api("com.baomidou:mybatis-plus-jsqlparser")

    // Jackson (for JSON annotations)
    api("com.fasterxml.jackson.core:jackson-annotations")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // 本项目依赖 - 直接依赖itsuka-core
    api(project(":common:itsuka-spring-boot-starter"))
    api(project(":common:itsuka-web-spring-boot-starter"))
}

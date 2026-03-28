dependencies {
    // Spring Security Core (for UserDetails)
    compileOnly("org.springframework.security:spring-security-core")
    
    // MyBatis Plus
    api("com.baomidou:mybatis-plus-spring-boot4-starter")
    api("com.baomidou:mybatis-plus-jsqlparser")
    
    // 本项目依赖 - 直接依赖itsuka-core
    api(project(":common:itsuka-spring-boot-starter"))
    api(project(":common:itsuka-web-spring-boot-starter"))
}

dependencies {
    // Spring Boot Mail Starter
    api("org.springframework.boot:spring-boot-starter-mail")

    // JavaMail API
    api("com.sun.mail:jakarta.mail:2.0.1")
    
    // Jakarta Activation API
    api("jakarta.activation:jakarta.activation-api")
    
    // FreeMarker Template Engine (可选)
    compileOnly("org.freemarker:freemarker:2.3.32")
    
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
}

// 配置测试任务
tasks.test {
    useJUnitPlatform()
}

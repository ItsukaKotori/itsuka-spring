dependencies {
    // Spring Boot Mail Starter
    api("org.springframework.boot:spring-boot-starter-mail")
    
    // Spring Boot Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Spring Boot AutoConfiguration
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // JavaMail API
    api("com.sun.mail:jakarta.mail:2.0.1")
    
    // Jakarta Activation API
    implementation("jakarta.activation:jakarta.activation-api")
    
    // FreeMarker Template Engine (可选)
    compileOnly("org.freemarker:freemarker:2.3.32")
    
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
    
    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

// 配置测试任务
tasks.test {
    useJUnitPlatform()
}

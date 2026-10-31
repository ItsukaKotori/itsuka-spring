dependencies {
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))

    // Spring AOP
    api("org.springframework.boot:spring-boot-starter-aspectj")

    // JDBC
    api("org.springframework.boot:spring-boot-starter-jdbc")

    // 可选：请求信息采集
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")

    // 可选：登录用户解析
    compileOnly("org.springframework.security:spring-security-core")

    // 可选：参数序列化
    compileOnly("org.springframework.boot:spring-boot-starter-jackson")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    // Spring Boot 4 管理的 Testcontainers 2.x 使用 testcontainers-* 坐标
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}

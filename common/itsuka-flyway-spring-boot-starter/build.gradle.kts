dependencies {
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
    api("org.springframework.boot:spring-boot-starter-flyway")

    // Flyway 11 的 MySQL 支持在独立模块中；放入 runtime 让 MySQL 项目开箱即用。
    // PostgreSQL 等其他数据库由接入方自行添加对应的 Flyway 数据库模块。
    runtimeOnly("org.flywaydb:flyway-mysql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    // Spring Boot 4 管理的 Testcontainers 2.x 使用 testcontainers-* 坐标
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.flywaydb:flyway-mysql")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}

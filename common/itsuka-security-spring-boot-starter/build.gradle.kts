dependencies {
    // JWT dependencies
    api("io.jsonwebtoken:jjwt-api")
    api("io.jsonwebtoken:jjwt-impl")
    api("io.jsonwebtoken:jjwt-jackson")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Jakarta Validation
    api("jakarta.validation:jakarta.validation-api")

    // Project Dependencies
    api(project(":common:itsuka-spring-boot-starter"))
    implementation(project(":common:itsuka-redis-spring-boot-starter"))
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    // Spring Boot Web (optional)
    compileOnly("org.springframework:spring-web")

    // MinIO - 使用api确保消费者能访问MinIO类
    api("io.minio:minio")
    
    // 本项目依赖
    api(project(":common:itsuka-spring-boot-starter"))
}

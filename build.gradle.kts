import java.util.Properties

plugins {
    id("io.freefair.lombok") version "8.10.2"
    id("java-library")
    id("org.springframework.boot") version "4.0.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("maven-publish")
    id("signing")
}

// 加载Nexus配置文件
val nexusConfig = Properties()
file("nexus.properties").inputStream().use { nexusConfig.load(it) }

// 各模块POM描述（GitHub Packages的"About this package"取自此处）
val moduleDescriptions = mapOf(
    "itsuka-spring-boot-starter" to "Itsuka 基础 starter：统一响应体、JSON 配置与通用工具类",
    "itsuka-web-spring-boot-starter" to "Web 层增强：分页 DTO、全局异常处理、脱敏序列化与 Web 工具类",
    "itsuka-mybatis-plus-spring-boot-starter" to "MyBatis-Plus 集成：分页工具、公共字段自动填充与数据权限拦截",
    "itsuka-redis-spring-boot-starter" to "Redis 集成：响应式/阻塞式模板、多库切换与缓存配置",
    "itsuka-dict-spring-boot-starter" to "数据字典：本地/Redis 缓存与字典模型装配",
    "itsuka-file-spring-boot-starter" to "文件存储：MinIO 客户端封装与自动配置",
    "itsuka-mail-spring-boot-starter" to "邮件发送：多协议 MailSender 与自动配置",
    "itsuka-security-spring-boot-starter" to "安全：JWT 令牌、黑名单与访问控制管理",
    "itsuka-log-spring-boot-starter" to "操作日志：注解式记录、SpEL 模板、MySQL 存储与分页查询",
    "itsuka-gateway" to "GeekWaves 网关：鉴权、限流与访问控制过滤器"
)

// 全局配置
allprojects {
    group = "org.itsuka"
    version = if (project.hasProperty("version")) project.version else "1.0.0-SNAPSHOT"
    
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
        maven { url = uri("https://maven.aliyun.com/repository/spring-plugin") }
        mavenCentral()
    }
}

// 子项目通用配置
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "io.freefair.lombok")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    // 编码设置
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    
    // 依赖管理
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
        }
        
        dependencies {
            dependency("org.mapstruct:mapstruct:1.6.3")
            dependency("org.mapstruct:mapstruct-processor:1.6.3")
            dependency("com.mysql:mysql-connector-j:8.0.33")
            dependency("org.projectlombok:lombok:1.18.30")
            dependency("jakarta.validation:jakarta.validation-api:3.1.1")
            dependency("com.baomidou:mybatis-plus-spring-boot4-starter:3.5.16")
            dependency("com.baomidou:mybatis-plus-jsqlparser:3.5.16")
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
            dependency("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.13")
            dependency("io.minio:minio:8.6.0")
            dependency("io.jsonwebtoken:jjwt-api:0.13.0")
            dependency("io.jsonwebtoken:jjwt-impl:0.13.0")
            dependency("io.jsonwebtoken:jjwt-jackson:0.13.0")
            dependency("jakarta.activation:jakarta.activation-api:2.1.0")
        }
    }
    
    // 测试配置
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    
    // 为所有子项目添加源码和javadoc任务
    tasks.register("sourcesJar", Jar::class) {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }
    
    tasks.register("javadocJar", Jar::class) {
        from(tasks.javadoc)
        archiveClassifier.set("javadoc")
    }
    
    // 配置javadoc
    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
        
        // 忽略Javadoc警告和错误
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        (options as StandardJavadocDocletOptions).addStringOption("encoding", "UTF-8")
        (options as StandardJavadocDocletOptions).addStringOption("charSet", "UTF-8")
        isFailOnError = false
    }
}

// 为itsuka-开头的子项目添加发布配置
configure(subprojects.filter { it.name.startsWith("itsuka-") }) {
    apply(plugin = "maven-publish")
    
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                
                // 添加源码jar
                artifact(tasks.named("sourcesJar"))
                
                // 添加javadoc jar
                artifact(tasks.named("javadocJar"))
                
                // 解析依赖版本
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
                
                // POM文件配置
                pom {
                    name.set(project.name)
                    description.set(moduleDescriptions[project.name] ?: "Itsuka Spring Boot Starter - ${project.name}")
                    url.set("https://github.com/ItsukaKotori/itsuka-spring")
                    
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("itsuka")
                            name.set("Itsuka Team")
                            email.set("support@itsuka.com")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:git://github.com/ItsukaKotori/itsuka-spring.git")
                        developerConnection.set("scm:git:ssh://git@github.com/ItsukaKotori/itsuka-spring.git")
                        url.set("https://github.com/ItsukaKotori/itsuka-spring")
                    }
                }
            }
        }
        
        repositories {
            maven {
                name = "nexus"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) {
                    nexusConfig["nexus.repository.snapshots"] as String
                } else {
                    nexusConfig["nexus.repository.releases"] as String
                })
                
                credentials {
                    username = nexusConfig["nexus.username"] as String
                    password = nexusConfig["nexus.password"] as String
                }
                
                // 允许不安全的协议（仅用于开发环境）
                isAllowInsecureProtocol = true
            }
            
            // GitHub Packages发布仓库
            maven {
                name = "githubPackages"
                url = uri("https://maven.pkg.github.com/ItsukaKotori/itsuka-spring")
                
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

// 根项目不构建 jar
tasks.jar {
    enabled = false
}

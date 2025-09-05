package com.itsuka.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Itsuka
 * @since 2023/3/18
 */
@SpringBootApplication
@MapperScan(basePackages = "com.itsuka.system.mapper")
public class SystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}

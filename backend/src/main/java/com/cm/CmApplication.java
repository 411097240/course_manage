package com.cm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cm.mapper")
public class CmApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmApplication.class, args);
    }
}

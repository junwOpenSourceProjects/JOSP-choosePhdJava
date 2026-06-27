package com.choosephd;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.choosephd.repository")
@EnableScheduling
public class ChoosePhdApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChoosePhdApplication.class, args);
    }
}

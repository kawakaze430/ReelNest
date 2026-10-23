package com.reelnest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReelNestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReelNestApplication.class, args);
    }
}

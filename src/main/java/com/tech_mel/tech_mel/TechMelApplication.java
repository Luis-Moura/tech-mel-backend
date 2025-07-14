package com.tech_mel.tech_mel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechMelApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechMelApplication.class, args);
    }
}
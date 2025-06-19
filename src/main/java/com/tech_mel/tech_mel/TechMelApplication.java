package com.tech_mel.tech_mel;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Objects;

@SpringBootApplication
public class TechMelApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechMelApplication.class, args);
    }
}
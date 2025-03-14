package com.tech_mel.tech_mel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TechMelApplication {

	public static void main(String[] args) {
//		Dotenv dotenv = Dotenv.load();
		SpringApplication.run(TechMelApplication.class, args);
	}

}

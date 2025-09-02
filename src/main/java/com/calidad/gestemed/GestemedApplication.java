package com.calidad.gestemed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestemedApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestemedApplication.class, args);
	}

}

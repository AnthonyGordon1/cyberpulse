package com.anthonygordon.cyberpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class CyberpulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CyberpulseApplication.class, args);
	}

}

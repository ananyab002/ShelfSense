package com.shelf_sense_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShelfSenseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShelfSenseBackendApplication.class, args);
		System.out.println("\nApplication Started! Email polling is scheduled.");
	}

}

package com.example.DoAn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DoAnApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoAnApplication.class, args);
	}

}

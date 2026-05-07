package com.example.DoAn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DoAnApplication {

	@PostConstruct
	public void init() {
		// Set timezone to Vietnam time (GMT+7) for the entire application.
		// This prevents date-shift bugs when hosting on servers in different timezones (e.g. Singapore/UTC).
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}

	public static void main(String[] args) {
		SpringApplication.run(DoAnApplication.class, args);
	}

}

package com.ecolink.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EcopointsP2Application {

	public static void main(String[] args) {
		SpringApplication.run(EcopointsP2Application.class, args);
	}

}

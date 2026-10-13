package com.example.tlu_gpms_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class TluGpmsSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(TluGpmsSystemApplication.class, args);
	}
}

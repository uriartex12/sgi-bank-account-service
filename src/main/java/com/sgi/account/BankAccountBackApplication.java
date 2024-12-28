package com.sgi.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main class of the Bank Account application.
 * Starts the Spring Boot application.
 */
@SpringBootApplication
public class BankAccountBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankAccountBackApplication.class, args);
	}

}

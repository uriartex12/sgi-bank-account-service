package com.sgi.bank_account_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableFeignClients
public class BankAccountBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankAccountBackApplication.class, args);
	}

}

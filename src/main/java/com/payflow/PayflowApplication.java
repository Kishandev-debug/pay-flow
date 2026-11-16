package com.payflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayflowApplication {

	public static void main(String[] args) {

		System.out.println(
				"JWT_SECRET system property exists: "
						+ (System.getProperty("JWT_SECRET") != null)
		);

		SpringApplication.run(PayflowApplication.class, args);
	}
}
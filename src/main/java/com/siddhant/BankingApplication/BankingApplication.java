package com.siddhant.BankingApplication;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "Banking Application API",
				version = "1.0",
				description = "API for Banking Application",
				contact = @Contact(
						name = "Siddhant Sinha",
						email = "sidsinha491@gmail.com"
				),
				license = @License(
						name = "Banking Application License"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = "Banking Application Documentation"
		)
)
public class BankingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingApplication.class, args);
	}

}

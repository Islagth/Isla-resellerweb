package com.example.enel_bitrix24_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class EnelBitrix24IntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnelBitrix24IntegrationApplication.class, args);
	}

}

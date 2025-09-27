package com.example.enel_bitrix24_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EnelBitrix24IntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnelBitrix24IntegrationApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}

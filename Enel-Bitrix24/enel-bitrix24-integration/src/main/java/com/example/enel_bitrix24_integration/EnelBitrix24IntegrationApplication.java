package com.example.enel_bitrix24_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EnelBitrix24IntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnelBitrix24IntegrationApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());

		RestTemplate restTemplate = new RestTemplate(factory);

		// ✅ Registra i converter predefiniti, incluso quello JSON (Jackson)
		restTemplate.setMessageConverters(List.of(
				new MappingJackson2HttpMessageConverter(), // per JSON
				new StringHttpMessageConverter(),           // per testo
				new FormHttpMessageConverter()              // opzionale
		));

		return restTemplate;
	}

}


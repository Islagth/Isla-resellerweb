package com.example.enel_bitrix24_integration.client;

import com.example.enel_bitrix24_integration.security.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EnelClient {

    private final WebClient webClient;
    private final TokenService tokenService;

    public EnelClient(TokenService tokenService) {
        this.tokenService = tokenService;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.enel.com/v1/")
                .build();
    }

    public String getResource(String path) {
        String token = tokenService.getAccessToken();

        return webClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}


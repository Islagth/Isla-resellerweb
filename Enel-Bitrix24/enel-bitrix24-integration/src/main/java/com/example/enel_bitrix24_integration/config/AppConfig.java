package com.example.enel_bitrix24_integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TokenRecord tokenRecord() {
        String accessToken = "il-tuo-access-token";
        String refreshToken = "il-tuo-refresh-token";
        long expiryTime = System.currentTimeMillis() + 3600_000; // ad esempio 1h di validità

        return new TokenRecord(accessToken, refreshToken, expiryTime);
    }
}
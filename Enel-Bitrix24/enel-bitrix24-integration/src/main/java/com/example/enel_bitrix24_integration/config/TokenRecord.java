package com.example.enel_bitrix24_integration.config;

import lombok.Data;

import org.springframework.stereotype.Component;

@Component
public class TokenRecord {

    private final String accessToken;
    private final String refreshToken;
    private final long expiryTime;


    public TokenRecord(String accessToken, String refreshToken, long expiryTime) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiryTime = expiryTime;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiryTime() {
        return expiryTime;
    }
}
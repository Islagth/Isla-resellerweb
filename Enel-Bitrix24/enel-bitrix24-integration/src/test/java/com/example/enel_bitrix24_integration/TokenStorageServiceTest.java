package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.config.TokenRecord;
import com.example.enel_bitrix24_integration.config.TokenResponse;
import com.example.enel_bitrix24_integration.security.TokenStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

public class TokenStorageServiceTest {

    private TokenStorageService tokenStorageService;

    @BeforeEach
    void setup() {
        tokenStorageService = new TokenStorageService();
    }

    @Test
    void testSaveAndFindTokens() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccess_token("access123");
        tokenResponse.setRefresh_token("refresh123");
        tokenResponse.setExpires_in(3600);

        tokenStorageService.saveTokens(tokenResponse);

        TokenRecord record = tokenStorageService.findByAccessToken("access123");
        assertNotNull(record);
        assertEquals("access123", record.getAccessToken());
        assertEquals("refresh123", record.getRefreshToken());

        // La scadenza dovrebbe essere almeno un'ora da adesso (3600 sec)
        assertTrue(record.getExpiryTime() > System.currentTimeMillis());
    }

    @Test
    void testOverwriteToken() throws InterruptedException {
        TokenResponse tokenResponse1 = new TokenResponse();
        tokenResponse1.setAccess_token("access123");
        tokenResponse1.setRefresh_token("refreshOld");
        tokenResponse1.setExpires_in(10);

        tokenStorageService.saveTokens(tokenResponse1);
        TokenRecord record1 = tokenStorageService.findByAccessToken("access123");
        assertNotNull(record1);
        assertEquals("refreshOld", record1.getRefreshToken());

        // Salva di nuovo con nuovo refresh token e expiry diversa
        TokenResponse tokenResponse2 = new TokenResponse();
        tokenResponse2.setAccess_token("access123");
        tokenResponse2.setRefresh_token("refreshNew");
        tokenResponse2.setExpires_in(20);

        tokenStorageService.saveTokens(tokenResponse2);
        TokenRecord record2 = tokenStorageService.findByAccessToken("access123");
        assertNotNull(record2);
        assertEquals("refreshNew", record2.getRefreshToken());
        assertTrue(record2.getExpiryTime() > record1.getExpiryTime());
    }

    @Test
    void testFindByAccessTokenNotFound() {
        TokenRecord record = tokenStorageService.findByAccessToken("nonexistent");
        assertNull(record);
    }
}

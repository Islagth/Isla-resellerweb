package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.config.TokenResponse;
import com.example.enel_bitrix24_integration.security.OAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OAuthService oauthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTokensSuccess() {
        TokenResponse expected = new TokenResponse();
        expected.setAccess_token("access123");
        expected.setRefresh_token("refresh123");
        expected.setExpires_in(3600);

        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(expected, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(TokenResponse.class))).thenReturn(responseEntity);

        TokenResponse result = oauthService.getTokens("codeX", "clientIdX", "clientSecretX", "http://redirect.uri");

        assertNotNull(result);
        assertEquals("access123", result.getAccess_token());
        assertEquals("refresh123", result.getRefresh_token());
        assertEquals(3600, result.getExpires_in());
    }

    @Test
    void testGetTokensFailureThrows() {
        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.getForEntity(anyString(), eq(TokenResponse.class))).thenReturn(responseEntity);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            oauthService.getTokens("codeX", "clientIdX", "clientSecretX", "http://redirect.uri");
        });

        assertTrue(ex.getMessage().contains("Errore OAuth getTokens"));
    }

    @Test
    void testRefreshTokensSuccess() {
        TokenResponse expected = new TokenResponse();
        expected.setAccess_token("newAccess123");
        expected.setRefresh_token("newRefresh123");
        expected.setExpires_in(3600);

        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(expected, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(TokenResponse.class))).thenReturn(responseEntity);

        TokenResponse result = oauthService.refreshTokens("refreshOld", "clientIdX", "clientSecretX");

        assertNotNull(result);
        assertEquals("newAccess123", result.getAccess_token());
        assertEquals("newRefresh123", result.getRefresh_token());
        assertEquals(3600, result.getExpires_in());
    }

    @Test
    void testRefreshTokensFailureThrows() {
        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        when(restTemplate.getForEntity(anyString(), eq(TokenResponse.class))).thenReturn(responseEntity);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            oauthService.refreshTokens("refreshOld", "clientIdX", "clientSecretX");
        });

        assertTrue(ex.getMessage().contains("Errore OAuth refreshTokens"));
    }
}
package com.example.enel_bitrix24_integration.security;


import com.example.enel_bitrix24_integration.config.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OAuthService {

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(OAuthService.class);

    public OAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TokenResponse getTokens(String code, String clientId, String clientSecret, String redirectUri) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://oauth.bitrix.info/oauth/token/")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUri);

        logger.info("Richiesta getTokens con code: {}", code);
        ResponseEntity<TokenResponse> response = restTemplate.getForEntity(builder.toUriString(), TokenResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            logger.info("Token ottenuto con successo per code: {}", code);
            return response.getBody();
        } else {
            logger.error("Errore OAuth getTokens con code {}: {}", code, response.getStatusCode());
            throw new RuntimeException("Errore OAuth getTokens: " + response.getStatusCode());
        }
    }

    public TokenResponse refreshTokens(String refreshToken, String clientId, String clientSecret) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://oauth.bitrix.info/oauth/token/")
                .queryParam("grant_type", "refresh_token")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("refresh_token", refreshToken);

        logger.info("Richiesta refreshTokens con refreshToken.");
        ResponseEntity<TokenResponse> response = restTemplate.getForEntity(builder.toUriString(), TokenResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            logger.info("Refresh token eseguito con successo.");
            return response.getBody();
        } else {
            logger.error("Errore OAuth refreshTokens: {}", response.getStatusCode());
            throw new RuntimeException("Errore OAuth refreshTokens: " + response.getStatusCode());
        }
    }


    public void getRestTemplate() {
    }
}

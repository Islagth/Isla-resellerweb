package com.example.enel_bitrix24_integration.security;

import com.example.enel_bitrix24_integration.config.TokenResponse;
import com.example.enel_bitrix24_integration.config.TokenRecord;
import com.example.enel_bitrix24_integration.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenStorageService {

    // Mappa semplice per demo, chiave: access_token, valore: token record
    private final Map<String, TokenRecord> tokenStore = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(TokenStorageService.class);

    public void saveTokens(TokenResponse tokens) {
        long expiryTime = System.currentTimeMillis() + (tokens.getExpires_in() * 1000L);
        TokenRecord record = new TokenRecord(
                tokens.getAccess_token(),
                tokens.getRefresh_token(),
                expiryTime
        );

        logger.info("Salvataggio token con accessToken: {}, scadenza fra {} ms", record.getAccessToken(), tokens.getExpires_in() * 1000L);

        // Rimuove vecchio token se presente (evita accumulo)
        tokenStore.values().removeIf(t -> t.getAccessToken().equals(record.getAccessToken()));

        tokenStore.put(record.getAccessToken(), record);
        logger.debug("Token salvato nel tokenStore. Numero token attivi: {}", tokenStore.size());
    }

    public TokenRecord findByAccessToken(String accessToken) {
        logger.debug("Ricerca token con accessToken: {}", accessToken);
        return tokenStore.get(accessToken);
    }


}

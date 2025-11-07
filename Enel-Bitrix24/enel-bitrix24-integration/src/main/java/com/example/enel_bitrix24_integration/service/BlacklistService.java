package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LottoBlacklistDTO;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

@Service
public class BlacklistService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    @Value("${enel.api.base-url}")
    private String baseUrl; // preso da application.yml

    @Value("${webhook.api-key}")
    private String apiKey;

    @Getter
    private List<LottoBlacklistDTO> ultimiLottiBlacklist = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(BlacklistService.class);

    public BlacklistService(RestTemplate restTemplate, ObjectMapper objectMapper, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
    }

    private HttpHeaders getBearerAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());
        return headers;
    }

    private HttpHeaders getApiKeyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-auth-token", apiKey);
        return headers;
    }

        @Scheduled(fixedRate = 300000) // ogni 5 minuti
        public List<LottoBlacklistDTO> verificaBlacklistDisponibili() {
            try {
                String url = baseUrl + "/partner-api/v5/blacklist";
                logger.info("🔍 Avvio verifica lotti blacklist chiamando: {}", url);
        
                HttpEntity<String> entity = new HttpEntity<>(getBearerAuthHeaders());
        
                // ✅ RestTemplate deserializza direttamente l’array JSON in List<LottoBlacklistDTO>
                ResponseEntity<List<LottoBlacklistDTO>> response = restTemplate.exchange(
                        new URI(url),
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<LottoBlacklistDTO>>() {}
                );
        
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ultimiLottiBlacklist = response.getBody();
                    logger.info("✅ Lotti blacklist aggiornati: {}", ultimiLottiBlacklist.size());
                } else {
                    logger.error("❌ Errore nella chiamata API esterna: {}", response.getStatusCode());
                }
        
            } catch (Exception e) {
                logger.error("❌ Errore durante la verifica lotti di blacklist: {}", e.getMessage(), e);
            }
        
            return ultimiLottiBlacklist != null ? ultimiLottiBlacklist : Collections.emptyList();
        }

    public byte[] scaricaLottoBlacklistZip(long idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/blacklist/" + idLotto + ".zip";
        HttpEntity<String> entity = new HttpEntity<>(getApiKeyHeaders());

        ResponseEntity<byte[]> response = restTemplate.exchange(new URI(url), HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            byte[] zipDataBlacklist = response.getBody();
            if (zipDataBlacklist != null && zipDataBlacklist.length > 0) {
                logger.info("Scaricamento ZIP lotto blacklist {} riuscito, dimensione: {} bytes", idLotto, zipDataBlacklist.length);
                return zipDataBlacklist;
            } else {
                logger.error("Errore scaricamento ZIP per lotto blacklist {}: contenuto vuoto", idLotto);
                throw new RuntimeException("Errore scaricamento ZIP per lotto blacklist " + idLotto);
            }
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            logger.error("Slice Id not found per lotto blacklist {}", idLotto);
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            logger.error("Slice Id not available per lotto blacklist {}", idLotto);
            throw new RuntimeException("Slice Id not available");
        } else {
            logger.error("Errore inatteso scaricamento lotto blacklist {}: {}", idLotto, response.getStatusCode());
            throw new RuntimeException("Errore inatteso: " + response.getStatusCode());
        }
    }

    public void confermaLotto(long idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/blacklist/" + idLotto;
        HttpEntity<String> entity = new HttpEntity<>(getApiKeyHeaders());

        ResponseEntity<String> response = restTemplate.postForEntity(new URI(url), entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            boolean success = root.path("success").asBoolean(false);
            if (success) {
                logger.info("Lotto {} scaricato correttamente", idLotto);
            } else {
                String message = root.path("message").asText("Errore sconosciuto");
                logger.error("Errore conferma lotto {}: {}", idLotto, message);
                throw new RuntimeException("Errore conferma lotto " + idLotto + ": " + message);
            }
        } else {
            logger.error("Errore nella risposta conferma lotto {}: status {}", idLotto, response.getStatusCode());
            throw new RuntimeException("Errore conferma lotto, status: " + response.getStatusCode());
        }
    }
}


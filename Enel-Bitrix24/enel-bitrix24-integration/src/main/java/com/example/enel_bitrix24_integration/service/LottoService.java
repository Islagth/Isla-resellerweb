package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LottoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    @Value("${enel.api.base-url}")
    private String baseUrl;

    @Value("${webhook.api-key}")
    private String apiKey;

    @Getter
    private List<LottoDTO> ultimiLotti = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(LottoService.class);

    public LottoService(RestTemplate restTemplate, ObjectMapper objectMapper, TokenService tokenService) {
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

     @Scheduled(fixedRate = 60000)
    public List<LottoDTO> verificaLottiDisponibili() {
        try {
            String url = baseUrl + "/partner-api/v5/slices";
            logger.info("Avvio verifica lotti disponibili chiamando: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(getBearerAuthHeaders());

            ResponseEntity<String> response = restTemplate.exchange(new URI(url), HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ultimiLotti = Arrays.asList(objectMapper.readValue(response.getBody(), LottoDTO[].class));
                logger.info("Lotti aggiornati: {}", ultimiLotti.size());
            } else {
                logger.error("Errore nella chiamata API esterna: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Errore durante aggiornamento lotti: {}", e.getMessage(), e);
        }
        return ultimiLotti;
    }

    // Scarica JSON di un lotto specifico
    public String scaricaLottoJson(String idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".json";
        logger.info("Scaricamento JSON per lotto id: {}", idLotto);

        HttpEntity<String> entity = new HttpEntity<>(getApiKeyHeaders());
        ResponseEntity<String> response = restTemplate.exchange(new URI(url), HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("✅ Scaricamento completato per lotto id: {}", idLotto);
            return response.getBody();
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            logger.warn("⚠️ Slice Id non trovato per lotto id: {}", idLotto);
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            logger.warn("🚫 Slice Id non disponibile per lotto id: {}", idLotto);
            throw new RuntimeException("Slice Id not available");
        } else {
            logger.error("Errore scaricamento JSON per lotto {}: status {}", idLotto, response.getStatusCode());
            throw new RuntimeException("Errore scaricamento JSON: " + response.getStatusCode());
        }
    }

    public byte[] scaricaLottoZip(String idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".zip";
        logger.info("Scaricamento ZIP per lotto id: {}", idLotto);

        HttpEntity<String> entity = new HttpEntity<>(getApiKeyHeaders()); // usa API-Key

        ResponseEntity<byte[]> response = restTemplate.exchange(new URI(url), HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            byte[] zipData = response.getBody();
            if (zipData != null && zipData.length > 0) {
                logger.info("Scaricamento ZIP completato per lotto id: {}, dimensione bytes: {}", idLotto, zipData.length);
                return zipData;
            } else {
                logger.error("Errore scaricamento ZIP per lotto {}: contenuto vuoto", idLotto);
                throw new RuntimeException("Errore scaricamento ZIP per lotto " + idLotto);
            }
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            logger.error("Slice Id non trovato per lotto id: {}", idLotto);
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            logger.error("Slice Id non disponibile per lotto id: {}", idLotto);
            throw new RuntimeException("Slice Id not available");
        } else {
            logger.error("Errore inatteso durante scaricamento ZIP lotto {}: status {}", idLotto, response.getStatusCode());
            throw new RuntimeException("Errore inatteso: " + response.getStatusCode());
        }
    }
}



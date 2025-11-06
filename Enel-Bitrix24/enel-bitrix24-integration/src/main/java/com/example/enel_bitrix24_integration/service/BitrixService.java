package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class BitrixService {

    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(BitrixService.class);

    @Value("${enel.api.base-url}")
    private String baseUrl; // preso da application.yml

    @Value("${webhook.api-key}")
    private String apiKey;

    private final TokenService tokenService;

    public BitrixService(TokenService tokenService) {
        this.restTemplate = new RestTemplate();
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

    /**
     * 📤 Invio di un contatto “lavorato” a Bitrix24 con retry
     */
    public LeadResponse invioLavorato(LeadRequest request) {
        String url = baseUrl + "/partner-api/v5/worked";

        HttpHeaders headers = getBearerAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // ✅ aggiungi questo
        headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // ✅ opzionale ma consigliato

        // Log header e JSON inviato
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule()); // gestisce LocalDateTime
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            logger.info("📤 Invio a Enel [{}]", url);
            logger.info("📦 Body JSON inviato:\n{}", json);
        } catch (JsonProcessingException e) {
            logger.error("❌ Errore serializzazione JSON del request: {}", e.getMessage());
        }

        HttpEntity<LeadRequest> entity = new HttpEntity<>(request, headers);

        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                logger.info("📨 Invio contatto lavorato (tentativo {}): {}", attempt, request);
                ResponseEntity<LeadResponse> response = restTemplate.postForEntity(url, entity, LeadResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("✅ Invio contatto riuscito al tentativo {}", attempt);
                    return response.getBody();
                } else {
                    logger.warn("⚠️ Risposta non valida da Enel ({}): {}",
                            response.getStatusCode(), response.getBody());
                }
            } catch (Exception e) {
                logger.error("❌ Errore al tentativo {}: {}", attempt, e.getMessage());
                if (attempt == maxRetry) {
                    LeadResponse error = new LeadResponse();
                    error.setSuccess(false);
                    error.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi: " + e.getMessage());
                    return error;
                }
                try {
                    Thread.sleep(1000L * attempt); // Backoff progressivo
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        LeadResponse fallback = new LeadResponse();
        fallback.setSuccess(false);
        fallback.setMessage("Errore imprevisto dopo tentativi multipli");
        return fallback;
    }

}

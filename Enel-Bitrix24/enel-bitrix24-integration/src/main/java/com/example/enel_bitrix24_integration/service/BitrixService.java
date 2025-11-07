package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class BitrixService {

    @Autowired
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

        // ✅ Header corretti
        HttpHeaders headers = getBearerAuthHeaders();
        headers.setContentType(MediaType.valueOf("application/json;charset=UTF-8"));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // ✅ Configura ObjectMapper per LocalDateTime e formati corretti
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(request);
            logger.info("📤 Invio a Enel [{}]", url);
            logger.info("📦 Body JSON inviato:\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
        } catch (JsonProcessingException e) {
            logger.error("❌ Errore serializzazione JSON del request: {}", e.getMessage());
            LeadResponse error = new LeadResponse();
            error.setSuccess(false);
            error.setMessage("Errore serializzazione JSON: " + e.getMessage());
            return error;
        }

        // ✅ Corpo come stringa per evitare problemi di conversione automatica
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // ✅ Assicuriamoci che RestTemplate gestisca JSON
        if (restTemplate.getMessageConverters().stream()
                .noneMatch(c -> c instanceof MappingJackson2HttpMessageConverter)) {
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        }

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

            } catch (HttpClientErrorException e) {
                logger.error("❌ Errore HTTP {} al tentativo {}: {}", e.getStatusCode(), attempt, e.getMessage());
                if (e.getStatusCode() == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
                    logger.error("❗ Verifica Content-Type e formato JSON");
                }
                if (attempt == maxRetry) {
                    LeadResponse error = new LeadResponse();
                    error.setSuccess(false);
                    error.setMessage("Errore HTTP dopo " + maxRetry + " tentativi: " + e.getMessage());
                    return error;
                }

            } catch (Exception e) {
                logger.error("❌ Errore generico al tentativo {}: {}", attempt, e.getMessage());
                if (attempt == maxRetry) {
                    LeadResponse error = new LeadResponse();
                    error.setSuccess(false);
                    error.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi: " + e.getMessage());
                    return error;
                }

            }

            // ⏳ Backoff progressivo
            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        LeadResponse fallback = new LeadResponse();
        fallback.setSuccess(false);
        fallback.setMessage("Errore imprevisto dopo tentativi multipli");
        return fallback;
    }


}

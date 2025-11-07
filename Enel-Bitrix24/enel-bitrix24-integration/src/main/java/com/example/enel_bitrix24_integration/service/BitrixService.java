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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        int maxRetry = 3;

        // 📦 Prepara headers base
        HttpHeaders headers = getBearerAuthHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // 🧾 Log JSON prima dell'invio
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            logger.info("📤 Invio a Enel [{}]", url);
            logger.info("📦 Body JSON inviato:\n{}", json);
        } catch (JsonProcessingException e) {
            logger.error("❌ Errore serializzazione JSON del request: {}", e.getMessage());
        }

        // ✅ Entità JSON
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LeadRequest> jsonEntity = new HttpEntity<>(request, headers);

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                logger.info("📨 Invio contatto lavorato (JSON) tentativo {}: {}", attempt, request);
                ResponseEntity<LeadResponse> response = restTemplate.postForEntity(url, jsonEntity, LeadResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("✅ Invio contatto riuscito in formato JSON al tentativo {}", attempt);
                    return response.getBody();
                } else {
                    logger.warn("⚠️ Risposta non valida da Enel ({}): {}", response.getStatusCode(), response.getBody());
                }
            } catch (HttpClientErrorException e) {
                logger.error("❌ Errore HTTP {} al tentativo {}: {}", e.getStatusCode(), attempt, e.getMessage());

                // 🔁 Se l'errore è 415 → fallback su form-data
                if (e.getStatusCode() == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
                    logger.warn("🔄 Errore 415: passo automaticamente a invio form-data...");
                    return invioLavoratoForm(request); // ⬅️ fallback automatico
                }
            } catch (Exception e) {
                logger.error("❌ Errore generico al tentativo {}: {}", attempt, e.getMessage());
            }

            // ⏱️ Backoff progressivo
            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        LeadResponse fallback = new LeadResponse();
        fallback.setSuccess(false);
        fallback.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi (JSON)");
        return fallback;
    }


    private LeadResponse invioLavoratoForm(LeadRequest request) {
        String url = baseUrl + "/partner-api/v5/worked";

        HttpHeaders headers = getBearerAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        boolean usaDescrizioneResultCode = false; // <-- metti a false se Enel vuole "D109"

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("workedCode", request.getWorkedCode());
        form.add("worked_Date", request.getWorked_Date() != null ? request.getWorked_Date().toString() : "");
        form.add("worked_End_Date", request.getWorked_End_Date() != null ? request.getWorked_End_Date().toString() : "");
        form.add("caller", request.getCaller());
        form.add("workedType", request.getWorkedType());
        form.add("campaignId", request.getCampaignId() != null ? request.getCampaignId().toString() : "");
        form.add("contactId", request.getContactId() != null ? request.getContactId().toString() : "");
        form.add("chatHistory", request.getChatHistory());

        if (request.getResultCode() != null) {
            String resultValue = usaDescrizioneResultCode
                    ? request.getResultCode().getEsito()
                    : request.getResultCode().name();
            form.add("resultCode", resultValue);
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                logger.info("📨 Invio contatto lavorato (form) tentativo {}: {}", attempt, request);
                logger.info("📦 Form inviato:\n{}", form);

                ResponseEntity<LeadResponse> response = restTemplate.postForEntity(url, entity, LeadResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("✅ Invio contatto riuscito in formato form-data al tentativo {}", attempt);
                    return response.getBody();
                } else {
                    logger.warn("⚠️ Risposta non valida da Enel ({}): {}", response.getStatusCode(), response.getBody());
                }
            } catch (Exception e) {
                logger.error("❌ Errore al tentativo {} (form-data): {}", attempt, e.getMessage());
            }

            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        LeadResponse fallback = new LeadResponse();
        fallback.setSuccess(false);
        fallback.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi (form-data)");
        return fallback;
    }


}

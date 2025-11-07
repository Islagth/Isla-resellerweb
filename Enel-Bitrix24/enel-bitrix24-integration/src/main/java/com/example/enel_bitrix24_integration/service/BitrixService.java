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

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        logger.info("📤 Invio a Enel [{}]", url);

        // ✅ Configurazione headers
        HttpHeaders headers = getBearerAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // ✅ Configurazione del mapper (formato date corretto)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ✅ Log body JSON in formato leggibile
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            logger.info("📦 Body JSON inviato:\n{}", json);
        } catch (JsonProcessingException e) {
            logger.error("❌ Errore serializzazione JSON del request: {}", e.getMessage());
        }

        // ✅ Configurazione RestTemplate con lo stesso mapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, converter);

        HttpEntity<LeadRequest> entity = new HttpEntity<>(request, headers);
        int maxRetry = 3;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                logger.info("📨 Invio contatto lavorato (JSON) tentativo {}: {}", attempt, request);
                ResponseEntity<LeadResponse> response = restTemplate.postForEntity(url, entity, LeadResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("✅ Invio contatto riuscito al tentativo {}", attempt);
                    logger.info("📬 Risposta Enel:\n{}", mapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(response.getBody()));
                    return response.getBody();
                } else {
                    logger.warn("⚠️ Risposta non valida da Enel ({}): {}", response.getStatusCode(), response.getBody());
                }

            } catch (HttpClientErrorException.UnsupportedMediaType e) {
                logger.error("❌ Errore HTTP 415 UNSUPPORTED_MEDIA_TYPE al tentativo {}: {}", attempt, e.getMessage());
                logger.warn("🔄 Errore 415: passo automaticamente a invio form-data...");
                return invioLavoratoForm(request); // fallback

            } catch (Exception e) {
                logger.error("❌ Errore al tentativo {}: {}", attempt, e.getMessage());
                if (attempt == maxRetry) {
                    LeadResponse error = new LeadResponse();
                    error.setSuccess(false);
                    error.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi: " + e.getMessage());
                    return error;
                }

                try {
                    Thread.sleep(1000L * attempt); // backoff progressivo
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



    private LeadResponse invioLavoratoForm(LeadRequest request) {
        String url = baseUrl + "/partner-api/v5/worked";
        logger.info("📤 Invio a Enel [{}] in formato form-data", url);

        HttpHeaders headers = getBearerAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // ✅ Preparazione form-data
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("workedCode", request.getWorkedCode());
        form.add("worked_Date", request.getWorked_Date() != null
                ? request.getWorked_Date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : "");
        form.add("worked_End_Date", request.getWorked_End_Date() != null
                ? request.getWorked_End_Date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : "");
        form.add("resultCode", request.getResultCode() != null ? request.getResultCode().name() : "");
        form.add("caller", request.getCaller());
        form.add("workedType", request.getWorkedType());
        form.add("campaignId", request.getCampaignId() != null ? request.getCampaignId().toString() : "");
        form.add("contactId", request.getContactId() != null ? request.getContactId().toString() : "");
        

        // ✅ Log leggibile del form inviato
        logger.info("📦 Form inviato:\n{}",
                form.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("\n"))
        );

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        RestTemplate restTemplate = new RestTemplate();
        int maxRetry = 3;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                logger.info("📨 Invio contatto lavorato (form) tentativo {}: {}", attempt, request);

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("✅ Invio contatto riuscito (form) al tentativo {}", attempt);
                    logger.info("📬 Risposta Enel (form):\n{}", response.getBody());

                    LeadResponse success = new LeadResponse();
                    success.setSuccess(true);
                    success.setMessage(response.getBody());
                    return success;
                } else {
                    logger.warn("⚠️ Risposta non valida da Enel (form) ({}): {}", response.getStatusCode(), response.getBody());
                }

            } catch (Exception e) {
                logger.error("❌ Errore al tentativo {} (form-data): {}", attempt, e.getMessage());
                if (attempt == maxRetry) {
                    LeadResponse error = new LeadResponse();
                    error.setSuccess(false);
                    error.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi (form-data): " + e.getMessage());
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
        fallback.setMessage("Errore imprevisto dopo tentativi multipli (form-data)");
        return fallback;
    }



}

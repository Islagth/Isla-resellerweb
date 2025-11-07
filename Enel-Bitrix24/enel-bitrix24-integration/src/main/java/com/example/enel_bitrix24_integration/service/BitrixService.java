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

    // ✅ Header con autenticazione + Content-Type esplicito
    HttpHeaders headers = getBearerAuthHeaders();
    headers.set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    // ✅ Serializzazione manuale
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    String jsonBody;
    try {
        jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        logger.info("📦 Body JSON inviato:\n{}", jsonBody);
    } catch (JsonProcessingException e) {
        logger.error("❌ Errore serializzazione JSON: {}", e.getMessage());
        LeadResponse err = new LeadResponse();
        err.setSuccess(false);
        err.setMessage("Errore serializzazione JSON: " + e.getMessage());
        return err;
    }

    HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

    RestTemplate restTemplate = new RestTemplate();
    int maxRetry = 3;

    for (int attempt = 1; attempt <= maxRetry; attempt++) {
        try {
            logger.info("📨 Invio contatto lavorato (JSON) tentativo {}: {}", attempt, request);

            ResponseEntity<LeadResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    LeadResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("✅ Invio contatto riuscito al tentativo {}", attempt);
                logger.info("📬 Risposta Enel:\n{}", 
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody()));
                return response.getBody();
            } else {
                logger.warn("⚠️ Risposta non valida ({}): {}", response.getStatusCode(), response.getBody());
            }

        } catch (HttpClientErrorException.UnsupportedMediaType e) {
            logger.error("❌ Errore 415 UNSUPPORTED_MEDIA_TYPE al tentativo {}: {}", attempt, e.getMessage());
            logger.warn("🔄 Server non accetta JSON — passo a invio form-data...");
            return invioLavoratoForm(request);

        } catch (Exception e) {
            logger.error("❌ Errore al tentativo {}: {}", attempt, e.getMessage());
            if (attempt == maxRetry) {
                LeadResponse err = new LeadResponse();
                err.setSuccess(false);
                err.setMessage("Errore dopo " + maxRetry + " tentativi: " + e.getMessage());
                return err;
            }

            try {
                Thread.sleep(1000L * attempt);
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


    public LeadResponse invioLavoratoForm(LeadRequest request) {
    String url = baseUrl + "/partner-api/v5/worked";
    logger.info("📤 Invio a Enel [{}] in formato form-data", url);

    HttpHeaders headers = getBearerAuthHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("workedCode", request.getWorkedCode());
    form.add("worked_Date", request.getWorked_Date() != null ? request.getWorked_Date().toString() : "");
    form.add("worked_End_Date", request.getWorked_End_Date() != null ? request.getWorked_End_Date().toString() : "");
    form.add("resultCode", request.getResultCode() != null ? request.getResultCode().name() : "");
    form.add("caller", request.getCaller());
    form.add("workedType", request.getWorkedType());
    form.add("campaignId", String.valueOf(request.getCampaignId()));
    form.add("contactId", String.valueOf(request.getContactId()));

    logger.info("📦 Form inviato:");
    form.forEach((k, v) -> logger.info("{}={}", k, v));

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

    RestTemplate restTemplate = new RestTemplate();
    int maxRetry = 3;

    for (int attempt = 1; attempt <= maxRetry; attempt++) {
        try {
            logger.info("📨 Invio contatto lavorato (form) tentativo {}: {}", attempt, request);
            ResponseEntity<LeadResponse> response = restTemplate.postForEntity(url, entity, LeadResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("✅ Invio contatto (form) riuscito al tentativo {}", attempt);
                return response.getBody();
            }

            logger.warn("⚠️ Risposta non valida ({}): {}", response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            logger.error("❌ Errore al tentativo {} (form-data): {}", attempt, e.getMessage());
            if (attempt == maxRetry) {
                LeadResponse err = new LeadResponse();
                err.setSuccess(false);
                err.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi (form-data): " + e.getMessage());
                return err;
            }

            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    LeadResponse fallback = new LeadResponse();
    fallback.setSuccess(false);
    fallback.setMessage("Errore imprevisto dopo tentativi multipli (form)");
    return fallback;
}

}

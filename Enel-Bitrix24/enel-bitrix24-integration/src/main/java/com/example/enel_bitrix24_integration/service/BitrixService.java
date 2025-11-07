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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
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
        String url = baseUrl + "/partner-api/v5/workedcontact";
        logger.info("📤 Invio a Enel [{}]", url);

        HttpHeaders headers = getBearerAuthHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));

        String jsonBody;
        try {
            jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            logger.info("📦 Body JSON inviato:\n{}", jsonBody);
        } catch (JsonProcessingException e) {
            logger.error("❌ Errore serializzazione JSON: {}", e.getMessage(), e);
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
                logger.info("📨 Invio contatto lavorato (JSON) tentativo {}: {}", attempt, request.getWorkedCode());
                ResponseEntity<LeadResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, LeadResponse.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("✅ Invio riuscito al tentativo {}", attempt);
                    logger.info("📬 Risposta Enel:\n{}",
                            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody()));
                    return response.getBody();
                } else {
                    logger.warn("⚠️ Risposta non valida ({}): {}", response.getStatusCode(), response.getBody());
                }

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                logger.error("❌ Errore HTTP {} al tentativo {}: {} - {}",
                        e.getStatusCode(), attempt, e.getStatusText(), e.getResponseBodyAsString(), e);

            } catch (ResourceAccessException e) {
                logger.error("❌ Errore di connessione al tentativo {}: {}", attempt, e.getMessage(), e);

            } catch (Exception e) {
                logger.error("❌ Errore imprevisto al tentativo {}: {}",
                        attempt, e.getMessage() != null ? e.getMessage() : e.toString(), e);
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
        fallback.setMessage("Errore imprevisto dopo " + maxRetry + " tentativi");
        return fallback;
    }



}

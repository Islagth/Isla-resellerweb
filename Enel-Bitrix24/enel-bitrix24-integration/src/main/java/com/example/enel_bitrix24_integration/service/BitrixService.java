package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    public LeadResponse invioLavorato(LeadRequest request) {
        String url = baseUrl + "/partner-api/v5/workedcontact";
        HttpHeaders headers = getBearerAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LeadRequest> entity = new HttpEntity<>(request, headers);

        int maxRetry = 3;
        int retryCount = 0;
        logger.info("Avvio invio lavorato con dati: {}", request);

        while (retryCount < maxRetry) {
            try {
                LeadResponse response = restTemplate.postForObject(url, entity, LeadResponse.class);
                logger.info("Chiamata API lavorato riuscita al tentativo {}", retryCount + 1);
                return response;
            } catch (Exception e) {
                retryCount++;
                logger.error("Errore chiamata API al tentativo {}: {}", retryCount, e.getMessage());
                if (retryCount >= maxRetry) {
                    LeadResponse errorResponse = new LeadResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi: " + e.getMessage());
                    logger.error("Fallito invio lavorato dopo {} tentativi", maxRetry);
                    return errorResponse;
                }
                try {
                    logger.info("Attesa prima del retry n.{}", retryCount);
                    Thread.sleep(1000 * retryCount); // delay progressivo 1s, 2s, 3s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LeadResponse errorResponse = new LeadResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Errore chiamata API, retry interrotto.");
                    logger.error("Retry interrotto da InterruptedException");
                    return errorResponse;
                }
            }
        }
        LeadResponse errorResponse = new LeadResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage("Errore imprevisto nel retry");
        logger.error("Errore imprevisto nel retry invio lavorato");
        return errorResponse;
    }
}

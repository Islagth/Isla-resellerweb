package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BitrixService {

    private final RestTemplate restTemplate;

    @Value("${enel.api.base-url}")
    private String baseUrl; // preso da application.yml

    public BitrixService() {
        this.restTemplate = new RestTemplate();
    }


    public LeadResponse invioLavorato(LeadRequest request) {
        String url = baseUrl + "/partner-api/v5/workedcontact";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LeadRequest> entity = new HttpEntity<>(request, headers);

        int maxRetry = 3;
        int retryCount = 0;
        while (retryCount < maxRetry) {
            try {
                return restTemplate.postForObject(url, entity, LeadResponse.class);
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetry) {
                    LeadResponse errorResponse = new LeadResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Errore chiamata API dopo " + maxRetry + " tentativi: " + e.getMessage());
                    return errorResponse;
                }
                try {
                    Thread.sleep(1000 * retryCount); // delay progressivo 1s, 2s, 3s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LeadResponse errorResponse = new LeadResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Errore chiamata API, retry interrotto.");
                    return errorResponse;
                }
            }
        }
        LeadResponse errorResponse = new LeadResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage("Errore imprevisto nel retry");
        return errorResponse;
    }
}

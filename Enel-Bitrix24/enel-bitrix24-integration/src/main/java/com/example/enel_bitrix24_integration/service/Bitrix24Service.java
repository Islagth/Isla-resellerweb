package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.config.Bitrix24Properties;
import com.example.enel_bitrix24_integration.controller.EnelWebhookController;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.enel_bitrix24_integration.dto.EnelLeadRequest;
import com.example.enel_bitrix24_integration.dto.Bitrix24Response;
import com.example.enel_bitrix24_integration.dto.EsitoTelefonata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Service
public class Bitrix24Service {

    private static final Logger logger = LoggerFactory.getLogger(Bitrix24Service.class);

    private final Bitrix24Properties properties;

    public Bitrix24Service(Bitrix24Properties properties) {
        this.properties = properties;
    }

    public RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis()); // timeout connessione
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());   // timeout lettura
        return new RestTemplate(factory);
    }


    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // 1s, 2s, 4s
    )
    public Bitrix24Response createLead(EnelLeadRequest request) {

        try {

        RestTemplate restTemplate = createRestTemplate();

        // 🔀 Mapping Enel → Bitrix24
        Map<String, Object> leadData = new HashMap<>();
        leadData.put("CAMPAIGN_ID", request.getCampaign_Id());
        leadData.put("TELEFONO_CONTATTO", request.getTelefono_Contatto());
        leadData.put("ID_ANAGRAFICA", request.getId_Anagrafica());
        leadData.put("COD_CONTRATTO", request.getCod_Contratto());
        leadData.put("POD_PDR", request.getPod_Pdr());


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(leadData, headers);

        ResponseEntity<Bitrix24Response> responseEntity = restTemplate.postForEntity(
                properties.getUrl(),
                entity,
                Bitrix24Response.class
        );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                logger.info("✅ Lead creato su Bitrix24: {}", responseEntity.getBody().getResult());
                return responseEntity.getBody();
            } else {
                logger.warn("⚠️ Errore Bitrix24: {}", responseEntity.getStatusCode());
                return new Bitrix24Response();
            }

        } catch (RestClientException ex) {
            logger.error("❌ Connessione a Bitrix24 fallita", ex);
            throw ex; // lascia che Retry gestisca i tentativi

        }
    }

}

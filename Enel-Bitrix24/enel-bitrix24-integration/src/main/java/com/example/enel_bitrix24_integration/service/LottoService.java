package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Value("${enel.api.base-url}")
    private String baseUrl; // preso da application.yml

    // Metodo per ottenere gli ultimi lotti (usato dall'endpoint REST)
    @Getter
    private List<LottoDTO> ultimiLotti = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(LottoService.class);

    public LottoService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Eseguito ogni minuto
    @Scheduled(fixedRate = 60000)
    public List<LottoDTO> verificaLottiDisponibili() {
        try {
            String url = baseUrl + "/partner-api/v5/slices";
            logger.info("Avvio verifica lotti disponibili chiamando: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

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

    public String scaricaLottoJson(String idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".json";
        logger.info("Scaricamento JSON per lotto id: {}", idLotto);
        ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Scaricamento JSON completato per lotto id: {}", idLotto);
            return response.getBody();
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            logger.error("Slice Id non trovato per lotto id: {}", idLotto);
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) { // esempio: lotto non disponibile
            logger.error("Slice Id non disponibile per lotto id: {}", idLotto);
            throw new RuntimeException("Slice Id not available");
        } else {
            logger.error("Errore scaricamento JSON per lotto {}: status {}", idLotto, response.getStatusCode());
            throw new RuntimeException("Errore scaricamento JSON per lotto " + idLotto);
        }
    }

    public byte[] scaricaLottoZip(String idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".zip";
        logger.info("Scaricamento ZIP per lotto id: {}", idLotto);

        ResponseEntity<byte[]> response = restTemplate.exchange(new URI(url), HttpMethod.GET, null, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            byte[] zipData = response.getBody();
            if (zipData != null && zipData.length > 0) {
                logger.info("Scaricamento ZIP completato per lotto id: {}, dimensione bytes: {}", idLotto, zipData.length);
                return zipData; // ritorna il binario al controller
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


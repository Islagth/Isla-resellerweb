package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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

    public LottoService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Eseguito ogni minuto
    @Scheduled(fixedRate = 60000)
    public List<LottoDTO> verificaLottiDisponibili() {
        try {
            String url = baseUrl + "/partner-api/v5/slices";
            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ultimiLotti = Arrays.asList(objectMapper.readValue(response.getBody(), LottoDTO[].class));
                System.out.println("Lotti aggiornati: " + ultimiLotti.size());
            } else {
                System.err.println("Errore nella chiamata API esterna: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Errore durante aggiornamento lotti: " + e.getMessage());
        }
        return ultimiLotti;
    }


    public String scaricaLottoJson(String idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".json";
        ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) { // esempio: lotto non disponibile
            throw new RuntimeException("Slice Id not available");
        } else {
            throw new RuntimeException("Errore scaricamento JSON per lotto " + idLotto);
        }
    }


    public byte[] scaricaLottoZip(String idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".zip";

        ResponseEntity<byte[]> response = restTemplate.exchange(new URI(url), HttpMethod.GET, null, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            byte[] zipData = response.getBody();
            if (zipData != null && zipData.length > 0) {
                return zipData; // ritorna il binario al controller
            } else {
                throw new RuntimeException("Errore scaricamento ZIP per lotto " + idLotto);
            }
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new RuntimeException("Slice Id not available");
        } else {
            throw new RuntimeException("Errore inatteso: " + response.getStatusCode());
        }
    }
}


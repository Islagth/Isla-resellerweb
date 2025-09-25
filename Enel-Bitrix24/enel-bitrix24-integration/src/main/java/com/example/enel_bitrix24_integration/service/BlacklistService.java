package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LottoBlacklistDTO;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BlacklistService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${enel.api.base-url}")
    private String baseUrl; // preso da application.yml

    // Metodo per ottenere gli ultimi lotti (usato dall'endpoint REST)
    @Getter
    private List<LottoBlacklistDTO> ultimiLottiBlacklist = new ArrayList<>();

    public BlacklistService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }


    // Eseguito ogni minuto
    @Scheduled(fixedRate = 60000)
    public List<LottoBlacklistDTO> verificaBlacklistDisponibili() {
        try {
            String url = baseUrl + "/partner-api/v5/blacklist";
            ResponseEntity<String> response =
                    restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ultimiLottiBlacklist = Arrays.asList(objectMapper.readValue(response.getBody(), LottoBlacklistDTO[].class));
                System.out.println("Lotti blacklist aggiornati: " + ultimiLottiBlacklist.size());
            } else {
                System.err.println("Errore nella chiamata API esterna: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Errore durante la verifica lotti di blacklist: " + e.getMessage());
        }
        return ultimiLottiBlacklist;
    }


    public byte[] scaricaLottoBlacklistZip(long idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/blacklist/" + idLotto + ".zip";
        ResponseEntity<byte[]> response = restTemplate.exchange(new URI(url), HttpMethod.GET, null, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            byte[] zipDataBlacklist = response.getBody();
            if (zipDataBlacklist != null && zipDataBlacklist.length > 0) {
                return zipDataBlacklist; // ritorna il binario al controller
            } else {
                throw new RuntimeException("Errore scaricamento ZIP per lotto blacklist " + idLotto);
            }
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new RuntimeException("Slice Id not found");
        } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new RuntimeException("Slice Id not available");
        } else {
            throw new RuntimeException("Errore inatteso: " + response.getStatusCode());
        }
    }


    public void confermaLotto(long idLotto) throws Exception {
        String url = baseUrl + "/partner-api/v5/blacklist/" + idLotto;
        ResponseEntity<String> response = restTemplate.postForEntity(new URI(url), null, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            boolean success = root.path("success").asBoolean(false);
            if (success) {
                System.out.println("Lotto " + idLotto + " scaricato correttamente.");
            } else {
                String message = root.path("message").asText("Errore sconosciuto");
                throw new RuntimeException("Errore conferma lotto " + idLotto + ": " + message);
            }
        }
    }

}

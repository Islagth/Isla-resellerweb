package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.ContactDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Service
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final LottoService lottoService;
    private final ObjectMapper objectMapper;

    public ContactService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl, LottoService lottoService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.lottoService = lottoService;
        this.objectMapper = objectMapper;
    }

    // ----------------- CREAZIONE CONTATTO -----------------
    public String creaContatto(ContactDTO contactDTO, String accessToken) throws Exception {
        logger.info("Avvio creazione contatto: {} {}", contactDTO.getNAME(), contactDTO.getLAST_NAME());
        String url = baseUrl + "/rest/crm.contact.add";
        Map<String, Object> fields = new ObjectMapper().convertValue(contactDTO, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);

        String result = postForResultString(url, payload, accessToken, "creazione contatto");
        logger.info("Creazione contatto completata: {}", result);
        return result;
    }

    // Crea contatti da JSON del lotto
    public void creaContattiDaLotto(String idLotto, String json, String accessToken) throws Exception {
        logger.info("Avvio creazione contatti da lotto id: {}", idLotto);

        List<ContactDTO> contatti = objectMapper.readValue(json, new TypeReference<List<ContactDTO>>() {});
        List<String> errori = new ArrayList<>();
        int successo = 0;

        for (ContactDTO contactDTO : contatti) {
            try {
                creaContatto(contactDTO, accessToken);
                successo++;
            } catch (Exception e) {
                logger.error("Errore creazione contatto: {} {}", contactDTO.getNAME(), contactDTO.getLAST_NAME(), e);
                errori.add(contactDTO.getNAME() + " " + contactDTO.getLAST_NAME() + ": " + e.getMessage());
            }
        }

        logger.info("Creazione contatti terminata: {} riusciti, {} falliti.", successo, errori.size());

        if (!errori.isEmpty()) {
            throw new RuntimeException("Alcuni contatti non sono stati creati: " + String.join("; ", errori));
        }
    }

    // ----------------- AGGIORNAMENTO CONTATTO -----------------
    public String aggiornaContatto(int contactId, Map<String, Object> fields, Map<String, Object> params, String accessToken) throws Exception {
        logger.info("Avvio aggiornamento contatto ID: {}", contactId);
        String url = baseUrl + "/rest/crm.contact.update";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);
        payload.put("FIELDS", fields);
        if (params != null && !params.isEmpty()) {
            payload.put("PARAMS", params);
        }

        String result = postForResultString(url, payload, accessToken, "aggiornamento contatto");
        logger.info("Aggiornamento contatto ID {} completato: {}", contactId, result);
        return result;
    }

    // ----------------- GET CONTATTO PER ID -----------------
    public Map<String, Object> getContattoById(int contactId, String accessToken) throws Exception {
        logger.info("Recupero contatto per ID: {}", contactId);
        String url = baseUrl + "/rest/9/03w7isr7xmjog2c6/crm.contact.get.json";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);

        Map<String, Object> result = postForResultMap(url, payload, accessToken, "recupero contatto");
        logger.info("Recupero contatto ID {} completato", contactId);
        return result;
    }

    // ----------------- LISTA CONTATTO -----------------
    public Map<String, Object> listaContatti(Map<String, Object> filter, Map<String, String> order, List<String> select, Integer start, String accessToken) throws Exception {
        logger.info("Richiesta lista contatti");
        String url = baseUrl + "/rest/9/1varqs6u91afcteh/crm.contact.list.json";

        Map<String, Object> payload = new HashMap<>();
        if (filter != null) {
            payload.put("FILTER", filter);
        }
        if (order != null) {
            payload.put("ORDER", order);
        }
        if (select != null) {
            payload.put("SELECT", select);
        }
        if (start != null) {
            payload.put("START", start);
        }

        Map<String, Object> result = postForResultMap(url, payload, accessToken, "lista contatti");
        logger.info("Lista contatti recuperata, numero elementi: {}", ((List<?>)result.getOrDefault("result", new ArrayList<>())).size());
        return result;
    }

    // ----------------- ELIMINAZIONE CONTATTO -----------------
    public boolean eliminaContatto(int contactId, String accessToken) throws Exception {
        logger.info("Avvio eliminazione contatto ID: {}", contactId);
        String url = baseUrl + "/rest/crm.contact.delete";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);

        Map<String, Object> response = postForResultMap(url, payload, accessToken, "eliminazione contatto");
        Object result = response.get("result");
        logger.info("Eliminazione contatto ID {} risultato: {}", contactId, result);
        if (result instanceof Boolean) {
            return (Boolean) result;
        } else {
            throw new RuntimeException("Risposta inattesa dal server Bitrix24 durante eliminazione contatto");
        }
    }


    // Metodo privato per chiamate POST che restituiscono stringa (messaggi di successo)
    private String postForResultString(String url, Map<String, Object> payload, String accessToken, String actionDescription) throws Exception {
        logger.info("Esecuzione azione: {} con URL: {}", actionDescription, url);
        Map<String, Object> response = postForResultMap(url, payload, accessToken, actionDescription);
        Object result = response.get("result");
        if (result != null) {
            logger.info("{} completata con risultato: {}", actionDescription, result.toString());
            return actionDescription.substring(0, 1).toUpperCase() + actionDescription.substring(1) + " riuscita: " + result.toString();
        } else {
            String msg = "Risultato indefinito durante " + actionDescription;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    // Metodo privato generico per chiamate POST che restituiscono mappa
    private Map<String, Object> postForResultMap(String url, Map<String, Object> payload, String accessToken, String actionDescription) throws Exception {
        logger.info("Invio POST per {} a URL: {}", actionDescription, url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(new URI(url), request, Map.class);
            logger.info("Risposta ricevuta con status: {}", response.getStatusCode());
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Token non valido o scaduto durante {}", actionDescription);
            throw new RuntimeException("Token non valido o scaduto.");
        } catch (HttpClientErrorException.Forbidden e) {
            logger.error("Accesso negato per {}: {}", actionDescription, e.getMessage());
            throw new RuntimeException("Accesso negato per " + actionDescription);
        } catch (HttpClientErrorException.BadRequest e) {
            logger.error("Richiesta non valida per {}: {}", actionDescription, e.getResponseBodyAsString());
            throw new RuntimeException("Richiesta non valida per " + actionDescription + ": " + e.getResponseBodyAsString());
        } catch (HttpClientErrorException e) {
            logger.error("Errore HTTP generico per {}: {}", actionDescription, e.getResponseBodyAsString());
            throw new RuntimeException("Errore HTTP generico per " + actionDescription + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Errore durante {}: {}", actionDescription, e.getMessage(), e);
            throw new RuntimeException("Errore durante " + actionDescription + ": " + e.getMessage(), e);
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("result")) {
                logger.info("Azione {} completata con risultato valido", actionDescription);
                return body;
            } else {
                String msg = "Risposta inattesa dal server Bitrix24 durante " + actionDescription;
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "Errore " + actionDescription + ": HTTP " + response.getStatusCode();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }


}

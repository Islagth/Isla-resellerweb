package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.ContactDTO;
import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.ResultCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String webHookUrl;
    private final LottoService lottoService;
    private final ObjectMapper objectMapper;

    // Cache in memoria dell'ultimo stato noto (in produzione -> Redis o DB)
    private final Map<Long, ContactDTO> cacheContatti = new ConcurrentHashMap<>();

    public ContactService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl, @Value("https://b24-vayzx4.bitrix24.it/rest/9/txk5orlo651kxu97") String webHookUrl, LottoService lottoService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.webHookUrl = webHookUrl;
        this.lottoService = lottoService;
        this.objectMapper = objectMapper;
    }

    // ----------------- CREAZIONE CONTATTO -----------------
    public Map<String, Integer> creaContattiDaLotto(String idLotto, String json) throws Exception {
        logger.info("Avvio creazione contatti da lotto id: {}", idLotto);
        logger.debug("JSON in ingresso per lotto {} (contatti): {}", idLotto, json);

        List<ContactDTO> contatti = objectMapper.readValue(json, new TypeReference<List<ContactDTO>>() {});
        Map<String, Integer> contactMap = new HashMap<>();

        for (ContactDTO dto : contatti) {
            try {
                Integer contactId = addContact(dto, null);
                if (contactId != null) {
                    contactMap.put(String.valueOf(dto.getIdAnagrafica()), contactId);
                    logger.info("✅ Contatto {} creato con ID Bitrix: {}", dto.getIdAnagrafica(), contactId);
                } else {
                    logger.warn("⚠️ Creazione contatto {} non ha restituito un ID valido.", dto.getIdAnagrafica());
                }
            } catch (Exception e) {
                logger.error("❌ Errore creazione contatto '{}': {}", dto.getIdAnagrafica(), e.getMessage(), e);
            }
        }

        logger.info("Creazione contatti terminata. Totale: {}", contactMap.size());
        return contactMap;
    }

    public Integer addContact(ContactDTO dto, Map<String, Object> params) throws Exception {
        logger.info("Avvio creazione contatto: {} {}", dto.getIdAnagrafica(), dto.getTelefono());

        Map<String, Object> fields = new HashMap<>();
        fields.put("NAME", dto.getIdAnagrafica());
        fields.put("PHONE", List.of(Map.of("VALUE", dto.getTelefono(), "VALUE_TYPE", "WORK")));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fields", fields);

        String url = webHookUrl + "/crm.contact.add";
        logger.info("Invio POST per creazione contatto a URL: {}", url);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
        if (response.getBody() != null && response.getBody().get("result") != null) {
            return (Integer) response.getBody().get("result");
        } else {
            throw new RuntimeException("Risposta Bitrix non valida durante la creazione del contatto");
        }
    }


    // ----------------- AGGIORNAMENTO CONTATTO -----------------
    public String aggiornaContatto(int contactId, Map<String, Object> fields, Map<String, Object> params) throws Exception {
        logger.info("Avvio aggiornamento contatto ID: {}", contactId);
        String url =  webHookUrl  + "/crm.contact.update";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);
        payload.put("FIELDS", fields);
        if (params != null && !params.isEmpty()) {
            payload.put("PARAMS", params);
        }

        String result = postForResultString(url, payload, "aggiornamento contatto");
        logger.info("Aggiornamento contatto ID {} completato: {}", contactId, result);
        return result;
    }

    // ----------------- GET CONTATTO PER ID -----------------
    public Map<String, Object> getContattoById(int contactId) throws Exception {
        logger.info("Recupero contatto per ID: {}", contactId);
        String url = baseUrl + "/rest/9/03w7isr7xmjog2c6/crm.contact.get.json";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);

        Map<String, Object> result = postForResultMap(url, payload, "recupero contatto");
        logger.info("Recupero contatto ID {} completato", contactId);
        return result;
    }

    // ----------------- LISTA CONTATTO -----------------
    public Map<String, Object> listaContatti(Map<String, Object> filter, Map<String, String> order, List<String> select, Integer start) throws Exception {
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

        Map<String, Object> result = postForResultMap(url, payload, "lista contatti");
        logger.info("Lista contatti recuperata, numero elementi: {}", ((List<?>)result.getOrDefault("result", new ArrayList<>())).size());
        return result;
    }

    // ----------------- ELIMINAZIONE CONTATTO -----------------
    public boolean eliminaContatto(int contactId, String phone) throws Exception {
        logger.info("Avvio eliminazione contatto ID: {}, telefono: {}", contactId, phone);
        String url =  webHookUrl  + "/crm.contact.delete";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);
        payload.put("PHONE", phone);  // aggiunto numero di telefono

        Map<String, Object> response = postForResultMap(url, payload, "eliminazione contatto");
        Object result = response.get("result");
        logger.info("Eliminazione contatto ID {} risultato: {}", contactId, result);
        if (result instanceof Boolean) {
            return (Boolean) result;
        } else {
            throw new RuntimeException("Risposta inattesa dal server Bitrix24 durante eliminazione contatto");
        }
    }


    // Metodo privato per chiamate POST che restituiscono stringa (messaggi di successo)
    private String postForResultString(String url, Map<String, Object> payload, String actionDescription) throws Exception {
        logger.info("Esecuzione azione: {} con URL: {}", actionDescription, url);
        Map<String, Object> response = postForResultMap(url, payload, actionDescription);
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
    private Map<String, Object> postForResultMap(String url, Map<String, Object> payload, String actionDescription) throws Exception {
        logger.info("Invio POST per {} a URL: {}", actionDescription, url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Rimosso l'header Authorization perché non serve più

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(new URI(url), request, Map.class);
            logger.info("Risposta ricevuta con status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            logger.error("Errore HTTP per {}: {}", actionDescription, e.getResponseBodyAsString());
            throw new RuntimeException("Errore HTTP per " + actionDescription + ": " + e.getResponseBodyAsString());
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

   /**
     * Recupera tutti i custom field dei contatti da Bitrix24
     *
     * @return List di Map con le informazioni dei custom field
     */
    public List<Map<String, Object>> listaCustomFields() {
        try {
            String url = baseUrl + "rest/9/nmvjjijpb9vit7my/crm.contact.userfield.list.json";
            Map<String, Object> body = Map.of(
                    "filter", new HashMap<>(),
                    "order", Map.of("SORT", "ASC", "ID", "ASC")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode result = root.path("result");
                if (result.isArray()) {
                    List<Map<String, Object>> fields = new ArrayList<>();
                    for (JsonNode fieldNode : result) {
                        fields.add(objectMapper.convertValue(fieldNode, Map.class));
                    }
                    return fields;
                }
            }
        } catch (Exception e) {
            logger.error("❌ Errore durante il recupero dei custom field: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    /**
     * Recupera il valore del campo custom 'UF_CRM_RESULT_CODE' per un contatto
     * in modo dinamico usando l'ID del campo ottenuto da crm.contact.userfield.list
     */
    public String getResultCodeForContact(Integer contactId) {
        try {
            // 🔹 Recupera la lista dei custom field dei contatti
            List<Map<String, Object>> userFields = listaCustomFields();
            // listaCustomFields() chiama crm.contact.userfield.list e ritorna List<Map<String,Object>>

            // 🔹 Trova il campo "RESULT_CODE" tra i custom fields
            Optional<Map<String, Object>> campoResultCodeOpt = userFields.stream()
                    .filter(f -> "RESULT_CODE".equals(f.get("FIELD_NAME")))
                    .findFirst();

            if (campoResultCodeOpt.isEmpty()) {
                logger.warn("⚠️ Custom field RESULT_CODE non trovato");
                return null;
            }

            String fieldId = campoResultCodeOpt.get().get("ID").toString();

            // 🔹 Recupera il contatto specifico includendo il campo custom
            Map<String, Object> filter = new HashMap<>();
            filter.put("ID", contactId);

            Map<String, Object> result = listaContatti(
                    filter,
                    null,
                    List.of("ID", "NAME", "DATE_MODIFY", fieldId), // usa l'ID dinamico del campo custom
                    0
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lista = (List<Map<String, Object>>) result.get("result");

            if (lista != null && !lista.isEmpty()) {
                Map<String, Object> contattoMap = lista.get(0);
                Object codeValue = contattoMap.get(fieldId);
                if (codeValue != null) {
                    logger.info("📄 RESULT_CODE per contatto {}: {}", contactId, codeValue);
                    return codeValue.toString();
                } else {
                    logger.info("ℹ️ Nessun RESULT_CODE presente per contatto {}", contactId);
                }
            } else {
                logger.warn("⚠️ Nessun contatto trovato con ID {}", contactId);
            }

        } catch (Exception e) {
            logger.error("❌ Errore durante il recupero di RESULT_CODE per contatto {}: {}", contactId, e.getMessage(), e);
        }

        return null;
    }


        public List<LeadRequest> trovaContattiModificati() {
        List<LeadRequest> modificati = new ArrayList<>();

        try {
            // 🔹 Recupera contatti attivi
            Map<String, Object> filter = Map.of("ACTIVE", "Y");
            Map<String, Object> result = listaContatti(
                    filter, null, List.of("ID", "NAME", "DATE_MODIFY"), 0
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lista = (List<Map<String, Object>>) result.get("result");
            if (lista == null || lista.isEmpty()) {
                logger.info("Nessun contatto attivo trovato.");
                return modificati;
            }

            for (Map<String, Object> contattoMap : lista) {
                Integer id;
                try {
                    id = Integer.valueOf(String.valueOf(contattoMap.get("ID")));
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ ID non valido per contatto: {}", contattoMap.get("ID"));
                    continue;
                }

                String dateModify = (String) contattoMap.get("DATE_MODIFY");
                if (dateModify == null || dateModify.isBlank()) {
                    logger.warn("⚠️ Contatto {} senza data di modifica", id);
                    continue;
                }

                ContactDTO nuovo = new ContactDTO();

                // 🔹 Parsing ISO 8601 → LocalDateTime
                try {
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateModify);
                    LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
                    nuovo.setDATE_MODIFY(localDateTime);
                } catch (DateTimeParseException ex) {
                    try {
                        // 🔁 Fallback per formati tipo "yyyy-MM-dd HH:mm:ss"
                        DateTimeFormatter fallbackFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDateTime localDateTime = LocalDateTime.parse(dateModify, fallbackFormatter);
                        nuovo.setDATE_MODIFY(localDateTime);
                    } catch (Exception innerEx) {
                        logger.warn("❌ Formato data non riconosciuto per contatto {}: {}", id, dateModify);
                        continue;
                    }
                }

                // 🔹 Recupera valore del campo custom UF_CRM_RESULT_CODE
                String resultCodeValue = getResultCodeForContact(id);
                nuovo.setRESULT_CODE(resultCodeValue);

                // 🔹 Recupera eventuale contatto precedente dalla cache
                ContactDTO vecchio = cacheContatti.get(id.longValue());
                boolean modificato = vecchio == null ||
                        !Objects.equals(vecchio.getDATE_MODIFY(), nuovo.getDATE_MODIFY()) ||
                        !Objects.equals(vecchio.getRESULT_CODE(), nuovo.getRESULT_CODE());

                if (modificato) {
                    LeadRequest req = new LeadRequest();
                    req.setContactId(id.longValue());
                    req.setWorkedCode("AUTO_" + id);
                    req.setWorked_Date(LocalDateTime.now());
                    req.setResultCode(ResultCode.fromString(resultCodeValue));
                    req.setCaller("AUTO_SCHEDULER");

                    modificati.add(req);
                    cacheContatti.put(id.longValue(), nuovo);

                    logger.info("🔄 Contatto {} modificato → aggiunto alla lista invii automatici", id);
                }
            }

        } catch (Exception e) {
            logger.error("🔥 Errore durante il recupero o confronto contatti", e);
        }

        logger.info("✅ Totale contatti modificati trovati: {}", modificati.size());
        return modificati;
    }

}



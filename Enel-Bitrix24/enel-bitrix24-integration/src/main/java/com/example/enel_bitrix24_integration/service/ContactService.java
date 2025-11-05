package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.ActivityDTO;
import com.example.enel_bitrix24_integration.dto.ContactDTO;
import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.ResultCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
    private final ActivityService activityService;

    // Cache in memoria dell'ultimo stato noto (in produzione -> Redis o DB)
    private final Map<Long, ContactDTO> cacheContatti = new ConcurrentHashMap<>();
    private LocalDateTime ultimaVerifica = LocalDateTime.now().minusHours(1);

    @Autowired
    public ContactService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl, @Value("https://b24-vayzx4.bitrix24.it/rest/9/txk5orlo651kxu97") String webHookUrl, LottoService lottoService, ObjectMapper objectMapper, ActivityService activityService) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.webHookUrl = webHookUrl;
        this.lottoService = lottoService;
        this.objectMapper = objectMapper;
        this.activityService = activityService;
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
   public ContactDTO getContattoById(int contactId) throws Exception {
        logger.info("Recupero contatto per ID: {}", contactId);
        String url = baseUrl + "rest/9/03w7isr7xmjog2c6/crm.contact.get.json";

        Map<String, Object> payload = new HashMap<>();
        payload.put("ID", contactId);

        Map<String, Object> result = postForResultMap(url, payload, "recupero contatto");

        if (result == null || !result.containsKey("result")) {
            logger.warn("⚠️ Nessun contatto trovato per ID {}", contactId);
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.get("result");
        ContactDTO contact = mapToContactDTO(data);

        logger.info("✅ Recupero contatto ID {} completato", contactId);
        return contact;
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
            String url = baseUrl + "/rest/9/nmvjjijpb9vit7my/crm.contact.userfield.list.json";
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
        List<Map<String, Object>> tuttiContatti = new ArrayList<>();

        try {
            // Filtro: solo contatti attivi modificati dopo ultimaVerifica
            String filtroData = ultimaVerifica.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            Map<String, Object> filter = new HashMap<>();
            filter.put("ACTIVE", "Y");
            filter.put(">DATE_MODIFY", filtroData); // ✅ solo contatti modificati

            List<String> select = List.of("ID", "NAME", "PHONE", "DATE_MODIFY", "UF_CRM_RESULT_CODE");
            int start = 0;

            while (true) {
                Map<String, Object> result = listaContatti(filter, null, select, start);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lista = (List<Map<String, Object>>) result.get("result");

                if (lista == null || lista.isEmpty()) break;
                tuttiContatti.addAll(lista);

                Object next = result.get("next");
                if (next == null || Integer.parseInt(next.toString()) == 0) break;
                start = Integer.parseInt(next.toString());

                // Riduci la velocità per non saturare Bitrix
                Thread.sleep(500);
            }

            for (Map<String, Object> contattoMap : tuttiContatti) {
                Integer id;
                try {
                    id = Integer.valueOf(String.valueOf(contattoMap.get("ID")));
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ ID non valido per contatto: {}", contattoMap.get("ID"));
                    continue;
                }

                String name = (String) contattoMap.get("NAME");
                String dateModify = (String) contattoMap.get("DATE_MODIFY");
                if (dateModify == null || dateModify.isBlank()) continue;

                LocalDateTime dataModifica;
                try {
                    dataModifica = OffsetDateTime.parse(dateModify).toLocalDateTime();
                } catch (Exception ex) {
                    try {
                        DateTimeFormatter fallbackFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        dataModifica = LocalDateTime.parse(dateModify, fallbackFormatter);
                    } catch (Exception innerEx) {
                        logger.warn("❌ Formato data non riconosciuto per contatto {}: {}", id, dateModify);
                        continue;
                    }
                }

                String resultCodeValue = (String) contattoMap.get("UF_CRM_RESULT_CODE");
                if (resultCodeValue == null || resultCodeValue.isBlank()) resultCodeValue = "UNKNOWN";

                ResultCode resultCode = ResultCode.fromString(resultCodeValue);

                ContactDTO nuovo = new ContactDTO();
                nuovo.setNAME(name);
                nuovo.setDATE_MODIFY(dataModifica);
                nuovo.setRESULT_CODE(resultCode);

                ContactDTO vecchio = cacheContatti.get(id.longValue());
                boolean modificato = vecchio == null ||
                        !Objects.equals(vecchio.getDATE_MODIFY(), nuovo.getDATE_MODIFY()) ||
                        !Objects.equals(vecchio.getRESULT_CODE(), nuovo.getRESULT_CODE());

                if (modificato) {
                    ActivityDTO ultimaActivity = activityService.getUltimaActivityPerContatto(id);

                    LeadRequest req = new LeadRequest();
                    req.setContactId(id.longValue());
                    req.setWorkedCode(extractPrimaryPhone(nuovo));
                    req.setResultCode(resultCode);
                    req.setCaller("AUTO_SCHEDULER");

                    if (ultimaActivity != null && ultimaActivity.getStartTime() != null && ultimaActivity.getEndTime() != null) {
                        req.setWorked_Date(ultimaActivity.getStartTime());
                        req.setWorked_End_Date(ultimaActivity.getEndTime());
                    } else {
                        req.setWorked_Date(LocalDateTime.now());
                        req.setWorked_End_Date(LocalDateTime.now().plusMinutes(2));
                    }

                    modificati.add(req);
                    cacheContatti.put(id.longValue(), nuovo);
                    logger.info("🔄 Contatto {} modificato (ResultCode: {})", id, resultCode);
                }
            }

            // ✅ aggiorna ultimaVerifica solo se la chiamata è andata bene
            ultimaVerifica = LocalDateTime.now();

        } catch (Exception e) {
            logger.error("🔥 Errore durante il recupero o confronto contatti", e);
        }

        logger.info("✅ Totale contatti modificati trovati: {}", modificati.size());
        return modificati;
    }



    private Map<String, Object> postForResultMap(String url, Map<String, Object> params) {
        int tentativi = 0;
        int maxTentativi = 5;
        long delay = 2000; // 2 secondi

        while (tentativi < maxTentativi) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, params, Map.class);
                return response.getBody();
            } catch (HttpServerErrorException e) {
                if (e.getMessage().contains("QUERY_LIMIT_EXCEEDED")) {
                    tentativi++;
                    logger.warn("⚠️ Limite Bitrix24 raggiunto. Attendo {} ms prima di ritentare (tentativo {})", delay, tentativi);
                    sleepSafe(delay);
                    delay *= 2; // backoff esponenziale
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Troppi tentativi falliti per " + url);
    }

    private void sleepSafe(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }


    /**
     * Converte una stringa in Long in modo sicuro, restituendo null se non è numerica.
     */
    private Long parseLongSafe(String value) {
        try {
            return (value != null && !value.isBlank()) ? Long.valueOf(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }


   private ContactDTO mapToContactDTO(Map<String, Object> item) {
        ContactDTO dto = new ContactDTO();

        // Campi base
        dto.setNAME((String) item.get("NAME"));
        dto.setLAST_NAME((String) item.get("LAST_NAME"));
        dto.setSECOND_NAME((String) item.get("SECOND_NAME"));
        dto.setCOMMENTS((String) item.get("COMMENTS"));
        dto.setSOURCE_ID((String) item.get("SOURCE_ID"));
        dto.setTYPE_ID((String) item.get("TYPE_ID"));

        // Conversione date (Bitrix restituisce stringhe tipo "2025-11-05T10:12:33+03:00")
        dto.setDATE_CREATE(parseDateTime(item.get("DATE_CREATE")));
        dto.setDATE_MODIFY(parseDateTime(item.get("DATE_MODIFY")));

        // Gestione multifield PHONE / EMAIL
        if (item.containsKey("PHONE")) {
            dto.setPHONE(parseMultiFieldList(item.get("PHONE")));
        }
        if (item.containsKey("EMAIL")) {
            dto.setEMAIL(parseMultiFieldList(item.get("EMAIL")));
        }

        // Estrazione telefono principale per comodità
        if (dto.getPHONE() != null && !dto.getPHONE().isEmpty()) {
            dto.setTelefono(dto.getPHONE().get(0).getVALUE());
        }

        return dto;
    }



    /**
     * Converte una stringa in Long in modo sicuro, restituendo null se non è numerica.
     */
    private Long parseLongSafe(String value) {
        try {
            return (value != null && !value.isBlank()) ? Long.valueOf(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String extractPrimaryPhone(ContactDTO contact) {
        if (contact == null || contact.getPHONE() == null || contact.getPHONE().isEmpty()) {
            return null;
        }
        ContactDTO.MultiField primary = contact.getPHONE().get(0);
        return primary != null ? primary.getVALUE() : null;
    }

    @SuppressWarnings("unchecked")
    private List<ContactDTO.MultiField> parseMultiFieldList(Object obj) {
        if (obj == null) return null;
        try {
            List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
            return list.stream()
                    .map(m -> new ContactDTO.MultiField(
                            (String) m.get("VALUE"),
                            (String) m.get("VALUE_TYPE")
                    ))
                    .toList();
        } catch (ClassCastException e) {
            logger.warn("Formato non previsto per MultiField: {}", obj);
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            String str = value.toString().replace(" ", "T");
            return LocalDateTime.parse(str.substring(0, 19)); // taglia eventuale offset
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }





}



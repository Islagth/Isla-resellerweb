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

    public ContactService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl,@Value("https://b24-vayzx4.bitrix24.it/rest/9/txk5orlo651kxu97") String webHookUrl, LottoService lottoService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.webHookUrl = webHookUrl;
        this.lottoService = lottoService;
        this.objectMapper = objectMapper;
    }

    // ----------------- CREAZIONE CONTATTO -----------------
    public String creaContatto(ContactDTO contactDTO) throws Exception {
        logger.info("Avvio creazione contatto: {} {}", contactDTO.getNAME(), contactDTO.getLAST_NAME());
        String url = webHookUrl + "/crm.contact.add";
        Map<String, Object> fields = objectMapper.convertValue(contactDTO, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);

        String result = postForResultString(url, payload, "creazione contatto");
        logger.info("Creazione contatto completata: {}", result);
        return result;
    }

    // Crea contatti da JSON del lotto
    public List<Integer> creaContattiDaLotto(String idLotto, String json) throws Exception {
        logger.info("Avvio creazione contatti da lotto id: {}", idLotto);

        List<ContactDTO> contatti = objectMapper.readValue(json, new TypeReference<List<ContactDTO>>() {});
        List<String> errori = new ArrayList<>();
        int successo = 0;

        for (ContactDTO contactDTO : contatti) {
            try {
                creaContatto(contactDTO);
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
        return null;
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

    //--------------------CONNETTI CONTATTO CON DEAL----------------------
    public void linkContactToDeal(Integer dealId, Integer contactId) {
        logger.info("Inizio collegamento link contatto ID: {}, dealId: {}", contactId, dealId);
        // 1️⃣ Prepara il payload JSON
        Map<String, Object> fields = new HashMap<>();
        fields.put("CONTACT_ID", contactId);
        fields.put("SORT", 100);
        fields.put("IS_PRIMARY", "Y");

        Map<String, Object> params = new HashMap<>();
        params.put("id", dealId);
        params.put("fields", fields);

        // 2️⃣ Costruisci la richiesta HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

        // 3️⃣ URL completo del metodo Bitrix
        String url = webHookUrl + "rest/9/nqg040m0onmcsp34/crm.deal.contact.add.json";

        try {
            // 4️⃣ Chiamata REST
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            // 5️⃣ Gestione della risposta
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Risposta vuota da Bitrix durante il collegamento contatto-deal");
            }

            if (body.containsKey("error")) {
                String error = (String) body.get("error");
                String errorDescription = (String) body.get("error_description");
                throw new RuntimeException("Errore Bitrix [" + error + "]: " + errorDescription);
            }

            logger.info("✅ Collegato contatto %d al deal %d%n, contactId, dealId");

        } catch (Exception e) {
            throw new RuntimeException("Errore nella chiamata REST per collegare il contatto al deal: " + e.getMessage(), e);
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
     * @return List di Map con le informazioni dei custom field
     */
    public List<Map<String, Object>> listaCustomFields() {
        try {
            String url =  baseUrl + "/rest/9/nmvjjijpb9vit7my/crm.contact.userfield.list.json";

            // Corpo della richiesta (nessun filtro, restituisce tutti i campi)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filter", new HashMap<>()); // nessun filtro
            requestBody.put("order", Map.of("SORT", "ASC", "ID", "ASC")); // ordinamento

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode result = root.path("result");

                if (result.isArray()) {
                    List<Map<String, Object>> fields = new ArrayList<>();
                    for (JsonNode fieldNode : result) {
                        Map<String, Object> fieldMap = objectMapper.convertValue(fieldNode, Map.class);
                        fields.add(fieldMap);
                    }
                    return fields;
                }
            }
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei custom field dei contatti: {}", e.getMessage(), e);
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
            Map<String, Object> userFieldsResponse = (Map<String, Object>) listaCustomFields();
            // listaCustomFields() è un metodo che chiama crm.contact.userfield.list e ritorna List<Map<String,Object>>

            // 🔹 Trova l'ID del campo 'UF_CRM_RESULT_CODE'
            Optional<Map<String, Object>> campoResultCodeOpt = userFieldsResponse.entrySet().stream()
                    .map(e -> (Map<String, Object>) e.getValue())
                    .filter(f -> "RESULT_CODE".equals(f.get("FIELD_NAME")))
                    .findFirst();

            if (campoResultCodeOpt.isEmpty()) {
                logger.warn("Custom field RESULT_CODE non trovato");
                return null;
            }

            String fieldId = campoResultCodeOpt.get().get("ID").toString();

            // 🔹 Recupera il contatto specifico usando listaContatti
            Map<String, Object> filter = new HashMap<>();
            filter.put("ID", contactId);

            Map<String, Object> result = listaContatti(
                    filter,
                    null,
                    List.of("ID", "NAME", "DATE_MODIFY", fieldId), // usa l'ID dinamico
                    0
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lista = (List<Map<String, Object>>) result.get("result");

            if (lista != null && !lista.isEmpty()) {
                Map<String, Object> contattoMap = lista.get(0);
                Object codeValue = contattoMap.get(fieldId);
                if (codeValue != null) {
                    return codeValue.toString();
                }
            }
        } catch (Exception e) {
            logger.warn("Errore durante il recupero di RESULT_CODE per contatto {}: {}", contactId, e.getMessage());
        }

        return null;
    }


   public List<LeadRequest> trovaContattiModificati() {
        List<LeadRequest> modificati = new ArrayList<>();

        try {
            // Recupera contatti attivi tramite il metodo esistente
            Map<String, Object> filter = new HashMap<>();
            filter.put("ACTIVE", "Y");
            Map<String, Object> result = listaContatti(filter, null,
                    List.of("ID", "NAME", "DATE_MODIFY"), 0);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lista = (List<Map<String, Object>>) result.get("result");

            for (Map<String, Object> contattoMap : lista) {
                Integer id = (Integer) contattoMap.get("ID");
                String dateModify = (String) contattoMap.get("DATE_MODIFY");

                ContactDTO nuovo = new ContactDTO();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date parsedDate = sdf.parse(dateModify);
                    nuovo.setDATE_MODIFY(parsedDate);
                } catch (ParseException e) {
                    logger.warn("Formato data non valido per contatto {}: {}", id, dateModify);
                    continue;
                }

                // 🔹 Recupera il valore corrente del campo custom UF_CRM_RESULT_CODE
                String resultCodeValue = getResultCodeForContact(id);
                nuovo.setRESULT_CODE(resultCodeValue);

                // Confronto con la cache
                ContactDTO vecchio = cacheContatti.get(id);
                boolean modificato = false;

                if (vecchio == null) {
                    modificato = true;
                } else {
                    boolean dataDiversa = !Objects.equals(vecchio.getDATE_MODIFY(), nuovo.getDATE_MODIFY());
                    boolean campoDiverso = !Objects.equals(vecchio.getRESULT_CODE(), nuovo.getRESULT_CODE());
                    modificato = dataDiversa || campoDiverso;
                }

                if (modificato) {
                    LeadRequest req = new LeadRequest();
                    req.setContactId(Long.valueOf(id));
                    req.setWorkedCode("CONTACT-" + id);
                    req.setWorked_Date(LocalDateTime.now());
                    req.setResultCode(ResultCode.fromString(resultCodeValue));
                    req.setCaller("AUTO_SCHEDULER");

                    modificati.add(req);
                    cacheContatti.put(Long.valueOf(id), nuovo);
                }
            }
        } catch (Exception e) {
            logger.error("Errore durante il recupero o il confronto dei contatti", e);
        }

        return modificati;
    }
}



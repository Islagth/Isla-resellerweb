package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class DealService {

    public final Map<Long, ActivityDTO> cacheAttivita = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String  webHookUrl;
    private final ObjectMapper objectMapper;
    private final ActivityService activityService;
    private final ContactService contactService;

    private static final Logger logger = LoggerFactory.getLogger(DealService.class);
    private LocalDateTime ultimaVerifica = LocalDateTime.now().minusHours(1);
    private final ConcurrentHashMap<Integer, String> cacheResultCodeDeal = new ConcurrentHashMap<>();

    @Autowired
    public DealService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl, @Value("https://b24-vayzx4.bitrix24.it/rest/9/txk5orlo651kxu97") String webHookUrl, ObjectMapper objectMapper, @Lazy ActivityService activityService, @Lazy ContactService contactService) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.webHookUrl = webHookUrl;
        this.objectMapper = objectMapper;
        this.activityService = activityService;
        this.contactService = contactService;
    }

    // ----------------- CREAZIONE DEAL -----------------

    public Map<String, Integer> creaDealDaLotto(String idLotto, String json) throws Exception {
        logger.info("Avvio creazione deal da lotto id: {}", idLotto);
        logger.debug("JSON in ingresso per lotto {} (deal): {}", idLotto, json);

        List<DealDTO> deals = objectMapper.readValue(json, new TypeReference<List<DealDTO>>() {});
        Map<String, Integer> dealMap = new HashMap<>();

        for (DealDTO dto : deals) {
            try {
                Integer dealId = addDeal(dto, null);
                if (dealId != null) {
                    dealMap.put(String.valueOf(dto.getIdAnagrafica()), dealId);
                    logger.info("✅ Deal creato con ID {} per anagrafica {}", dealId, dto.getIdAnagrafica());
                } else {
                    logger.warn("⚠️ Nessun ID valido restituito per il deal {}", dto.getIdAnagrafica());
                }
            } catch (Exception e) {
                logger.error("❌ Errore creazione deal '{}': {}", dto.getIdAnagrafica(), e.getMessage(), e);
            }
        }

        logger.info("Creazione deal terminata. Totale: {}", dealMap.size());
        return dealMap;
    }

    public Integer addDeal(DealDTO dto, Map<String, Object> params) throws Exception {
        logger.info("Creazione deal per anagrafica {}", dto.getIdAnagrafica());

        Map<String, Object> fields = new HashMap<>();
        fields.put("TITLE", "Lotto " + dto.getIdAnagrafica());
        fields.put("UF_CRM_1762455213", dto.getIdAnagrafica());  // eventuale campo personalizzato

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fields", fields);

        String url = webHookUrl + "/crm.deal.add.json";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);

        if (response.getBody() != null && response.getBody().get("result") != null) {
            return (Integer) response.getBody().get("result");
        } else {
            throw new RuntimeException("Risposta Bitrix non valida durante la creazione del deal");
        }
    }

    // ----------------- AGGIORNAMENTO DEAL -----------------
    public boolean updateDeal(DealDTO dto, Map<String, Object> params) {
        if (dto.getId() == null || dto.getId() <= 0) {
            throw new IllegalArgumentException("ID del deal deve essere valido per l’update");
        }
        logger.info("Avvio aggiornamento deal ID: {}", dto.getId());
        String url = webHookUrl + "/crm.deal.update";

        Map<String, Object> fields = convertDtoToFields(dto);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", dto.getId());
        requestBody.put("fields", fields);
        requestBody.put("params", params != null ? params : Map.of());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> body = extractAndValidateBody(response);

        Object result = body.get("result");
        logger.info("Aggiornamento deal ID {} risultato: {}", dto.getId(), result);
        return Boolean.TRUE.equals(result);
    }

    // ----------------- GET DEAL PER ID -----------------
    public DealDTO getDealById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID del deal deve essere valido e positivo");
        }
        logger.info("Richiesta recupero deal per ID: {}", id);
        String url = baseUrl + "/rest/9/txk5orlo651kxu97/crm.deal.get.json";

        Map<String, Object> requestBody = Collections.singletonMap("ID", id);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> body = extractAndValidateBody(response);

        if (body.containsKey("result")) {
            Map<String, Object> resultMap = (Map<String, Object>) body.get("result");
            logger.info("Deal recuperato con successo per ID: {}", id);
            return mapToDealDTO(resultMap);
        }
        String msg = "Risposta inattesa dal server: " + body;
        logger.error(msg);
        throw new RuntimeException(msg);
    }

    // ----------------- LISTA DEAL -----------------
     public DealListResult getDealsList(List<String> select, Map<String, Object> filter,
                                       Map<String, String> order, int start) {
        logger.info("Richiesta lista deal");
        String url = baseUrl + "/rest/9/9yi2oktsybau3wkn/crm.deal.list.json";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("select", select != null ? select : Collections.singletonList("*"));
        requestBody.put("filter", filter != null ? filter : Collections.emptyMap());
        requestBody.put("order", order != null ? order : Collections.emptyMap());
        requestBody.put("start", start);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> body = extractAndValidateBody(response);

        List<DealDTO> deals = new ArrayList<>();
        Integer nextStart = null;

        if (body.containsKey("result")) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("result");
            for (Map<String, Object> item : results) {
                deals.add(mapToDealDTO(item));
            }
            logger.info("Recuperati {} deal", deals.size());
        } else {
            logger.info("Nessun deal recuperato");
        }

        if (body.containsKey("next")) {
            Object nextVal = body.get("next");
            if (nextVal != null) {
                try {
                    nextStart = Integer.parseInt(nextVal.toString());
                } catch (NumberFormatException ignored) { }
            }
        }

        return new DealListResult(deals, nextStart);
    }


    // ----------------- ELIMINAZIONE DEAL -----------------
    public boolean deleteDeal(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID del deal deve essere valido e positivo");
        }
        logger.info("Avvio cancellazione deal ID: {}", id);
        String url = webHookUrl +  "/crm.deal.delete";

        Map<String, Object> requestBody = Collections.singletonMap("ID", id);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> body = extractAndValidateBody(response);

        boolean deleted = Boolean.TRUE.equals(body.get("result"));
        logger.info("Cancellazione deal ID {} risultato: {}", id, deleted);
        return deleted;
    }

     //--------------------CONNETTI CONTATTO CON DEAL----------------------
    public void linkContactToDeal(Integer dealId, Integer contactId) {
        logger.info("Inizio collegamento contatto {} al deal {}", contactId, dealId);

        Map<String, Object> fields = new HashMap<>();
        fields.put("CONTACT_ID", contactId);
        fields.put("SORT", 100);
        fields.put("IS_PRIMARY", "Y");

        Map<String, Object> params = new HashMap<>();
        params.put("id", dealId);
        params.put("fields", fields);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

        String url = webHookUrl + "/crm.deal.contact.add.json";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || body.containsKey("error")) {
                throw new RuntimeException("Errore Bitrix durante collegamento contatto-deal: " + body);
            }

            logger.info("✅ Collegato contatto {} al deal {}", contactId, dealId);

        } catch (Exception e) {
            logger.error("❌ Errore durante il collegamento contatto {} → deal {}: {}", contactId, dealId, e.getMessage(), e);
            throw new RuntimeException("Errore nella chiamata REST per collegare contatto e deal: " + e.getMessage(), e);
        }
    }

  public List<Map<String, Object>> listaCustomFieldsDeal() {
    try {
        String url = baseUrl + "/rest/9/8l35dfi7lq1xbjwz/crm.deal.userfield.list.json";

        Map<String, Object> requestBody = Map.of(
                "filter", Collections.emptyMap(),
                "order", Map.of("SORT", "ASC", "ID", "ASC")
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = response.getBody();
            JsonNode result = root.path("result");

            if (result.isArray()) {
                List<Map<String, Object>> fields = new ArrayList<>();
                for (JsonNode fieldNode : result) {
                    fields.add(objectMapper.convertValue(fieldNode, Map.class));
                }

                logger.info("✅ Recuperati {} campi custom per i deal", fields.size());
                for (Map<String, Object> field : fields) {
                logger.info("🔹 Nome interno: {}, Etichetta: {}, Tipo: {}",
                field.get("FIELD_NAME"),
                field.get("EDIT_FORM_LABEL"),
                field.get("USER_TYPE_ID"));

                // Se il campo è enumerato, mostra anche la lista delle opzioni
                if ("enumeration".equals(field.get("USER_TYPE_ID"))) {
                    Object listObj = field.get("LIST");
                    if (listObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> mapItem) {
                                logger.info("   🟢 ID: {}, VALUE: {}",
                                        mapItem.get("ID"),
                                        mapItem.get("VALUE"));
                }
            }
        }
    }
}

                return fields;
            }
        }
    } catch (Exception e) {
        logger.error("❌ Errore durante il recupero dei custom field dei deal: {}", e.getMessage(), e);
    }

    return Collections.emptyList();
}


public List<DealDTO> recuperaTuttiDeal() {
        List<DealDTO> tuttiDeal = new ArrayList<>();

        try {
            // 🔹 Seleziona i campi utili (standard + custom)
            List<String> select = List.of(
                    "ID",
                    "TITLE",
                    "DATE_MODIFY",
                    "UF_CRM_1761843804",        // campo custom ResultCode
                    "UF_CRM_1762455213"         // campo custom ID anagrafica
            );

            // 🔹 Filtro: tutti i deal modificati da ultima verifica (o tutti se null)
            Map<String, Object> filter = new HashMap<>();
            if (ultimaVerifica != null) {
                String filtroData = ultimaVerifica.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                filter.put(">DATE_MODIFY", filtroData);
            }

            int start = 0;
            while (true) {
                DealListResult result = getDealsList(select, filter, null, start);
                List<DealDTO> dealsPage = result.getDeals();
                if (dealsPage.isEmpty()) break;

                tuttiDeal.addAll(dealsPage);
                logger.info("Recuperati {} deal da start={}", dealsPage.size(), start);

                Integer next = result.getNextStart();
                if (next == null || next == 0) break;
                start = next;

                sleepSafe(500); // evita rate limit
            }

            logger.info("✅ Totale deal recuperati da Bitrix: {}", tuttiDeal.size());

        } catch (Exception e) {
            logger.error("❌ Errore durante il recupero dei deal da Bitrix", e);
        }

        return tuttiDeal;
    }


public List<LeadRequest> trovaContattiModificati(List<DealDTO> tuttiDeal) throws Exception {
    List<LeadRequest> modificati = new ArrayList<>();

    // Mappa veloce per convertire valore testuale in ResultCode
    Map<String, ResultCode> resultCodeMap = Arrays.stream(ResultCode.values())
            .collect(Collectors.toMap(rc -> rc.getEsito().toLowerCase(), rc -> rc));

    logger.info("Lista ResultCode disponibili:");
    for (ResultCode rc : ResultCode.values()) {
        logger.info(" - {} -> esito='{}'", rc.name(), rc.getEsito());
    }

    for (DealDTO deal : tuttiDeal) {
        Integer dealId = deal.getId();
        if (dealId == null) {
            logger.warn("⚠️ Ignorato deal con ID null, titolo: {}", deal.getTitle());
            continue;
        }

        List<Long> contattiDelDeal = getContattiDaDeal(dealId.longValue());
        if (contattiDelDeal == null || contattiDelDeal.isEmpty()) {
            logger.warn("⚠️ Nessun contatto trovato per deal {}", dealId);
            continue;
        }

        Long contactId = contattiDelDeal.get(0);
        ContactDTO contact = contactService.getContattoById(contactId.intValue());
        if (contact == null) {
            logger.warn("⚠️ Contatto {} non trovato per deal {}", contactId, dealId);
            continue;
        }

        // Mapping ResultCode
        ResultCode mapped = mapResultCodeFromComments(contact.getCOMMENTS(), resultCodeMap);

        // Estrazione contactId da SOURCE_DESCRIPTION con fallback
        Long finalContactId = extractContactId(contact.getSOURCE_DESCRIPTION(), contactId, dealId);

        // Creazione LeadRequest con tutti i campi obbligatori
        LeadRequest req = new LeadRequest();
        req.setContactId(finalContactId);
        req.setResultCode(mapped);

        req.setCaller("3932644963");
        req.setWorkedCode(extractPhone(contact));
        req.setWorkedType("O");
        req.setCampaignId(65704L);

        // Date attività
        setActivityDates(req, activityService.getUltimaActivityPerDeal(dealId));

        modificati.add(req);

        logger.info("Deal {} - contact {} - LeadRequest pronto per invio: resultCode={}, phone={}, dates {}->{}",
                dealId, finalContactId, mapped, req.getWorked_Date(), req.getWorked_End_Date());
    }

    logger.info("✅ Totale contatti modificati trovati: {}", modificati.size());
    return modificati;
}



// --- Helper methods ---

/**
 * Mappa COMMENTS di un contatto a ResultCode usando "contains" per supportare testo extra
 */
private ResultCode mapResultCodeFromComments(String comments, Map<String, ResultCode> resultCodeMap) {
    if (comments == null || comments.isBlank()) return ResultCode.UNKNOWN;
    String c = comments.trim().toLowerCase();
    for (Map.Entry<String, ResultCode> entry : resultCodeMap.entrySet()) {
        if (c.contains(entry.getKey())) {
            return entry.getValue();
        }
    }
    return ResultCode.UNKNOWN;
}

private Long extractContactId(String sourceDescription, Long fallbackContactId, Integer dealId) {
    if (sourceDescription != null && !sourceDescription.isBlank()) {
        try {
            return Long.valueOf(sourceDescription.trim());
        } catch (NumberFormatException e) {
            logger.warn("⚠️ Deal {} - SOURCE_DESCRIPTION non numerico ({}), uso fallback contactId = {}", dealId, sourceDescription, fallbackContactId);
        }
    } else {
        logger.warn("⚠️ Deal {} senza SOURCE_DESCRIPTION, uso fallback contactId = {}", dealId, fallbackContactId);
    }
    return fallbackContactId;
}

private String extractPhone(ContactDTO contact) {
    if (contact.getPHONE() != null && !contact.getPHONE().isEmpty()) {
        return contact.getPHONE().get(0).getVALUE();
    }
    return "+0000000000";
}

private void setActivityDates(LeadRequest req, ActivityDTO activity) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/dd/MM HH:mm:ss"); // formato Enel

        LocalDateTime start = now;
        LocalDateTime end = now.plusMinutes(2);

        if (activity != null && activity.getStartTime() != null) {
            start = activity.getStartTime();
            end = activity.getEndTime() != null ? activity.getEndTime() : start.plusMinutes(2);
        }

        req.setWorked_Date(start);
        req.setWorked_End_Date(end);
    }


    
    private Integer extractNextStartFromLastResponse() {
        return 0;
    }


    private void sleepSafe(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

    public List<Long> trovaDealConAttivitaModificate() {
        List<Long> dealConAttivitaModificate = new ArrayList<>();

        try {
            Map<String, Object> filter = Map.of("OWNER_TYPE_ID", 2); // 2 = Deal in Bitrix24
           List<ActivityDTO> activities = activityService.getActivityList(filter, null, 0).getActivities();

            for (ActivityDTO nuova : activities) {
                ActivityDTO vecchia = cacheAttivita.get(nuova.getId());

                boolean modificata = vecchia == null ||
                        !Objects.equals(vecchia.getDateModify(), nuova.getDateModify());

                if (modificata) {
                    cacheAttivita.put(nuova.getId(), nuova);
                    if (nuova.getOwnerId() != null) {
                        dealConAttivitaModificate.add(nuova.getOwnerId());
                        logger.info("🔁 Attività {} modificata o nuova → Deal {}", nuova.getId(), nuova.getOwnerId());
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Errore durante il controllo delle attività modificate", e);
        }

        return dealConAttivitaModificate;
    }



    public List<Long> getContattiDaDeal(Long dealId) {
        try {
            String url = baseUrl + "/rest/9/1uuo825zp4af6sha/crm.deal.contact.items.get.json";
            Map<String, Object> requestBody = Map.of("id", dealId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("result")) return Collections.emptyList();

            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("result");
            List<Long> contatti = new ArrayList<>();

            for (Map<String, Object> contact : results) {
                if (contact.get("CONTACT_ID") != null)
                    contatti.add(Long.valueOf(contact.get("CONTACT_ID").toString()));
            }

            logger.info("Deal {} collegato a {} contatti", dealId, contatti.size());

            // Rate limit friendly delay
            sleepSafe(500);
            return contatti;

        } catch (HttpServerErrorException e) {
            if (e.getResponseBodyAsString().contains("QUERY_LIMIT_EXCEEDED")) {
                logger.warn("⏸️ Rate limit Bitrix24 raggiunto. Attesa 60 secondi...");
                sleepSafe(60_000);
            }
            logger.error("Errore 503 nel recupero contatti per deal {}", dealId, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Errore nel recupero contatti per deal {}", dealId, e);
            return Collections.emptyList();
        }
    }

    public class DealListResult {
        private List<DealDTO> deals;
        private Integer nextStart;

        public DealListResult(List<DealDTO> deals, Integer nextStart) {
            this.deals = deals;
            this.nextStart = nextStart;
        }

        public List<DealDTO> getDeals() {
            return deals;
        }

        public Integer getNextStart() {
            return nextStart;
        }
    }



    // ----------------- HELPERS -----------------

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> extractAndValidateBody(ResponseEntity<Map> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Errore HTTP: " + response.getStatusCode());
        }

        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new RuntimeException("Risposta vuota dal server");
        }

        if (body.containsKey("error")) {
            String error = (String) body.get("error");
            String errorDesc = (String) body.get("error_description");
            switch (error) {
                case "ID is not defined or invalid":
                    throw new IllegalArgumentException(errorDesc);
                case "Access denied":
                    throw new SecurityException(errorDesc);
                case "Not found":
                    throw new IllegalStateException(errorDesc);
                case "NO_AUTH_FOUND":
                    throw new SecurityException("Autenticazione non valida: " + errorDesc);
                case "INVALID_REQUEST":
                    throw new IllegalArgumentException(errorDesc);
                    // Aggiungere altri codici errore specifici qui se necessario
                default:
                    throw new RuntimeException("Errore API Bitrix24: " + errorDesc);
            }
        }

        return body;
    }

    private Map<String, Object> convertDtoToFields(DealDTO dto) {
        Map<String, Object> fields = new HashMap<>();
        if (dto.getTitle() != null) fields.put("TITLE", dto.getTitle());
        if (dto.getTypeId() != null) fields.put("TYPE_ID", dto.getTypeId());
        if (dto.getCategoryId() != null) fields.put("CATEGORY_ID", dto.getCategoryId());
        if (dto.getStageId() != null) fields.put("STAGE_ID", dto.getStageId());
        if (dto.getIsRecurring() != null) fields.put("IS_RECURRING", dto.getIsRecurring());
        if (dto.getIsReturnCustomer() != null) fields.put("IS_RETURN_CUSTOMER", dto.getIsReturnCustomer());
        if (dto.getIsRepeatedApproach() != null) fields.put("IS_REPEATED_APPROACH", dto.getIsRepeatedApproach());
        if (dto.getProbability() != null) fields.put("PROBABILITY", dto.getProbability());
        if (dto.getCurrencyId() != null) fields.put("CURRENCY_ID", dto.getCurrencyId());
        if (dto.getOpportunity() != null) fields.put("OPPORTUNITY", dto.getOpportunity());
        if (dto.getIsManualOpportunity() != null) fields.put("IS_MANUAL_OPPORTUNITY", dto.getIsManualOpportunity());
        if (dto.getTaxValue() != null) fields.put("TAX_VALUE", dto.getTaxValue());
        if (dto.getCompanyId() != null) fields.put("COMPANY_ID", dto.getCompanyId());
        if (dto.getContactIds() != null) fields.put("CONTACT_IDS", dto.getContactIds());
        if (dto.getBeginDate() != null) fields.put("BEGINDATE", dto.getBeginDate());
        if (dto.getCloseDate() != null) fields.put("CLOSEDATE", dto.getCloseDate());
        if (dto.getOpened() != null) fields.put("OPENED", dto.getOpened());
        if (dto.getClosed() != null) fields.put("CLOSED", dto.getClosed());
        if (dto.getComments() != null) fields.put("COMMENTS", dto.getComments());
        if (dto.getAssignedById() != null) fields.put("ASSIGNED_BY_ID", dto.getAssignedById());
        if (dto.getSourceId() != null) fields.put("SOURCE_ID", dto.getSourceId());
        if (dto.getSourceDescription() != null) fields.put("SOURCE_DESCRIPTION", dto.getSourceDescription());
        if (dto.getAdditionalInfo() != null) fields.put("ADDITIONAL_INFO", dto.getAdditionalInfo());
        if (dto.getLocationId() != null) fields.put("LOCATION_ID", dto.getLocationId());
        if (dto.getOriginatorId() != null) fields.put("ORIGINATOR_ID", dto.getOriginatorId());
        if (dto.getOriginId() != null) fields.put("ORIGIN_ID", dto.getOriginId());
        if (dto.getUtmSource() != null) fields.put("UTM_SOURCE", dto.getUtmSource());
        if (dto.getUtmMedium() != null) fields.put("UTM_MEDIUM", dto.getUtmMedium());
        if (dto.getUtmCampaign() != null) fields.put("UTM_CAMPAIGN", dto.getUtmCampaign());
        if (dto.getUtmContent() != null) fields.put("UTM_CONTENT", dto.getUtmContent());
        if (dto.getUtmTerm() != null) fields.put("UTM_TERM", dto.getUtmTerm());
        if (dto.getCustomFields() != null) fields.putAll(dto.getCustomFields());
        return fields;
    }

    private DealDTO mapToDealDTO(Map<String, Object> map) {
        DealDTO dto = new DealDTO();
        dto.setId(parseInteger(map.get("ID")));
        dto.setTitle((String) map.get("TITLE"));
        dto.setTypeId((String) map.get("TYPE_ID"));
        dto.setCategoryId(parseInteger(map.get("CATEGORY_ID")));
        dto.setStageId((String) map.get("STAGE_ID"));
        dto.setOpportunity(parseDouble(map.get("OPPORTUNITY")));
        dto.setIsManualOpportunity((String) map.get("IS_MANUAL_OPPORTUNITY"));
        dto.setAssignedById(parseInteger(map.get("ASSIGNED_BY_ID")));
        // Gestione campi custom
        Map<String, Object> customFields = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().startsWith("UF_CRM_")) {
                customFields.put(entry.getKey(), entry.getValue());
            }
        }
        dto.setCustomFields(customFields);
        return dto;
    }

    private Integer parseInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }




}


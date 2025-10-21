package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.DealDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;

@Service
public class DealService {

     private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(DealService.class);

    public DealService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    // ----------------- CREAZIONE DEAL -----------------
    // Crea deal da JSON del lotto
    public void creaDealDaLotto(String idLotto, String json, String accessToken) throws Exception {
        logger.info("Avvio creazione deal da lotto id: {}", idLotto);


        List<DealDTO> deals = objectMapper.readValue(json, new TypeReference<List<DealDTO>>() {});
        for (DealDTO dto : deals) {
            try {
                addDeal(dto, null, accessToken);
            } catch (Exception e) {
                logger.error("Errore creazione deal: {}", dto.getTitle(), e);
            }
        }
    }

    // Creazione singolo deal
    public Integer addDeal(DealDTO dto, Map<String, Object> params, String accessToken) {
        logger.info("Avvio creazione deal con titolo: {}", dto.getTitle());
        String url = baseUrl + "/rest/crm.deal.add";
        Map<String, Object> fields = convertDtoToFields(dto);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fields", fields);
        requestBody.put("params", params != null ? params : Map.of());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> body = extractAndValidateBody(response);

        if (body.containsKey("result") && body.get("result") instanceof Integer) {
            Integer dealId = (Integer) body.get("result");
            logger.info("Deal creato con ID: {}", dealId);
            return dealId;
        }
        throw new RuntimeException("Risposta inattesa dal server: " + body);
    }

    // ----------------- AGGIORNAMENTO DEAL -----------------
    public boolean updateDeal(DealDTO dto, Map<String, Object> params, String accessToken) {
        if (dto.getId() == null || dto.getId() <= 0) {
            throw new IllegalArgumentException("ID del deal deve essere valido per l’update");
        }
        logger.info("Avvio aggiornamento deal ID: {}", dto.getId());
        String url = baseUrl + "/rest/crm.deal.update";

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
    public DealDTO getDealById(Integer id, String accessToken) {
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
    public List<DealDTO> getDealsList(List<String> select, Map<String, Object> filter,
                                      Map<String, String> order, int start, String accessToken) {
        logger.info("Richiesta lista deal con filter: {}, order: {}, start: {}", filter, order, start);
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
        if (body.containsKey("result")) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("result");
            for (Map<String, Object> item : results) {
                deals.add(mapToDealDTO(item));
            }
            logger.info("Recuperati {} deal", deals.size());
            return deals;
        }
        logger.info("Nessun deal recuperato");
        return deals;
    }

    // ----------------- ELIMINAZIONE DEAL -----------------
    public boolean deleteDeal(Integer id, String accessToken) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID del deal deve essere valido e positivo");
        }
        logger.info("Avvio cancellazione deal ID: {}", id);
        String url = baseUrl + "/rest/crm.deal.delete";

        Map<String, Object> requestBody = Collections.singletonMap("ID", id);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> body = extractAndValidateBody(response);

        boolean deleted = Boolean.TRUE.equals(body.get("result"));
        logger.info("Cancellazione deal ID {} risultato: {}", id, deleted);
        return deleted;
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

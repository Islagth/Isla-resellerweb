package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.ActivityDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ActivityService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String  webHookUrl;
    

    private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);

    public ActivityService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl,@Value("https://b24-vayzx4.bitrix24.it/rest/9/txk5orlo651kxu97") String webHookUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.webHookUrl = webHookUrl;
    }


    public List<ActivityDTO> getActivityList(Map<String, Object> filter, List<String> select, int start) {
        try {
            String url = baseUrl + "/rest/9/27wvf2b46se5233m/crm.activity.list.json";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filter", filter != null ? filter : Collections.emptyMap());
            requestBody.put("select", select != null ? select : List.of("ID", "OWNER_ID", "OWNER_TYPE_ID", "TYPE_ID", "DATE_MODIFY", "SUBJECT", "DESCRIPTION", "START_TIME", "END_TIME", "RESPONSIBLE_ID", "STATUS"));
            requestBody.put("start", start);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = extractAndValidateBody(response);
            List<ActivityDTO> activities = new ArrayList<>();

            if (body.containsKey("result")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("result");
                for (Map<String, Object> item : results) {
                    ActivityDTO dto = new ActivityDTO();
                    dto.setId(Long.valueOf(item.get("ID").toString()));
                    dto.setOwnerId(item.get("OWNER_ID") != null ? Long.valueOf(item.get("OWNER_ID").toString()) : null);
                    dto.setOwnerTypeId(item.get("OWNER_TYPE_ID") != null ? Integer.valueOf(item.get("OWNER_TYPE_ID").toString()) : null);
                    dto.setTypeId((String) item.get("TYPE_ID"));
                    dto.setDescription((String) item.get("DESCRIPTION"));
                    dto.setSubject((String) item.get("SUBJECT"));
                    dto.setStatus((String) item.get("STATUS"));
                    dto.setResponsibleId(item.get("RESPONSIBLE_ID") != null ? Long.valueOf(item.get("RESPONSIBLE_ID").toString()) : null);

                    // Conversione delle date
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    if (item.get("DATE_MODIFY") != null)
                        dto.setDateModify(LocalDateTime.parse(item.get("DATE_MODIFY").toString(), formatter));
                    if (item.get("START_TIME") != null)
                        dto.setStartTime(LocalDateTime.parse(item.get("START_TIME").toString(), formatter));
                    if (item.get("END_TIME") != null)
                        dto.setEndTime(LocalDateTime.parse(item.get("END_TIME").toString(), formatter));

                    activities.add(dto);
                }
            }

            logger.info("Recuperate {} attività da Bitrix24", activities.size());
            return activities;

        } catch (Exception e) {
            logger.error("Errore durante il recupero delle attività da Bitrix24", e);
            return Collections.emptyList();
        }
    }

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

}

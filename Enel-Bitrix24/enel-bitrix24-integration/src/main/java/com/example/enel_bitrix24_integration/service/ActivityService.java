package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.ActivityDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ActivityService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String  webHookUrl;

    private final DealService dealService;
    

    private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);

    @Autowired
    public ActivityService(RestTemplate restTemplate, @Value("${bitrix24.api.base-url}") String baseUrl, @Value("https://b24-vayzx4.bitrix24.it/rest/9/txk5orlo651kxu97") String webHookUrl,@Lazy DealService dealService) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.webHookUrl = webHookUrl;
        this.dealService = dealService;
    }


    public ActivityListResult getActivityList(Map<String, Object> filter, List<String> select, int start) {
        try {
            String url = baseUrl + "/rest/9/27wvf2b46se5233m/crm.activity.list.json";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filter", filter != null ? filter : Collections.emptyMap());
            requestBody.put("select", select != null ? select : List.of(
                    "ID", "OWNER_ID", "OWNER_TYPE_ID", "TYPE_ID", "DATE_MODIFY",
                    "SUBJECT", "DESCRIPTION", "START_TIME", "END_TIME",
                    "RESPONSIBLE_ID", "STATUS"
            ));
            requestBody.put("start", start);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createJsonHeaders());
            ResponseEntity<Map> response = callBitrixApiWithRetry(url, entity, Map.class);

            Map<String, Object> body = extractAndValidateBody(response);
            List<ActivityDTO> activities = new ArrayList<>();
            Integer nextStart = null;

            if (body.containsKey("result")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("result");

                for (Map<String, Object> item : results) {
                    ActivityDTO dto = new ActivityDTO();
                    dto.setId(Long.valueOf(item.get("ID").toString()));
                    dto.setOwnerId(item.get("OWNER_ID") != null ? Long.valueOf(item.get("OWNER_ID").toString()) : null);
                    dto.setOwnerTypeId(item.get("OWNER_TYPE_ID") != null ? Integer.valueOf(item.get("OWNER_TYPE_ID").toString()) : null);
                    dto.setTypeId(item.get("TYPE_ID") != null ? item.get("TYPE_ID").toString() : null);
                    dto.setDescription((String) item.get("DESCRIPTION"));
                    dto.setSubject((String) item.get("SUBJECT"));
                    dto.setStatus((String) item.get("STATUS"));
                    dto.setResponsibleId(item.get("RESPONSIBLE_ID") != null ? Long.valueOf(item.get("RESPONSIBLE_ID").toString()) : null);

                    dto.setDateModify(parseDateSafely(Objects.toString(item.get("DATE_MODIFY"), "")));
                    dto.setStartTime(parseDateSafely(Objects.toString(item.get("START_TIME"), "")));
                    dto.setEndTime(parseDateSafely(Objects.toString(item.get("END_TIME"), "")));

                    activities.add(dto);
                }
            }

            if (body.containsKey("next")) {
                Object nextVal = body.get("next");
                if (nextVal != null) {
                    try {
                        nextStart = Integer.parseInt(nextVal.toString());
                    } catch (NumberFormatException ignored) {}
                }
            }

           
            return new ActivityListResult(activities, nextStart);

        } catch (Exception e) {
            logger.error("❌ Errore durante il recupero delle attività da Bitrix24", e);
            return new ActivityListResult(Collections.emptyList(), null);
        }
    }

   public ActivityDTO getUltimaActivityPerDeal(Integer dealId) {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("OWNER_ID", dealId);
            filter.put("OWNER_TYPE_ID", 2); // 3 = Contact in Bitrix CRM
            filter.put("TYPE_ID", 2);

            List<String> select = List.of(
                    "ID", "OWNER_ID", "TYPE_ID", "START_TIME", "END_TIME", "DATE_MODIFY", "SUBJECT", "DESCRIPTION"
            );

            // ✅ Recupera la lista di attività dal metodo principale
            List<ActivityDTO> activities = getActivityList(filter, select, 0).getActivities();

            if (activities == null || activities.isEmpty()) {
                logger.info("ℹ️ Nessuna activity trovata per il deal {}", dealId);
                return null;
            }

            // ✅ Ordina per data di modifica o fine chiamata (più recente per prima)
            activities.sort(Comparator.comparing(
                    (ActivityDTO a) -> Optional.ofNullable(a.getDateModify()).orElse(a.getEndTime()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed());

            ActivityDTO ultima = activities.get(0);
            logger.info("📞 Ultima activity per deal {} → ID: {}, modificata il {}", dealId, ultima.getId(), ultima.getDateModify());
            return ultima;

        } catch (Exception e) {
            logger.warn("⚠️ Errore nel recupero dell’ultima activity per deal {}: {}", dealId, e.getMessage());
            return null;
        }
    }
    /*public Set<Long> trovaContattiInAttesaDaAttivitaModificate() {
        Set<Long> contattiInAttesa = new HashSet<>();
        Map<Long, List<ActivityDTO>> dealAttivitaMap = new HashMap<>();

        try {
            int start = 0;
            boolean continua = true;

            while (continua) {
                Map<String, Object> filter = Map.of("OWNER_TYPE_ID", 2); // Assicurarsi che OWNER_TYPE_ID sia int 2
                ActivityListResult result = getActivityList(filter, null, start); // Usa il nuovo metodo paginato

                List<ActivityDTO> activities = result.getActivities();

                if (activities == null || activities.isEmpty()) break;

                for (ActivityDTO nuova : activities) {
                    if (nuova.getId() == null || nuova.getDateModify() == null) continue;

                    ActivityDTO vecchia = dealService.cacheAttivita.get(nuova.getId());
                    boolean modificata = vecchia == null ||
                            !Objects.equals(vecchia.getDateModify(), nuova.getDateModify());

                    if (modificata) {
                        dealService.cacheAttivita.put(nuova.getId(), nuova);

                        if (nuova.getOwnerId() != null) {
                            dealAttivitaMap
                                    .computeIfAbsent(nuova.getOwnerId(), k -> new ArrayList<>())
                                    .add(nuova);
                            logger.debug("🟢 Attività {} aggiornata per deal {}", nuova.getId(), nuova.getOwnerId());
                        }
                    }
                }

                Integer next = result.getNextStart();
                if (next == null || next == 0) {
                    continua = false;
                } else {
                    start = next;
                }
            }

            // 🔹 Recupera i contatti collegati a ciascun deal modificato
            for (Long dealId : dealAttivitaMap.keySet()) {
                List<Long> contatti = dealService.getContattiDaDeal(dealId);
                if (!contatti.isEmpty()) {
                    contattiInAttesa.addAll(contatti);
                    logger.info("🔁 Deal {} → {} contatti in attesa", dealId, contatti.size());
                }
            }

        } catch (Exception e) {
            logger.error("🔥 Errore durante il recupero dei contatti da attività modificate", e);
        }

        logger.info("✅ Totale contatti in attesa trovati: {}", contattiInAttesa.size());
        return contattiInAttesa;
    }*/


    private <T> ResponseEntity<T> callBitrixApiWithRetry(String url, HttpEntity<?> entity, Class<T> responseType) {
        int maxRetries = 5;
        int attempt = 0;
        long waitTime = 1500; // 1.5 secondi iniziali

        while (attempt < maxRetries) {
            try {
                return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            } catch (HttpServerErrorException e) {
                String body = e.getResponseBodyAsString();
                if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && body != null && body.contains("QUERY_LIMIT_EXCEEDED")) {
                    attempt++;
                    logger.warn("⏳ Limite Bitrix24 raggiunto (tentativo #{}/{}). Attendo {}ms prima di riprovare...", attempt, maxRetries, waitTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime *= 2; // backoff esponenziale
                } else {
                    throw e; // altri errori: interrompi
                }
            } catch (ResourceAccessException rae) {
                // Gestisce timeout di rete
                attempt++;
                logger.warn("🌐 Timeout o rete non disponibile (tentativo #{}/{}). Ritento tra {}ms...", attempt, maxRetries, waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                waitTime *= 2;
            }
        }

        throw new RuntimeException("❌ Limite Bitrix24 ancora superato dopo " + maxRetries + " tentativi");
    }

    /**
     * 🔧 Metodo helper per parsare date in modo sicuro da Bitrix24.
     * Gestisce sia formati "yyyy-MM-dd HH:mm:ss" che ISO (es. 2025-11-04T15:31:20Z)
     */
     private String parseDateSafely(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ISO_DATE_TIME
        );

        for (DateTimeFormatter fmt : formatters) {
            try {
                LocalDateTime dt = LocalDateTime.parse(dateString.trim(), fmt);
                return dt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            } catch (DateTimeParseException ignored) {}
        }

        logger.warn("⚠️ Formato data non riconosciuto da Bitrix24: '{}'", dateString);
        return null;
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

    public static class ActivityListResult {
        private List<ActivityDTO> activities;
        private Integer nextStart;

        public ActivityListResult(List<ActivityDTO> activities, Integer nextStart) {
            this.activities = activities;
            this.nextStart = nextStart;
        }

        public List<ActivityDTO> getActivities() {
            return activities;
        }

        public Integer getNextStart() {
            return nextStart;
        }
    }


}
















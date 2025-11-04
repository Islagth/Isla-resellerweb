package com.example.enel_bitrix24_integration.config;

import com.example.enel_bitrix24_integration.dto.ActivityDTO;
import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.dto.ResultCode;
import com.example.enel_bitrix24_integration.service.ActivityService;
import com.example.enel_bitrix24_integration.service.BitrixService;
import com.example.enel_bitrix24_integration.service.ContactService;
import com.example.enel_bitrix24_integration.service.DealService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class LeadScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeadScheduler.class);

    private final BitrixService bitrixService;
    private final ContactService contactService;
    private final DealService dealService;
    private final ActivityService activityService;

    private final Map<Long, String> contattiCache = new HashMap<>();
    private final Map<Long, ActivityDTO> attivitaCache = new HashMap<>();

    // Lista thread-safe in memoria
    private final List<LeadRequest> contattiInAttesa = new CopyOnWriteArrayList<>();

    public LeadScheduler(BitrixService bitrixService, ContactService contactService, DealService dealService, ActivityService activityService) {
        this.bitrixService = bitrixService;
        this.contactService = contactService;
        this.dealService = dealService;
        this.activityService = activityService;
    }

    @PostConstruct
    public void inizializzaCache() {
        logger.info("🔄 Inizializzazione cache contatti e attività...");

        try {
            List<LeadRequest> tuttiContatti = contactService.trovaContattiModificati();
            for (LeadRequest lead : tuttiContatti) {
                contattiCache.put(lead.getContactId(), String.valueOf(lead.getResultCode()));
            }
            Map<String, Object> filter = Map.of("OWNER_TYPE_ID", 2);
            List<ActivityDTO> attivitaIniziali = activityService.getActivityList(filter, null, 0);
            for (ActivityDTO attivita : attivitaIniziali) {
                attivitaCache.put(attivita.getId(), attivita);
            }
        } catch (Exception e) {
            logger.error("❌ Errore durante l’inizializzazione delle cache", e);
        }

        logger.info("✅ Cache inizializzata con {} contatti e {} attività", contattiCache.size(), attivitaCache.size());
    }


    /**
     * Aggiunge un contatto alla lista in attesa di invio
     */
    public void aggiungiContatto(LeadRequest request) {
        contattiInAttesa.add(request);
    }

    /**
     * 🔄 Ogni 15 minuti controlla i contatti modificati in Bitrix e li aggiunge alla coda
     */
    @Scheduled(fixedRate = 900_000)
    public void controllaModifiche() {
        logger.info("⏰ Avvio controllo periodico modifiche contatti e attività deal...");

        try {
            Set<Long> contattiInAttesa = new HashSet<>();

            // 1️⃣ Contatti modificati direttamente
            List<LeadRequest> contattiAggiornati = contactService.trovaContattiModificati();
            for (LeadRequest lead : contattiAggiornati) {
                Long contactId = lead.getContactId();
                String nuovoResultCode = String.valueOf(lead.getResultCode());
                String vecchioResultCode = contattiCache.get(contactId);

                boolean modificato = (vecchioResultCode == null || !Objects.equals(vecchioResultCode, nuovoResultCode));

                if (modificato) {
                    contattiCache.put(contactId, nuovoResultCode);
                    contattiInAttesa.add(contactId);
                    logger.info("📇 Contatto {} modificato direttamente aggiunto alla lista in attesa", contactId);
                }
            }

            // 2️⃣ Contatti derivati da attività modificate sui deal
            Set<Long> contattiDaAttivita = activityService.trovaContattiInAttesaDaAttivitaModificate();
            contattiInAttesa.addAll(contattiDaAttivita);

            // 3️⃣ Ciclo su tutti i contatti in attesa
            Map<Long, String> resultCodeCacheTemp = new HashMap<>(); // cache temporanea per chiamate getResultCode
            for (Long contactId : contattiInAttesa) {
                LeadRequest req = new LeadRequest();
                req.setContactId(contactId);
                req.setWorkedCode("AUTO_FROM_SCHEDULER");
                req.setResultCode(ResultCode.D102);
                req.setCaller("AUTO_SCHEDULER");
                req.setWorkedType("O");

                // Recupera RESULT_CODE dinamicamente se non presente in cache temporanea
                String resultCode = resultCodeCacheTemp.computeIfAbsent(contactId, id -> contactService.getResultCodeForContact(Math.toIntExact(id)));
                req.setResultCode(resultCode != null ? ResultCode.valueOf(resultCode) : ResultCode.D102);

                // Recupera ultima attività associata
                ActivityDTO ultimaActivity = activityService.getUltimaActivityPerContatto(Math.toIntExact(contactId));
                if (ultimaActivity != null && ultimaActivity.getStartTime() != null && ultimaActivity.getEndTime() != null) {
                    req.setWorked_Date(ultimaActivity.getStartTime());
                    req.setWorked_End_Date(ultimaActivity.getEndTime());
                } else {
                    req.setWorked_Date(LocalDateTime.now());
                    req.setWorked_End_Date(LocalDateTime.now().plusMinutes(2));
                }

                aggiungiContatto(req);
                logger.info("📋 Contatto {} aggiunto alla coda invii automatici", contactId);
            }

            if (contattiInAttesa.isEmpty()) {
                logger.info("✅ Nessun contatto in attesa rilevato in questo ciclo");
            } else {
                logger.info("📌 Totale contatti in attesa: {}", contattiInAttesa.size());
            }

        } catch (Exception e) {
            logger.error("❌ Errore durante il controllo periodico modifiche contatti/attività", e);
        }

        logger.info("✅ Controllo completato.\n");
    }





    /**
     * 📤 Ogni ora invia i contatti accumulati verso Enel
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void invioMultiploContatti() {
        if (contattiInAttesa.isEmpty()) {
            logger.info("📭 Nessun contatto da inviare in questo ciclo orario.");
            return;
        }

        logger.info("🚀 Invio di {} contatti in corso...", contattiInAttesa.size());
        for (LeadRequest request : List.copyOf(contattiInAttesa)) {
            LeadResponse response = bitrixService.invioLavorato(request);

            if (response.isSuccess()) {
                logger.info("✅ Contatto {} inviato correttamente.", request.getWorkedCode());
            } else {
                logger.warn("⚠️ Errore invio contatto {}: {}", request.getWorkedCode(), response.getMessage());
            }
        }

        contattiInAttesa.clear();
        logger.info("🧹 Lista contatti svuotata dopo l’invio orario.");
    }
}


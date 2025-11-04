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
    @Scheduled(fixedRate = 900000)
    public void controllaModifiche() {
        logger.info("⏰ Avvio controllo periodico modifiche contatti e attività deal...");

        try {
            // 1️⃣ Controllo contatti modificati
            List<LeadRequest> contattiAggiornati = contactService.trovaContattiModificati();
            for (LeadRequest lead : contattiAggiornati) {
                Long contactId = lead.getContactId();
                String nuovoResultCode = String.valueOf(lead.getResultCode());
                String vecchioResultCode = contattiCache.get(contactId);

                boolean modificato = (vecchioResultCode == null || !Objects.equals(vecchioResultCode, nuovoResultCode));

                if (modificato) {
                    contattiCache.put(contactId, nuovoResultCode);

                    // 🔍 Recupera l’ultima activity associata al contatto
                    ActivityDTO ultimaActivity = activityService.getUltimaActivityPerContatto(Math.toIntExact(contactId));

                    if (ultimaActivity != null && ultimaActivity.getStartTime() != null && ultimaActivity.getEndTime() != null) {
                        lead.setWorked_Date(ultimaActivity.getStartTime());
                        lead.setWorked_End_Date(ultimaActivity.getEndTime());
                    } else {
                        lead.setWorked_Date(LocalDateTime.now());
                        lead.setWorked_End_Date(LocalDateTime.now().plusMinutes(2));
                    }

                    // Aggiunge alla coda di invio
                    aggiungiContatto(lead);
                    logger.info("📇 Contatto {} aggiunto alla coda invii automatici", contactId);
                }
            }

            // 2️⃣ Controllo attività nuove o modificate collegate ai deal
            Map<String, Object> filter = Map.of("OWNER_TYPE_ID", 2); // 2 = Deal
            List<ActivityDTO> attivita = activityService.getActivityList(filter, null, 0);
            Set<Long> dealConAttivitaModificate = new HashSet<>();

            for (ActivityDTO nuova : attivita) {
                ActivityDTO vecchia = attivitaCache.get(nuova.getId());
                boolean modificata = (vecchia == null ||
                        !Objects.equals(vecchia.getDateModify(), nuova.getDateModify()));

                if (modificata) {
                    attivitaCache.put(nuova.getId(), nuova);
                    if (nuova.getOwnerId() != null) {
                        dealConAttivitaModificate.add(nuova.getOwnerId());
                        logger.info("📝 Attività {} modificata o nuova → Deal {}", nuova.getId(), nuova.getOwnerId());
                    }
                }
            }

            // 3️⃣ Recupera i contatti collegati ai deal modificati
            for (Long dealId : dealConAttivitaModificate) {
                List<Long> contattiDaDeal = dealService.getContattiDaDeal(dealId);
                for (Long contactId : contattiDaDeal) {
                    ActivityDTO ultimaActivity = activityService.getUltimaActivityPerContatto(Math.toIntExact(contactId));
                    LeadRequest req = new LeadRequest();
                    req.setContactId(contactId);
                    req.setWorkedCode("AUTO_FROM_DEAL");
                    req.setResultCode(ResultCode.D102);
                    req.setCaller("AUTO_SCHEDULER");
                    req.setWorkedType("O");

                    if (ultimaActivity != null) {
                        req.setWorked_Date(ultimaActivity.getStartTime());
                        req.setWorked_End_Date(ultimaActivity.getEndTime());
                    } else {
                        req.setWorked_Date(LocalDateTime.now());
                        req.setWorked_End_Date(LocalDateTime.now().plusMinutes(2));
                    }

                    aggiungiContatto(req);
                }
            }

            if (contattiInAttesa.isEmpty()) {
                logger.info("✅ Nessun contatto in attesa rilevato in questo ciclo");
            } else {
                logger.info("📋 Totale contatti in attesa: {}", contattiInAttesa.size());
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


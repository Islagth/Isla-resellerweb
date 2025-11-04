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
    private volatile boolean inEsecuzione = false; //

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
    private void sleepSafe(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

    @Scheduled(fixedRate = 900_000) // ogni 15 minuti
    public synchronized void controllaModifiche() {
        if (inEsecuzione) {
            logger.warn("⏳ Scheduler già in esecuzione. Salto questo ciclo.");
            return;
        }
        inEsecuzione = true;

        logger.info("⏰ Avvio controllo periodico modifiche contatti e attività deal...");

        try {
            Set<Long> contattiAggiornati = new HashSet<>();

            // 1️⃣ Contatti modificati (gestiti internamente dal ContactService)
            List<LeadRequest> leadsModificati = contactService.trovaContattiModificati();
            if (leadsModificati.isEmpty()) {
                logger.info("📭 Nessun contatto modificato rilevato.");
            } else {
                for (LeadRequest lead : leadsModificati) {
                    contattiAggiornati.add(lead.getContactId());
                    contattiInAttesa.add(lead); // già completo dal ContactService
                    logger.info("📇 Contatto {} aggiunto dai contatti modificati (ResultCode: {})",
                            lead.getContactId(), lead.getResultCode());
                    sleepSafe(300);
                }
            }

            // 2️⃣ Attività modificate → ottieni i contatti collegati
            Set<Long> contattiDaAttivita = activityService.trovaContattiInAttesaDaAttivitaModificate();
            if (!contattiDaAttivita.isEmpty()) {
                contattiAggiornati.addAll(contattiDaAttivita);
                for (Long contactId : contattiDaAttivita) {
                    LeadRequest req = new LeadRequest();
                    req.setContactId(contactId);
                    req.setWorkedCode("AUTO_FROM_SCHEDULER");
                    req.setCaller("AUTO_SCHEDULER");
                    req.setResultCode(ResultCode.D102); // Default solo se non c'è altro
                    req.setWorkedType("O");
                    req.setWorked_Date(LocalDateTime.now());
                    req.setWorked_End_Date(LocalDateTime.now().plusMinutes(2));
                    contattiInAttesa.add(req);
                    logger.info("🟡 Contatto {} aggiunto da attività modificata", contactId);
                    sleepSafe(300);
                }
            }

            // 3️⃣ Log finale
            logger.info("✅ Totale contatti aggiornati: {}", contattiAggiornati.size());
            logger.info("✅ Totale contatti in attesa: {}", contattiInAttesa.size());

        } catch (Exception e) {
            logger.error("❌ Errore durante controllo periodico modifiche contatti/attività", e);
        } finally {
            inEsecuzione = false;
            logger.info("🏁 Controllo completato.");
        }
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



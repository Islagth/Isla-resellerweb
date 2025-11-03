package com.example.enel_bitrix24_integration.config;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.service.BitrixService;
import com.example.enel_bitrix24_integration.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LeadScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeadScheduler.class);

    private final BitrixService bitrixService;
    private final ContactService contactService;

    // Lista thread-safe in memoria
    private final List<LeadRequest> contattiInAttesa = new CopyOnWriteArrayList<>();

    public LeadScheduler(BitrixService bitrixService, ContactService contactService) {
        this.bitrixService = bitrixService;
        this.contactService = contactService;
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
    @Scheduled(cron = "0 */15 * * * ?")
    public void controllaModificheContatti() {
        try {
            List<LeadRequest> contattiModificati = contactService.trovaContattiModificati();
            if (contattiModificati.isEmpty()) {
                logger.info("⏳ Nessun contatto modificato nell’ultimo ciclo.");
                return;
            }

            contattiInAttesa.addAll(contattiModificati);
            logger.info("🟢 Aggiunti {} contatti modificati alla lista di invio.", contattiModificati.size());
        } catch (Exception e) {
            logger.error("❌ Errore durante il controllo dei contatti modificati", e);
        }
    }

    /**
     * 📤 Ogni ora invia i contatti accumulati verso Bitrix24
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

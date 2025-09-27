package com.example.enel_bitrix24_integration.config;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.service.BitrixService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LeadScheduler {

    private final BitrixService bitrixService;

    // Lista in memoria (non DB)
    private final List<LeadRequest> contattiInAttesa = new ArrayList<>();

    public LeadScheduler(BitrixService bitrixService) {
        this.bitrixService = bitrixService;
    }

    // Metodo richiamato dal controller per aggiungere contatti
    public void aggiungiContatto(LeadRequest request) {
        contattiInAttesa.add(request);
    }

    /**
     * Eseguito ogni giorno alle 12:00 e 18:00
     */
    @Scheduled(cron = "0 0 12,18 * * ?")
    public void invioMultiploContatti() {
        if (contattiInAttesa.isEmpty()) {
            System.out.println("Nessun contatto da inviare alle 12/18");
            return;
        }

        for (LeadRequest request : new ArrayList<>(contattiInAttesa)) {
            LeadResponse response = bitrixService.invioLavorato(request);

            if (response.isSuccess()) {
                System.out.println("Contatto " + request.getWorkedCode() + " inviato correttamente");
            } else {
                System.err.println("Errore invio contatto " + request.getWorkedCode() + ": " + response.getMessage());
            }
        }

        // Svuota la lista dopo l'invio
        contattiInAttesa.clear();
    }

}
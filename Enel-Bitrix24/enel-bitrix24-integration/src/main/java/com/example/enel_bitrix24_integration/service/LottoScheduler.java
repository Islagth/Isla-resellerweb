package com.example.enel_bitrix24_integration.service;
import com.example.enel_bitrix24_integration.dto.CampaignDTO;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.example.enel_bitrix24_integration.dto.SliceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LottoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LottoScheduler.class);

    private final LottoService lottoService;
    private final ContactService contactService;
    private final DealService dealService;

 

    public LottoScheduler(LottoService lottoService,
                          ContactService contactService,
                          DealService dealService) {
        this.lottoService = lottoService;
        this.contactService = contactService;
        this.dealService = dealService;
    }

        @Scheduled(fixedRate = 60000)
    public void processaTuttiILotti() {
        logger.info("Avvio scheduler: elaborazione automatica lotti in corso...");

        try {
            // 1️⃣ Recupera tutte le campagne disponibili
            List<CampaignDTO> campaigns = lottoService.getCampaigns();
            if (campaigns == null || campaigns.isEmpty()) {
                logger.info("Nessuna campagna disponibile per la generazione dei lotti.");
                return;
            }

            // 2️⃣ Per ogni campagna, richiede un lotto e lo processa
            for (CampaignDTO campaign : campaigns) {
                int idCampagna = campaign.getId_campagna();  // o getId() a seconda del DTO
                logger.info("Elaborazione campagna ID={}", idCampagna);

                try {
                    // Richiede lotto per la campagna
                    SliceResponse slice = lottoService.requestLotto(idCampagna, 1); // ad es. 100 record per lotto
                    if (slice == null || slice.getId_lotto() <= 0) {
                        logger.warn("Nessun lotto generato per la campagna ID={}", idCampagna);
                        continue;
                    }

                    String idLotto = String.valueOf(slice.getId_lotto());
                    logger.info("Lotto generato (ID={}) per campagna ID={}", idLotto, idCampagna);

                    // 3️⃣ Scarica il lotto in formato JSON
                    String json = lottoService.scaricaLottoJson(idLotto);

                    // 4️⃣ Elabora i dati del lotto
                    contactService.creaContattiDaLotto(idLotto, json);
                    dealService.creaDealDaLotto(idLotto, json);

                    logger.info("✅ Lotto ID={} elaborato correttamente", idLotto);

                } catch (Exception e) {
                    logger.error("Errore nella lavorazione della campagna ID={}: {}", idCampagna, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Errore generale nel flusso automatico: {}", e.getMessage(), e);
        }

        logger.info("✅ Scheduler completato: elaborazione lotti terminata.");
    }
    

        /*
        @Scheduled(fixedRate = 60000)
        public void processaTuttiILotti() {
            try {
                List<LottoDTO> lottiDisponibili = lottoService.verificaLottiDisponibili();
                if (lottiDisponibili == null || lottiDisponibili.isEmpty()) return;
        
                for (LottoDTO lotto : lottiDisponibili) {
                    String idLotto = lotto.getId_lotto();
                    try {
                        String json = lottoService.scaricaLottoJson(idLotto);
                        contactService.creaContattiDaLotto(idLotto, json);
                        dealService.creaDealDaLotto(idLotto, json);
                    } catch (Exception e) {
                        logger.error("Errore nella lavorazione del lotto {}: {}", idLotto, e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("Errore generale nel flusso automatico: {}", e.getMessage(), e);
            }
        } */
}



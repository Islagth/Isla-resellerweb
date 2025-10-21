package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.LottoDTO;
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

    private final String accessToken = null; // oppure leggere da config se serve

    public LottoScheduler(LottoService lottoService,
                          ContactService contactService,
                          DealService dealService) {
        this.lottoService = lottoService;
        this.contactService = contactService;
        this.dealService = dealService;
    }

    @Scheduled(fixedRate = 60000) // ogni 60 secondi
    public void processaTuttiILotti() {
        try {
            logger.info("=== Avvio flusso automatico: verifica e lavorazione lotti ===");

            // Recupera tutti i lotti disponibili dal LottoService
            List<LottoDTO> lottiDisponibili = lottoService.verificaLottiDisponibili();

            if (lottiDisponibili == null || lottiDisponibili.isEmpty()) {
                logger.info("Nessun lotto disponibile al momento.");
                return;
            }

            // Per ogni lotto, scarica JSON e processa contatti e deal
            for (LottoDTO lotto : lottiDisponibili) {
                String idLotto = lotto.getId_lotto();
                try {
                    // Scarica JSON singolo lotto
                    String json = lottoService.scaricaLottoJson(idLotto);

                    // Creazione contatti dal lotto
                    contactService.creaContattiDaLotto(idLotto, json, accessToken);

                    // Creazione deal dal lotto
                    dealService.creaDealDaLotto(idLotto, json, accessToken);

                    logger.info("Lavorazione completata per lotto id: {}", idLotto);

                } catch (Exception e) {
                    logger.error("Errore nella lavorazione del lotto {}: {}", idLotto, e.getMessage(), e);
                }
            }

            logger.info("=== Flusso automatico completato ===");

        } catch (Exception e) {
            logger.error("Errore generale nel flusso automatico: {}", e.getMessage(), e);
        }
    }
}
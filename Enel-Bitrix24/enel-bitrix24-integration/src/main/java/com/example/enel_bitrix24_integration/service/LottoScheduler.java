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

        @Scheduled(fixedRate = 60000)
        public void processaTuttiILotti() {
            try {
                List<LottoDTO> lottiDisponibili = lottoService.verificaLottiDisponibili();
                if (lottiDisponibili == null || lottiDisponibili.isEmpty()) return;
        
                for (LottoDTO lotto : lottiDisponibili) {
                    String idLotto = lotto.getId_lotto();
                    try {
                        String json = lottoService.scaricaLottoJson(idLotto);
                        contactService.creaContattiDaLotto(idLotto, json, null);
                        dealService.creaDealDaLotto(idLotto, json, null);
                    } catch (Exception e) {
                        logger.error("Errore nella lavorazione del lotto {}: {}", idLotto, e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("Errore generale nel flusso automatico: {}", e.getMessage(), e);
            }
        }
}

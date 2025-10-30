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

                    // 1️⃣ Crea contatti e ottieni ID
                    List<Integer> contactIds = contactService.creaContattiDaLotto(idLotto, json);

                    // 2️⃣ Crea deal e ottieni ID
                    List<Integer> dealIds = dealService.creaDealDaLotto(idLotto, json);

                    // 3️⃣ Collega contatti e deal
                    for (Integer dealId : dealIds) {
                        for (Integer contactId : contactIds) {
                            contactService.linkContactToDeal(dealId, contactId);
                        }
                    }

                    logger.info("Lotto {} elaborato correttamente. Creati {} contatti e {} deal.",
                            idLotto, contactIds.size(), dealIds.size());

                } catch (Exception e) {
                    logger.error("Errore nella lavorazione del lotto {}: {}", idLotto, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Errore generale nel flusso automatico: {}", e.getMessage(), e);
        }
    }


}





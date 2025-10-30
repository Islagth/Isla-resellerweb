package com.example.enel_bitrix24_integration.service;

import com.example.enel_bitrix24_integration.dto.CampaignDTO;
import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.example.enel_bitrix24_integration.dto.SliceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
            // Recupera i lotti disponibili
            List<LottoDTO> lottiDisponibili = lottoService.verificaLottiDisponibili();
            if (lottiDisponibili == null || lottiDisponibili.isEmpty()) {
                logger.info("Nessun lotto disponibile da processare.");
                return;
            }

            for (LottoDTO lotto : lottiDisponibili) {
                String idLotto = lotto.getId_lotto();
                try {
                    // Scarica il lotto in formato JSON
                    String json = lottoService.scaricaLottoJson(idLotto);

                    // 1️⃣ Crea contatti e ottieni ID (sicuro da null)
                    List<Integer> contactIds = Optional.ofNullable(contactService.creaContattiDaLotto(idLotto, json))
                            .orElse(Collections.emptyList());

                    // 2️⃣ Crea deal e ottieni ID (sicuro da null)
                    List<Integer> dealIds = Optional.ofNullable(dealService.creaDealDaLotto(idLotto, json))
                            .orElse(Collections.emptyList());

                    // 3️⃣ Collega contatti e deal solo se entrambe le liste non sono vuote
                    if (!dealIds.isEmpty() && !contactIds.isEmpty()) {
                        for (Integer dealId : dealIds) {
                            for (Integer contactId : contactIds) {
                                contactService.linkContactToDeal(dealId, contactId);
                            }
                        }
                    } else {
                        logger.warn("Lotto {}: nessun deal o contatto da collegare.", idLotto);
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






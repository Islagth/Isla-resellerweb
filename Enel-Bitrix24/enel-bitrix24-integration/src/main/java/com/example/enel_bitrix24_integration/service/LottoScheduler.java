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
import java.util.Map;
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


    @@Scheduled(fixedRate = 60000)
    public void processaTuttiILotti() {
        try {
            List<LottoDTO> lottiDisponibili = lottoService.verificaLottiDisponibili();
            if (lottiDisponibili == null || lottiDisponibili.isEmpty()) {
                logger.info("⏳ Nessun lotto disponibile da processare.");
                return;
            }

            for (LottoDTO lotto : lottiDisponibili) {
                String idLotto = lotto.getId_lotto();
                logger.info("🚀 Inizio elaborazione lotto {}", idLotto);

                try {
                    String json = lottoService.scaricaLottoJson(idLotto);
                    logger.debug("📥 JSON ricevuto per lotto {}: {}", idLotto, json);

                    Map<String, Integer> contactMap = Optional.ofNullable(
                            contactService.creaContattiDaLotto(idLotto, json)
                    ).orElse(Collections.emptyMap());

                    Map<String, Integer> dealMap = Optional.ofNullable(
                            dealService.creaDealDaLotto(idLotto, json)
                    ).orElse(Collections.emptyMap());

                    int collegamentiEffettuati = 0;
                    for (Map.Entry<String, Integer> entry : dealMap.entrySet()) {
                        String idAnagrafica = entry.getKey();
                        Integer dealId = entry.getValue();

                        Integer contactId = contactMap.get(idAnagrafica);
                        if (contactId != null) {
                            dealService.linkContactToDeal(dealId, contactId);
                            collegamentiEffettuati++;
                            logger.info("🔗 Collegato contatto {} → deal {} (anagrafica: {})",
                                    contactId, dealId, idAnagrafica);
                        } else {
                            logger.warn("⚠️ Nessun contatto trovato per anagrafica {} (deal ID: {})",
                                    idAnagrafica, dealId);
                        }
                    }

                    logger.info("✅ Lotto {} elaborato: {} contatti, {} deal, {} collegamenti.",
                            idLotto, contactMap.size(), dealMap.size(), collegamentiEffettuati);

                } catch (Exception e) {
                    logger.error("❌ Errore nella lavorazione del lotto {}: {}", idLotto, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("🔥 Errore generale nel flusso automatico: {}", e.getMessage(), e);
        }
    }






}









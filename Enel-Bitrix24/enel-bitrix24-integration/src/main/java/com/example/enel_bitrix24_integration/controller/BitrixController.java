package com.example.enel_bitrix24_integration.controller;


import com.example.enel_bitrix24_integration.dto.DealDTO;
import com.example.enel_bitrix24_integration.service.ContactService;
import com.example.enel_bitrix24_integration.service.DealService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class BitrixController {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final DealService dealService;
    private final ContactService contactService;



  
    // Aggiungi Deal
    @PostMapping("/api/enel-leads/add-Deal")
    public ResponseEntity<?> createDeal(@RequestBody DealDTO dealDTO,
                                        @RequestParam(required = false) Map<String, Object> params) {
        logger.info("Ricevuta richiesta createDeal con dati: {}", dealDTO);
    
        try {
            Integer dealId = dealService.addDeal(dealDTO, params);
            logger.info("✅ Deal creato con ID: {}", dealId);
            return ResponseEntity.ok(Map.of("dealId", dealId));
        } catch (Exception e) {
            logger.error("❌ Errore durante la creazione del deal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    // Modifica Deal
    @PutMapping("/api/enel-leads/update-Deal")
    public ResponseEntity<?> updateDeal(@RequestBody DealDTO dto,
                                        @RequestParam(required = false) Map<String, Object> params) {
        logger.info("Ricevuta richiesta updateDeal per ID: {}", dto.getId());
        boolean success = dealService.updateDeal(dto, params);
        logger.info("Aggiornamento deal {} risultato: {}", dto.getId(), success);
        return ResponseEntity.ok(Map.of("updated", success));
    }

    // Cerca Deal per id
   @GetMapping("/api/enel-leads/get-deal/{id}")
    public ResponseEntity<?> getDealById(@PathVariable Integer id) {
        logger.info("Ricevuta richiesta getDealById per ID: {}", id);
        DealDTO dealDTO = dealService.getDealById(id);
        return ResponseEntity.ok(dealDTO);
    }

    // Ottieni tutta la lista dei Deal
    @PostMapping("/api/enel-leads/get-deals-list")
    public ResponseEntity<?> getDealsList(@RequestBody Map<String, Object> body) {
        logger.info("Ricevuta richiesta getDealsList");
        List<String> select = (List<String>) body.get("select");
        Map<String, Object> filter = (Map<String, Object>) body.get("filter");
        Map<String, String> order = (Map<String, String>) body.get("order");
        Integer start = (Integer) body.getOrDefault("start", 0);
        List<DealDTO> deals = dealService.getDealsList(select, filter, order, start).getDeals();
        logger.info("Recuperati {} deals", deals.size());
        return ResponseEntity.ok(deals);
    }

    // Cancella Deal
    @DeleteMapping("/api/enel-leads/delete-deal/{id}")
    public ResponseEntity<?> deleteDeal(@PathVariable Integer id) {
        logger.info("Ricevuta richiesta deleteDeal per ID: {}", id);
        boolean deleted = dealService.deleteDeal(id);
        logger.info("Eliminazione deal ID {} risultato: {}", id, deleted);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    // Aggiungi Contatto da lista Json
    @PostMapping("/api/enel-leads/{idLotto}/add-contact")
    public ResponseEntity<?> creaContattiDalLotto(@PathVariable String idLotto, @RequestBody String json) {
        logger.info("Ricevuta richiesta creaContattiDalLotto per lotto id: {}", idLotto);
        try {
            contactService.creaContattiDaLotto(idLotto, json);
            logger.info("Creazione contatti da lotto {} avviata con successo", idLotto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Creazione contatti avviata con successo"));
        } catch (Exception e) {
            logger.error("Errore nella creazione dei contatti da lotto " + idLotto, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Errore durante la creazione dei contatti: " + e.getMessage()));
        }
    }

    // Modifica contatto
    @PutMapping("/api/enel-leads/update-contact")
    public ResponseEntity<?> aggiornaContatto(@RequestParam int id,
                                              @RequestBody Map<String, Object> payload) {
        logger.info("Ricevuta richiesta aggiornaContatto per id: {}", id);
        try {
            Map<String, Object> fields = (Map<String, Object>) payload.get("fields");
            Map<String, Object> params = (Map<String, Object>) payload.getOrDefault("params", Collections.emptyMap());
            String result = contactService.aggiornaContatto(id, fields, params);
            logger.info("Aggiornamento contatto ID {} risultato: {}", id, result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Errore aggiornamento contatto ID " + id, e);
            return buildErrorResponse("Errore aggiornamento contatto", e);
        }
    }

    // Cerca Contatto tramite Id
     @GetMapping("/api/enel-leads/get-contact/{id}")
    public ResponseEntity<?> getContactoById(@PathVariable int id) {
        logger.info("Ricevuta richiesta getContattoById per id: {}", id);
        try {
            Map<String, Object> contact =contactService.getContattoById(id).getUF();
            logger.info("Recuperato contatto ID {}", id);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            logger.error("Errore recupero contatto ID " + id, e);
            return buildErrorResponse("Errore recupero contatto", e);
        }
    }

    // Ottieni lista dei contatti
    @PostMapping("/api/enel-leads/lista-contatti")
    public ResponseEntity<?> listaContatti(@RequestBody Map<String, Object> requestBody) {
        logger.info("Ricevuta richiesta listaContatti");
        try {
            Map<String, Object> filter = (Map<String, Object>) requestBody.get("filter");
            Map<String, String> order = (Map<String, String>) requestBody.get("order");
            List<String> select = (List<String>) requestBody.get("select");
            Integer start = requestBody.get("start") != null ? (Integer) requestBody.get("start") : null;
            Map<String, Object> result = contactService.listaContatti(filter, order, select, start);
            logger.info("Lista contatti recuperata con successo");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Errore recupero lista contatti", e);
            return buildErrorResponse("Errore recupero lista contatti", e);
        }
    }

    // Cancellazione contatto
   @DeleteMapping("/api/enel-leads/delete-contact/{id}")
    public ResponseEntity<?> eliminaContatto(@PathVariable int id,String phone) {
        logger.info("Ricevuta richiesta eliminaContatto per id: {}", id);
        try {
            boolean deleted = contactService.eliminaContatto(id,phone);
            logger.info("Eliminazione contatto ID {} risultato: {}", id, deleted);
            if (deleted) {
                return ResponseEntity.ok("Contatto eliminato con successo.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Impossibile eliminare il contatto.");
            }
        } catch (Exception e) {
            logger.error("Errore eliminazione contatto ID " + id, e);
            return buildErrorResponse("Errore eliminazione contatto", e);
        }
    }


    private ResponseEntity<String> buildErrorResponse(String message, Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message + ": " + e.getMessage());
    }

}

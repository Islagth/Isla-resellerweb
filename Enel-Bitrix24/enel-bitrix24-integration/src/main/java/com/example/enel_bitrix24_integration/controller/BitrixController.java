package com.example.enel_bitrix24_integration.controller;

import com.example.enel_bitrix24_integration.config.BitrixOAuthProperties;
import com.example.enel_bitrix24_integration.config.TokenResponse;
import com.example.enel_bitrix24_integration.dto.DealDTO;
import com.example.enel_bitrix24_integration.config.TokenRecord;
import com.example.enel_bitrix24_integration.security.OAuthService;
import com.example.enel_bitrix24_integration.service.ContactService;
import com.example.enel_bitrix24_integration.service.DealService;
import com.example.enel_bitrix24_integration.security.TokenStorageService;
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
    private final OAuthService oAuthService;
    private final TokenRecord tokenRecord;
    private final TokenStorageService tokenStorageService;
    private final BitrixOAuthProperties oAuthProperties;

    //Aggiungi Deal
    @PostMapping("/api/enel-leads/add-Deal")
    public ResponseEntity<?> createDeal(@RequestBody DealDTO dealDTO,
                                        @RequestParam(required = false) Map<String, Object> params) {
        logger.info("Ricevuta richiesta createDeal con dati: {}", dealDTO);
        Integer dealId = dealService.addDeal(dealDTO, params, null);
        logger.info("Deal creato con ID: {}", dealId);
        return ResponseEntity.ok(Map.of("dealId", dealId));
    }

    //Modifica Deal
    @PutMapping("/api/enel-leads/update-Deal")
    public ResponseEntity<?> updateDeal(@RequestBody DealDTO dto,
                                        @RequestParam(required = false) Map<String, Object> params) {
        logger.info("Ricevuta richiesta updateDeal per ID: {}", dto.getId());
        boolean success = dealService.updateDeal(dto, params, null);
        logger.info("Aggiornamento deal {} risultato: {}", dto.getId(), success);
        return ResponseEntity.ok(Map.of("updated", success));
    }

    //Cerca Deal per id
   @GetMapping(params = "id")
    public ResponseEntity<?> getDealById(@RequestParam Integer id) {
        logger.info("Ricevuta richiesta getDealById per ID: {}", id);
        DealDTO dealDTO = dealService.getDealById(id, null);
        return ResponseEntity.ok(dealDTO);
    }

    //Ottieni tutta la lista dei Deal
     @GetMapping
    public ResponseEntity<?> getDealsList(
            @RequestParam(required = false) List<String> select,
            @RequestParam(required = false) Map<String, Object> filter,
            @RequestParam(required = false) Map<String, String> order,
            @RequestParam(defaultValue = "0") Integer start
    ) { {
        logger.info("Ricevuta richiesta getDealsList");
        List<String> select = (List<String>) body.get("select");
        Map<String, Object> filter = (Map<String, Object>) body.get("filter");
        Map<String, String> order = (Map<String, String>) body.get("order");
        Integer start = (Integer) body.getOrDefault("start", 0);
        List<DealDTO> deals = dealService.getDealsList(select, filter, order, start, null);
        logger.info("Recuperati {} deals", deals.size());
        return ResponseEntity.ok(deals);
    }

    //Cancella Deal
    @DeleteMapping("/api/enel-leads/delete-deal/{id}")
    public ResponseEntity<?> deleteDeal(@PathVariable Integer id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Ricevuta richiesta deleteDeal per ID: {}", id);
        if (!isAuthorized(authHeader)) {
            logger.warn("Tentativo di accesso non autorizzato in deleteDeal");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Token non valido o mancante"));
        }
        String token = extractAccessToken(authHeader);

        boolean deleted = dealService.deleteDeal(id, token);
        logger.info("Eliminazione deal ID {} risultato: {}", id, deleted);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    //Aggiungi Contatto da lista Json
    @PostMapping("/api/enel-leads/{idLotto}/add-contact")
    public ResponseEntity<?> creaContattiDalLotto(@PathVariable String idLotto) {
        logger.info("Ricevuta richiesta creaContattiDalLotto per lotto id: {}", idLotto);
        try {
            contactService.creaContattiDaLotto(idLotto, null);
            logger.info("Creazione contatti da lotto {} avviata con successo", idLotto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Creazione contatti avviata con successo"));
        } catch (Exception e) {
            logger.error("Errore nella creazione dei contatti da lotto " + idLotto, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Errore durante la creazione dei contatti: " + e.getMessage()));
        }
    }


    //Modifica contatto
    @PutMapping("/api/enel-leads/update-contact")
    public ResponseEntity<?> aggiornaContatto(@RequestParam int id,
                                              @RequestBody Map<String, Object> payload) {
        logger.info("Ricevuta richiesta aggiornaContatto per id: {}", id);
        try {
            Map<String, Object> fields = (Map<String, Object>) payload.get("fields");
            Map<String, Object> params = (Map<String, Object>) payload.getOrDefault("params", Collections.emptyMap());
            String result = contactService.aggiornaContatto(id, fields, params, null);
            logger.info("Aggiornamento contatto ID {} risultato: {}", id, result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Errore aggiornamento contatto ID " + id, e);
            return buildErrorResponse("Errore aggiornamento contatto", e);
        }
    }


    //Cerca Contatto tramite Id
    @GetMapping
    public ResponseEntity<?> getContattoById(@PathVariable int id) {
        logger.info("Ricevuta richiesta getContattoById per id: {}", id);
        try {
            Map<String, Object> contact = contactService.getContattoById(id, null);
            logger.info("Recuperato contatto ID {}", id);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            logger.error("Errore recupero contatto ID " + id, e);
            return buildErrorResponse("Errore recupero contatto", e);
        }
    }


    //Ottieni lista dei contatti
    @GetMapping
    public ResponseEntity<?> listaContatti(@RequestBody Map<String, Object> requestBody) {
        logger.info("Ricevuta richiesta listaContatti");
        try {
            Map<String, Object> filter = (Map<String, Object>) requestBody.get("filter");
            Map<String, String> order = (Map<String, String>) requestBody.get("order");
            List<String> select = (List<String>) requestBody.get("select");
            Integer start = requestBody.get("start") != null ? (Integer) requestBody.get("start") : null;
            Map<String, Object> result = contactService.listaContatti(filter, order, select, start, null);
            logger.info("Lista contatti recuperata con successo");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Errore recupero lista contatti", e);
            return buildErrorResponse("Errore recupero lista contatti", e);
        }
    }

    //Cancellazione contatto
    @DeleteMapping("/api/enel-leads/delete-contact/{id}")
    public ResponseEntity<?> eliminaContatto(@PathVariable int id) {
        logger.info("Ricevuta richiesta eliminaContatto per id: {}", id);
        try {
            boolean deleted = contactService.eliminaContatto(id, null);
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
        // Log dettagliato (omesso qui ma consigliato)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message + ": " + e.getMessage());
    }


    @GetMapping("/api/enel-leads/oauth/authorize")
    public void redirectToBitrixOAuth(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        String authUrl = UriComponentsBuilder.fromHttpUrl("https://portal.bitrix24.com/oauth/authorize/")
                .queryParam("client_id", oAuthProperties.getClientId())
                .queryParam("redirect_uri", oAuthProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();
        logger.info("Redirect all'endpoint OAuth di Bitrix24 con stato: {}", state);
        response.sendRedirect(authUrl);
    }

    @GetMapping("/api/enel-leads/oauth/callback")
    public ResponseEntity<?> handleOAuthCallback(@RequestParam String code, @RequestParam String state) {
        logger.info("Ricevuto callback OAuth con code: {} e state: {}", code, state);
        try {
            TokenResponse tokens = oAuthService.getTokens(code,
                    oAuthProperties.getClientId(),
                    oAuthProperties.getClientSecret(),
                    oAuthProperties.getRedirectUri());

            tokenStorageService.saveTokens(tokens);
            logger.info("Token OAuth salvati con successo, accessToken scadenza in {} secondi", tokens.getExpires_in());

            return ResponseEntity.ok(Map.of(
                    "accessToken", tokens.getAccess_token(),
                    "refreshToken", tokens.getRefresh_token(),
                    "expiresIn", tokens.getExpires_in()
            ));
        } catch (Exception e) {
            logger.error("Errore ottenimento token OAuth con code: {}", code, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore ottenimento token OAuth", "message", e.getMessage()));
        }
    }

    // Metodo per autorizzazione con refresh automatico
    public boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Autorizzazione fallita: header Authorization mancante o malformato");
            return false;
        }

        String accessToken = authHeader.substring(7);
        TokenRecord record = tokenStorageService.findByAccessToken(accessToken);
        if (record == null) {
            logger.warn("Autorizzazione fallita: accessToken non trovato");
            return false;
        }

        if (System.currentTimeMillis() > record.getExpiryTime()) { // token scaduto
            logger.info("AccessToken scaduto, avvio refresh token");
            try {
                TokenResponse refreshed = oAuthService.refreshTokens(
                        record.getRefreshToken(),
                        oAuthProperties.getClientId(),
                        oAuthProperties.getClientSecret()
                );
                tokenStorageService.saveTokens(refreshed);
                logger.info("Refresh token eseguito con successo");
                return true;
            } catch (Exception e) {
                logger.error("Errore nel refresh token OAuth", e);
                return false;
            }
        }
        return true;
    }
    private String extractAccessToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean tokenExpired(TokenRecord record) {
        // Confronta l’ora attuale con la scadenza memorizzata
        return System.currentTimeMillis() > record.getExpiryTime();
    }




}

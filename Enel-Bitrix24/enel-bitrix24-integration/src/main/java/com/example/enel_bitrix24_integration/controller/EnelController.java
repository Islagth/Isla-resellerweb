package com.example.enel_bitrix24_integration.controller;
import com.example.enel_bitrix24_integration.config.EnelProperties;
import com.example.enel_bitrix24_integration.config.LeadScheduler;
import com.example.enel_bitrix24_integration.dto.*;
import com.example.enel_bitrix24_integration.service.BitrixService;
import com.example.enel_bitrix24_integration.service.BlacklistService;
import com.example.enel_bitrix24_integration.service.LottoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enel-leads")
@RequiredArgsConstructor
public class EnelController {

    private static final Logger logger = LoggerFactory.getLogger(EnelController.class);

    private final LottoService lottoService;
    private final BlacklistService blacklistService;
    private final BitrixService bitrixService;
    private final EnelProperties enelProperties;
    private final LeadScheduler leadScheduler;

    @Value("${webhook.api-key}")
    private String expectedApiKey;

    private boolean isAuthorized(String authHeader, String apiKey) {
        boolean validApiKey = apiKey != null && apiKey.equals(expectedApiKey);
        boolean validBearer = authHeader != null && authHeader.startsWith("Bearer ") && authHeader.substring(7).equals(enelProperties.getClientJwt());
        return validApiKey || validBearer;
    }


    //Crea contatto lavorato
    @PostMapping
    public ResponseEntity<?> creaContattoLavorato(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey,
            @RequestBody LeadRequest request) {
        logger.info("Ricevuta richiesta creaContattoLavorato per lead: {}", request);
        if (!isAuthorized(authHeader, apiKey)) {
            logger.warn("Accesso non autorizzato: token o API-Key mancanti/non validi.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi o mancanti"));
        }
        try {
            LeadResponse response = bitrixService.invioLavorato(request);
            logger.info("Invocato bitrixService.invioLavorato, risultato successo: {}", response.isSuccess());
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Errore durante la chiamata creaContattoLavorato", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    //Aggiunta contatto lavorato alla lista per l'invio
    @PostMapping("/aggiungi")
    public ResponseEntity<String> aggiungiContatto(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey,
            @RequestBody LeadRequest request) {
        if (!isAuthorized(authHeader, apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accesso non autorizzato.");
        }
        logger.info("Aggiunta contatto alla coda di invio schedulato: {}", request);
        leadScheduler.aggiungiContatto(request);
        return ResponseEntity.ok("Contatto aggiunto in coda per l'invio schedulato");
    }

    //Richiesta lista lotti da scaricare
    @GetMapping("/ultimi")
    public ResponseEntity<?> getUltimiLotti(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey) {
        if (!isAuthorized(authHeader, apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi"));
        }
        List<LottoDTO> lotti = lottoService.verificaLottiDisponibili();
        if (lotti.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lotti);
    }


    // Scarica lotto JSON → restituisce il contenuto
    @GetMapping("/scarica-json")
    public ResponseEntity<?> scaricaJson(
            @PathVariable String idLotto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey) {

        logger.info("Ricevuta richiesta scaricaJson per lotto id: {}", idLotto);

        if (!isAuthorized(authHeader, apiKey)) {
            logger.warn("Accesso non autorizzato: token o API-Key mancanti/non validi.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi o mancanti"));
        }

        try {
            String json = lottoService.scaricaLottoJson(idLotto);
            logger.info("scaricaJson completato con successo per lotto id: {}", idLotto);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (Exception e) {
            String errMsg = e.getMessage();
            logger.error("Errore scaricaJson per lotto id {}: {}", idLotto, errMsg);
            if ("Slice Id not found".equals(errMsg)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", errMsg));
            } else if ("Slice Id not available".equals(errMsg)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", errMsg));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Errore interno"));
            }
        }
    }



    // Scarica lotto ZIP → restituisce il file binario
    @GetMapping("/scarica-zip")
    public ResponseEntity<?> scaricaZip(
            @PathVariable String idLotto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey) {
        logger.info("Ricevuta richiesta scaricaZip per lotto id: {}", idLotto);

        if (!isAuthorized(authHeader, apiKey)) {
            logger.warn("Accesso non autorizzato: token o API-Key mancanti/non validi.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi o mancanti"));
        }

        try {
            byte[] zipData = lottoService.scaricaLottoZip(idLotto);
            logger.info("scaricaZip completato con successo per lotto id: {}", idLotto);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotto_" + idLotto + ".zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipData);

        } catch (RuntimeException e) {
            String message = e.getMessage();
            logger.error("Errore runtime download ZIP lotto {}: {}", idLotto, message, e);
            if ("Slice Id not found".equals(message)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("false", "Slice Id not found"));
            } else if ("Slice Id not available".equals(message)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("false", "Slice Id not available"));
            } else {
                return ResponseEntity.internalServerError()
                        .body(new ErrorResponse("false", "Errore download ZIP lotto " + idLotto + ": " + message));
            }
        } catch (Exception e) {
            logger.error("Errore generico download ZIP lotto {}: {}", idLotto, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("false", "Errore generico download ZIP lotto " + idLotto));
        }
    }
    //Richiesta lista lotti blacklist da scaricare
    @PostMapping("/ultimiBlacklist")
    public ResponseEntity<?> getUltimiLottiBlacklist(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey) {
        logger.info("Ricevuta richiesta lista ultimi lotti blacklist.");

        if (!isAuthorized(authHeader, apiKey)) {
            logger.warn("Accesso non autorizzato: token o API-Key mancanti/non validi.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi o mancanti"));
        }

        List<LottoBlacklistDTO> lottiB = blacklistService.verificaBlacklistDisponibili();
        logger.info("Numero lotti blacklist disponibili: {}", lottiB.size());

        if (lottiB.isEmpty()) {
            logger.info("Nessun lotto blacklist disponibile da restituire.");
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lottiB);
    }

    // Scarica lotto blacklist ZIP → restituisce il file binario
    @GetMapping("blacklist/{idLotto}/blacklist-zip ")
    public ResponseEntity<?> scaricaZipBlacklist(
            @PathVariable Long idLotto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey) {
        logger.info("Ricevuta richiesta scaricaZipBlacklist per lotto blacklist id: {}", idLotto);

        if (!isAuthorized(authHeader, apiKey)) {
            logger.warn("Accesso non autorizzato: token o API-Key mancanti/non validi.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi o mancanti"));
        }

        try {
            byte[] zipDataBlacklist = blacklistService.scaricaLottoBlacklistZip(idLotto);
            logger.info("scaricaZipBlacklist completato con successo per lotto blacklist id: {}", idLotto);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotto_blacklist" + idLotto + ".zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipDataBlacklist);

        } catch (RuntimeException e) {
            String message = e.getMessage();
            logger.error("Errore runtime download ZIP lotto blacklist {}: {}", idLotto, message, e);
            if ("Slice Id not found".equals(message)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("false", "Slice Id not found"));
            } else if ("Slice Id not available".equals(message)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("false", "Slice Id not available"));
            } else {
                return ResponseEntity.internalServerError()
                        .body(new ErrorResponse("false", "Errore download ZIP lotto blacklist " + idLotto + ": " + message));
            }
        } catch (Exception e) {
            logger.error("Errore generico download ZIP lotto blacklist {}: {}", idLotto, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("false", "Errore generico download ZIP lotto blacklist" + idLotto));
        }
    }

    //Invia conferma di processamento di lotto di notifica blacklist
    @PostMapping("/{id}/conferma")
    public ResponseEntity<?> confermaLotto(
            @PathVariable("id") long idLotto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "api-auth-token", required = false) String apiKey) {
        logger.info("Ricevuta richiesta conferma lotto blacklist id: {}", idLotto);

        if (!isAuthorized(authHeader, apiKey)) {
            logger.warn("Accesso non autorizzato: token o API-Key mancanti/non validi.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token o API-Key non validi o mancanti"));
        }

        try {
            blacklistService.confermaLotto(idLotto);
            logger.info("Conferma lotto blacklist id {} completata con successo", idLotto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lotto " + idLotto + " scaricato correttamente."));
        } catch (IllegalArgumentException e) {
            logger.warn("Id Lotto Blacklist non valido: {}", idLotto);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Id Lotto Blacklist non valido"));
        } catch (Exception e) {
            logger.error("Errore durante la conferma del lotto {}: {}", idLotto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Errore durante la conferma del lotto " + idLotto + ": " + e.getMessage()));
        }
    }

}

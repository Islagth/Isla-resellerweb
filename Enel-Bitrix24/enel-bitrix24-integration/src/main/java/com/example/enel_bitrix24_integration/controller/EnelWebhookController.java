package com.example.enel_bitrix24_integration.controller;
import com.example.enel_bitrix24_integration.config.EnelProperties;
import com.example.enel_bitrix24_integration.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.enel_bitrix24_integration.dto.EnelLeadRequest;
import com.example.enel_bitrix24_integration.dto.Bitrix24Response;
import com.example.enel_bitrix24_integration.service.Bitrix24Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enel-leads")
@RequiredArgsConstructor
public class EnelWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(EnelWebhookController.class);

    private final Bitrix24Service bitrix24Service;
    private final EnelProperties enelProperties;

    @PostMapping
    public ResponseEntity<?> receiveLead(
            @Valid @RequestBody EnelLeadRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("Ricevuta nuova richiesta di lead da Enel.");

        try {
            //Validazione del Bearer token tramite metodo dedicato
            if (!isAuthorized(authHeader)) {
                logger.warn("Tentativo di accesso non autorizzato: token mancante o non valido.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("UNAUTHORIZED", "Token non valido o mancante"));
            }

            // Invio del lead a Bitrix24
            Bitrix24Response response = bitrix24Service.createLead(request);
            logger.info("Lead inviato correttamente a Bitrix24");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            logger.warn("Errore di validazione: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));

        } catch (Exception ex) {
            logger.error("Errore interno durante la creazione del lead", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Errore interno al server, riprova più tardi"));
        }
    }

    // Metodo migliorato, separando la validazione dal controllo della stringa
    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        // Verifica il token in modo più robusto (es. confronto su token decodificato o via servizio)
        return token.equals(enelProperties.getClientJwt());
    }
}

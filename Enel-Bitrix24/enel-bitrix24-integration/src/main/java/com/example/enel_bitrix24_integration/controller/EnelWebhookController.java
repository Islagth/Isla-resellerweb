package com.example.enel_bitrix24_integration.controller;
import com.example.enel_bitrix24_integration.config.EnelProperties;
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

        // 🔐 Validazione Bearer token
        if (authHeader == null || !authHeader.equals("Bearer " + enelProperties.getClientJwt())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token non valido o mancante");
        }

        // 👉 Invio del lead a Bitrix24
        Bitrix24Response response = bitrix24Service.createLead(request);

        return ResponseEntity.ok(response);
    }
}

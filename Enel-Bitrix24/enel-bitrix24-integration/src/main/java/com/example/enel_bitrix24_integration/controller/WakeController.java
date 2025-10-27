package com.example.enel_bitrix24_integration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WakeController {

    @GetMapping("/wake")
    public ResponseEntity<String> wakeCheck() {
        return ResponseEntity.ok("SONO ACCESO");
    }
}


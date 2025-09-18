package com.example.enel_bitrix24_integration.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EnelClient {

    private final WebClient webClient;  // Istanza di WebClient per effettuare chiamate HTTP
    private final TokenService tokenService; // Servizio per ottenere il token di accesso


    /**
     * Costruttore con iniezione del TokenService.
     * Inizializza WebClient con l'URL base dell'API Enel.
     */
    public EnelClient(TokenService tokenService) {
        this.tokenService = tokenService;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.enel.com/v1/") // URL base per tutte le chiamate API
                .build();
    }

    /**
     * Metodo per effettuare una chiamata GET all'endpoint specificato.
     * Aggiunge l'intestazione di autorizzazione con token Bearer.
     * @param path Percorso della risorsa relativa all'URL base.
     * @return Il corpo della risposta come stringa (sincrono).
     */
    public String getResource(String path) {
        // Ottiene il token di accesso dal servizio
        String token = tokenService.getAccessToken();
        // Effettua la chiamata GET con il token nel header Authorization
        return webClient.get()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)  // Autenticazione Bearer
                .retrieve()
                .bodyToMono(String.class)  // Converte la risposta in Mono<String>
                .block();  // Blocca per ottenere il risultato in modo sincrono
    }
}


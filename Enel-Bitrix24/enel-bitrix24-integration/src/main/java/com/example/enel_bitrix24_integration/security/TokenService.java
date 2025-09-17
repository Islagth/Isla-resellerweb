package com.example.enel_bitrix24_integration.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    @Value("${enel.api.auth-url}")
    private String authUrl;

    @Value("${enel.api.client-id}")
    private String clientId;

    @Value("${enel.api.client-jwk}")
    private String clientJwk; // contiene l'intero JWK (pubblica + privata)

    private String accessToken;
    private Instant expiryTime;

    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            refreshToken();
        }
        return accessToken;
    }

    private void refreshToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 🔑 Costruisco e firmo il JWT dinamicamente
        String clientAssertion = buildClientAssertion();

        String body = "grant_type=client_credentials" +
                "&client_id=" + clientId +
                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                "&client_assertion=" + clientAssertion +
                "&scope=api.partner"; // ⚠️ scope richiesto dalla doc ENEL

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(authUrl, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> resp = response.getBody();
            this.accessToken = (String) resp.get("access_token");
            int expiresIn = (Integer) resp.get("expires_in");
            this.expiryTime = Instant.now().plusSeconds(expiresIn - 60); // margine di sicurezza
        } else {
            throw new RuntimeException("Errore ottenendo il token Enel: " + response.getStatusCode());
        }
    }

    /**
     * Costruisce e firma un client_assertion JWT come richiesto da ENEL
     */
    private String buildClientAssertion() {
        try {
            // Converte la stringa JWK in oggetto RSAKey
            RSAKey rsaJWK = RSAKey.parse(clientJwk);

            // Firmatario con la private key
            JWSSigner signer = new RSASSASigner(rsaJWK.toPrivateKey());

            Instant now = Instant.now();

            // Claims richiesti
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(clientId)                   // iss = clientId
                    .subject(clientId)                  // sub = clientId
                    .audience(authUrl)                  // aud = URL token endpoint
                    .issueTime(Date.from(now))          // iat
                    .expirationTime(Date.from(now.plusSeconds(300))) // exp (5 minuti)
                    .jwtID(UUID.randomUUID().toString()) // jti
                    .build();

            // Header e firma RS256
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims
            );

            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Errore nella generazione del client_assertion JWT", e);
        }
    }
}

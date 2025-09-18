package com.example.enel_bitrix24_integration.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Value("${enel.api.auth-url}")
    private String authUrl;

    @Value("${enel.api.client-id}")
    private String clientId;

    @Value("${enel.api.client-jwk}")
    private String clientJwk; // Contiene il JWK RSA (pubblica + privata), proteggere adeguatamente

    private String accessToken;
    private Instant expiryTime;

    private final RestTemplate restTemplate;

    // Costruttore con iniezione RestTemplate (meglio che creare ogni volta)
    public TokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Restituisce il token d'accesso, rigenerandolo se scaduto o assente.
     * Metodo sincronizzato per evitare richieste concorrenti di token.
     */
    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            refreshToken();
        }
        return accessToken;
    }

    /**
     * Effettua la chiamata HTTP per ottenere un nuovo access token da Enel.
     */
    private void refreshToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Costruisco e firmo il JWT client assertion dinamicamente
            String clientAssertion = buildClientAssertion();

            // Corpo della richiesta con parametri URL-encoded
            String body = "grant_type=client_credentials" +
                    "&client_id=" + clientId +
                    "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                    "&client_assertion=" + clientAssertion +
                    "&scope=api.partner"; // Scope richiesto da documentazione Enel

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // Chiamata POST al token endpoint
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(authUrl, HttpMethod.POST, request, (Class<Map<String, Object>>) (Class) Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> resp = response.getBody();
                this.accessToken = (String) resp.get("access_token");
                Integer expiresIn = (Integer) resp.get("expires_in");
                if (expiresIn == null) {
                    throw new IllegalStateException("Il campo expires_in è mancante nella risposta.");
                }
                // Imposto scadenza token con margine di sicurezza di 60 secondi
                this.expiryTime = Instant.now().plusSeconds(expiresIn - 60);
                logger.info("Token ottenuto con successo, scadenza tra {} secondi.", expiresIn);
            } else {
                throw new IllegalStateException("Errore ottenendo il token Enel: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Errore durante la richiesta di refresh del token Enel", e);
            throw new RuntimeException("Errore ottenendo il token Enel", e);
        }
    }

    /**
     * Costruisce e firma un client_assertion JWT come richiesto dalla API Enel.
     * @return JWT serializzato come stringa
     */
    private String buildClientAssertion() {
        try {
            // Parsing della stringa JWK RSA in oggetto RSAKey
            RSAKey rsaJWK = RSAKey.parse(clientJwk);

            // Creazione firmatario con chiave privata RSA
            JWSSigner signer = new RSASSASigner(rsaJWK.toPrivateKey());

            Instant now = Instant.now();

            // Costruzione claims richiesti per client assertion
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(clientId)                   // iss = clientId
                    .subject(clientId)                  // sub = clientId
                    .audience(authUrl)                  // aud = URL token endpoint
                    .issueTime(Date.from(now))          // iat = ora corrente
                    .expirationTime(Date.from(now.plusSeconds(300))) // exp = +5 minuti
                    .jwtID(UUID.randomUUID().toString()) // jti = identità unica
                    .build();

            // Header JWT con algoritmo RS256
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims
            );

            // Firma JWT
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            logger.error("Errore nella generazione del client_assertion JWT", e);
            throw new RuntimeException("Errore nella generazione del client_assertion JWT", e);
        }
    }
}

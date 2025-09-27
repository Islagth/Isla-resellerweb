package com.example.enel_bitrix24_integration.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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
    private String clientJwk; // Proteggere opportunamente il JWK

    private volatile String accessToken;

    private volatile Instant expiryTime;

    private RSAKey rsaKey;

    private final RestTemplate restTemplate;

    public TokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            // Parsing del JWK una sola volta per performance e sicurezza
            this.rsaKey = RSAKey.parse(clientJwk);
        } catch (Exception e) {
            logger.error("Errore nel parsing della JWK RSA: ", e);
            throw new IllegalStateException("Configurazione JWK invalida", e);
        }
    }

    /**
     * Restituisce il token di accesso, rigenerandolo se scaduto o assente.
     * Sincronizzato per evitare race condition in ambiente multi-thread.
     */
    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            logger.info("Token assente o scaduto, avvio refresh token.");
            refreshToken();
        } else {
            logger.debug("Token valido ancora disponibile, scadenza tra {} secondi", Duration.between(Instant.now(), expiryTime).getSeconds());
        }
        return accessToken;
    }

    /**
     * Esegue la chiamata HTTP per ottenere un nuovo token di accesso da Enel.
     */
    private void refreshToken() {
        logger.info("Start refresh token: invio richiesta a {}", authUrl);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String clientAssertion = buildClientAssertion();
            logger.debug("JWT client_assertion generato.");

            String body = "grant_type=client_credentials" +
                    "&client_id=" + clientId +
                    "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                    "&client_assertion=" + clientAssertion +
                    "&scope=api.partner";

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(authUrl, HttpMethod.POST, request, (Class<Map<String,Object>>)(Class) Map.class);
            logger.debug("Risposta ottenuta con status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resp = response.getBody();
                String newToken = (String) resp.get("access_token");
                Integer expiresIn = (Integer) resp.get("expires_in");

                if (newToken == null || expiresIn == null) {
                    String msg = "Risposta token incompleta: access_token o expires_in mancante";
                    logger.error(msg);
                    throw new IllegalStateException(msg);
                }

                this.accessToken = newToken;
                this.expiryTime = Instant.now().plusSeconds(expiresIn - 60);
                logger.info("Token ottenuto con successo, scadenza tra {} secondi.", expiresIn);

            } else {
                String msg = "Errore HTTP nella richiesta token: " + response.getStatusCode();
                logger.error(msg);
                throw new IllegalStateException(msg);
            }
        } catch (RestClientException e) {
            logger.error("Errore di comunicazione con il server di autenticazione Enel", e);
            throw new RuntimeException("Errore di comunicazione con il server di autenticazione Enel", e);
        } catch (Exception e) {
            logger.error("Errore imprevisto durante la richiesta di token", e);
            throw new RuntimeException("Errore imprevisto durante la richiesta di token", e);
        }
    }

    /**
     * Costruisce e firma il client_assertion JWT necessario per la richiesta token.
     */
    private String buildClientAssertion() {
        logger.debug("Generazione client_assertion JWT iniziata");
        try {
            JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
            Instant now = Instant.now();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(clientId)
                    .subject(clientId)
                    .audience(authUrl)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(300)))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);

            signedJWT.sign(signer);
            logger.debug("Client_assertion JWT firmato correttamente");
            return signedJWT.serialize();
        } catch (Exception e) {
            logger.error("Errore nella generazione del client_assertion JWT", e);
            throw new RuntimeException("Errore nella generazione del client_assertion JWT", e);
        }
    }
}

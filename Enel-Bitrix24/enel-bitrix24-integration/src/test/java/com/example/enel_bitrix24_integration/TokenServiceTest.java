package com.example.enel_bitrix24_integration;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private static final String CLIENT_ID = "my-client-id";
    private static final String AUTH_URL = "https://api.enel.com/oauth2/token";

    // Usa un JWK valido (RSA privato) preso dal configuration, da sostituire con uno reale per test
    private static final String JWK_JSON = """
            {
                               "kty": "RSA",
                               "d": "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                               "e": "AQAB",
                               "n": "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                               "alg": "RS256",
                               "use": "sig",
                               "kid": "my-key-id"
                             }
        """;

    @Test
    void testBuildClientAssertion() throws Exception {
        TokenService tokenService = new TokenService(new RestTemplate());

        // Imposta i campi privati tramite reflection perché ora clientJwk è parsato in init()
        java.lang.reflect.Field clientIdField = TokenService.class.getDeclaredField("clientId");
        clientIdField.setAccessible(true);
        clientIdField.set(tokenService, CLIENT_ID);

        java.lang.reflect.Field authUrlField = TokenService.class.getDeclaredField("authUrl");
        authUrlField.setAccessible(true);
        authUrlField.set(tokenService, AUTH_URL);

        java.lang.reflect.Field clientJwkField = TokenService.class.getDeclaredField("clientJwk");
        clientJwkField.setAccessible(true);
        clientJwkField.set(tokenService, JWK_JSON);

        // Inizializza la chiave RSA (metodo annotato @PostConstruct)
        java.lang.reflect.Method initMethod = TokenService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(tokenService);

        // Invoca il metodo privato buildClientAssertion via reflection
        var buildClientAssertionMethod = TokenService.class.getDeclaredMethod("buildClientAssertion");
        buildClientAssertionMethod.setAccessible(true);
        String jwtString = (String) buildClientAssertionMethod.invoke(tokenService);

        assertNotNull(jwtString, "Il JWT non deve essere nullo");
        System.out.println("JWT generato:\n" + jwtString);

        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        RSAKey rsaKey = RSAKey.parse(JWK_JSON);

        // Verifica firma JWT con la public key
        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
        assertTrue(signedJWT.verify(verifier), "La firma JWT deve essere valida");

        // Verifica claims essenziali
        assertEquals(CLIENT_ID, signedJWT.getJWTClaimsSet().getIssuer());
        assertEquals(CLIENT_ID, signedJWT.getJWTClaimsSet().getSubject());
        assertTrue(signedJWT.getJWTClaimsSet().getAudience().contains(AUTH_URL));
        assertNotNull(signedJWT.getJWTClaimsSet().getExpirationTime());
        assertNotNull(signedJWT.getJWTClaimsSet().getJWTID());

        System.out.println("Claims JWT:");
        System.out.println(signedJWT.getJWTClaimsSet().toJSONObject());
    }
}


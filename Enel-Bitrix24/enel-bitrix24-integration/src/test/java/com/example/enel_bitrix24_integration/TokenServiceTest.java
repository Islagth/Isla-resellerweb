package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.security.TokenService;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private static final String CLIENT_ID = "my-client-id";
    private static final String AUTH_URL = "https://api.enel.com/oauth2/token";

    // ⚠️ Usa un JWK valido (RSA privato) preso dal tuo application.yml
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
        TokenService tokenService = new TokenService();

        // Setto i campi tramite reflection (in test non uso Spring)
        java.lang.reflect.Field f1 = TokenService.class.getDeclaredField("clientId");
        f1.setAccessible(true);
        f1.set(tokenService, CLIENT_ID);

        java.lang.reflect.Field f2 = TokenService.class.getDeclaredField("authUrl");
        f2.setAccessible(true);
        f2.set(tokenService, AUTH_URL);

        java.lang.reflect.Field f3 = TokenService.class.getDeclaredField("clientJwk");
        f3.setAccessible(true);
        f3.set(tokenService, JWK_JSON);

        // Richiamo metodo privato con reflection
        var method = TokenService.class.getDeclaredMethod("buildClientAssertion");
        method.setAccessible(true);
        String jwtString = (String) method.invoke(tokenService);

        assertNotNull(jwtString, "Il JWT non deve essere nullo");
        System.out.println("JWT generato:\n" + jwtString);

        // ✅ Parsing e validazione
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        RSAKey rsaKey = RSAKey.parse(JWK_JSON);

        // Verifica firma con la public key
        boolean verified = signedJWT.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()));
        assertTrue(verified, "La firma JWT deve essere valida");

        // Verifica claims
        assertEquals(CLIENT_ID, signedJWT.getJWTClaimsSet().getIssuer());
        assertEquals(CLIENT_ID, signedJWT.getJWTClaimsSet().getSubject());
        assertTrue(signedJWT.getJWTClaimsSet().getAudience().contains(AUTH_URL));
        assertNotNull(signedJWT.getJWTClaimsSet().getExpirationTime());
        assertNotNull(signedJWT.getJWTClaimsSet().getJWTID());

        System.out.println("Claims JWT:");
        System.out.println(signedJWT.getJWTClaimsSet().toJSONObject());
    }
}

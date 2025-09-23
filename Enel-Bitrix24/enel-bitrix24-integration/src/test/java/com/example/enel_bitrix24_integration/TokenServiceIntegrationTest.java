package com.example.enel_bitrix24_integration;
import com.example.enel_bitrix24_integration.security.TokenService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startServer() {
        wireMockServer = new WireMockServer(8089); // Porta diversa da default Spring
        wireMockServer.start();
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @Test
    void testGetAccessToken_FullFlow() throws Exception {
        // Mock risposta token endpoint
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"mocked-token-123\",\"expires_in\":300}")
                ));

        TokenService tokenService = new TokenService(new RestTemplate());

        // Inietto valori via reflection
        setField(tokenService, "authUrl", "http://localhost:8089/oauth2/token");
        setField(tokenService, "clientId", "my-client-id");

        String fakeJwk = """
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
        setField(tokenService, "clientJwk", fakeJwk);

        // Inizializza la chiave RSA (chiama init())
        var initMethod = TokenService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(tokenService);

        // Eseguo richiesta
        String token = tokenService.getAccessToken();

        assertEquals("mocked-token-123", token);
        assertNotNull(token);

        // Verifica WireMock ha ricevuto la chiamata corretta
        wireMockServer.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=my-client-id"))
                .withRequestBody(containing("client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer"))
                .withRequestBody(containing("scope=api.partner"))
        );

        System.out.println("✅ Access token ottenuto: " + token);
    }

    // Utility per impostare campi privati via reflection
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = TokenService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

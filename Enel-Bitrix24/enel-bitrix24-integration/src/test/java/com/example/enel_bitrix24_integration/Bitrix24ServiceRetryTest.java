package com.example.enel_bitrix24_integration;
import com.example.enel_bitrix24_integration.config.Bitrix24Properties;
import com.example.enel_bitrix24_integration.dto.Bitrix24Response;
import com.example.enel_bitrix24_integration.dto.EnelLeadRequest;
import com.example.enel_bitrix24_integration.dto.EsitoTelefonata;
import com.example.enel_bitrix24_integration.service.Bitrix24Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
@EnableRetry
public class Bitrix24ServiceRetryTest {

    @Mock
    private RestTemplate restTemplate;

    private Bitrix24Service bitrix24Service;
    private Bitrix24Properties properties;

    private RetryTemplate retryTemplate;

    @BeforeEach
    void setup() {
        properties = new Bitrix24Properties();
        properties.setUrl("https://bitrix24.test/api/leads");

        restTemplate = Mockito.mock(RestTemplate.class);
        bitrix24Service = new Bitrix24Service(properties, restTemplate);

        // 🔑 Configuriamo il retry manuale (3 tentativi senza delay)
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
    }

    @Test
    void testRetrySucceedsOnThirdAttempt() {
        EnelLeadRequest request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");

        Bitrix24Response okResponse = new Bitrix24Response();
        okResponse.setResult("LEAD_OK");
        okResponse.setEsitoTelefonata(EsitoTelefonata.OK_A_DISTANZA);

        // Simula: 1° e 2° tentativo = eccezione, 3° = successo
        when(restTemplate.postForEntity(
                eq(properties.getUrl()),
                any(),
                eq(Bitrix24Response.class))
        )
                .thenThrow(new RestClientException("Timeout 1"))
                .thenThrow(new RestClientException("Timeout 2"))
                .thenReturn(new ResponseEntity<>(okResponse, HttpStatus.OK));

        // 🔥 Eseguiamo il metodo tramite RetryTemplate
        Bitrix24Response response = retryTemplate.execute(context ->
                bitrix24Service.createLead(request)
        );

        // ✅ Verifica risultato finale
        assertNotNull(response);
        assertEquals("LEAD_OK", response.getResult());
        assertNull(response.getError());
        assertEquals(EsitoTelefonata.OK_A_DISTANZA, response.getEsitoTelefonata());

        // ✅ Verifica che postForEntity sia stato chiamato 3 volte
        verify(restTemplate, times(3)).postForEntity(
                eq(properties.getUrl()),
                any(),
                eq(Bitrix24Response.class)
        );
    }
}


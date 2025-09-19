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
import org.springframework.retry.annotation.EnableRetry;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new Bitrix24Properties();
        properties.setUrl("https://bitrix24.test/api/leads");
        bitrix24Service = Mockito.spy(new Bitrix24Service(properties));
        doReturn(restTemplate).when(bitrix24Service).createRestTemplate();
    }

    @Test
    void testRetrySucceedsOnThirdAttempt() {
        EnelLeadRequest request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");

        // Primo e secondo tentativo lanciano eccezione, il terzo va a buon fine
        doThrow(new RestClientException("Timeout"))
                .doThrow(new RestClientException("Timeout"))
                .doReturn(new Bitrix24Response() {{
                    setResult("LEAD_OK");
                    setEsitoTelefonata(EsitoTelefonata.OK_A_DISTANZA);
                }})
                .when(restTemplate).postForEntity(
                        eq(properties.getUrl()),
                        any(),
                        eq(Bitrix24Response.class)
                );

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        assertEquals("LEAD_OK", response.getResult());
        assertNull(response.getError());
        assertEquals(EsitoTelefonata.OK_A_DISTANZA, response.getEsitoTelefonata());

        // Verifica che postForEntity sia stato chiamato 3 volte a causa del retry
        verify(restTemplate, times(3)).postForEntity(
                eq(properties.getUrl()),
                any(),
                eq(Bitrix24Response.class)
        );
    }
}


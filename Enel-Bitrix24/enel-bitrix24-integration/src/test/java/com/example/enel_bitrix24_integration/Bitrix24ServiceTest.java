package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.config.Bitrix24Properties;
import com.example.enel_bitrix24_integration.dto.Bitrix24Response;
import com.example.enel_bitrix24_integration.dto.EnelLeadRequest;
import com.example.enel_bitrix24_integration.dto.EsitoTelefonata;
import com.example.enel_bitrix24_integration.service.Bitrix24Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


public class Bitrix24ServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private Bitrix24Service bitrix24Service;

    @Captor
    private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

    private Bitrix24Properties properties;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        properties = new Bitrix24Properties();
        properties.setUrl("https://bitrix24.test/api/leads");

        // Reinietto manualmente perché Bitrix24Service ha @RequiredArgsConstructor
        bitrix24Service = new Bitrix24Service(properties);
    }

    @Test
    void testCreateLead_success() {
        // 📌 Arrange
        EnelLeadRequest request;
        request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");

        Bitrix24Response mockResponse = new Bitrix24Response();
        mockResponse.setResult("LEAD_ID_123");

        ResponseEntity<Bitrix24Response> responseEntity =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                eq(properties.getUrl()),
                any(HttpEntity.class),
                eq(Bitrix24Response.class))
        ).thenReturn(responseEntity);

        // Uso Reflection per sostituire RestTemplate interno
        Bitrix24Service spyService = Mockito.spy(bitrix24Service);
        doReturn(mockRestTemplate).when(spyService).createRestTemplate();

        // 📌 Act
       Bitrix24Response response = spyService.createLead(request);

        // 📌 Assert
        assertNotNull(response);
        assertEquals("LEAD_ID_123", response.getResult());
        assertNull(response.getError());
        assertEquals(EsitoTelefonata.OK_A_DISTANZA, response.getEsitoTelefonata());
    }

    @Test
    void testCreateLead_errorFromBitrix() {
        // 📌 Arrange
        EnelLeadRequest request;
        request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");

        ResponseEntity<Bitrix24Response> responseEntity =
                new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                eq(properties.getUrl()),
                any(HttpEntity.class),
                eq(Bitrix24Response.class))
        ).thenReturn(responseEntity);

        Bitrix24Service spyService = Mockito.spy(bitrix24Service);
        doReturn(mockRestTemplate).when(spyService).createRestTemplate();

        // 📌 Act
        Bitrix24Response response = spyService.createLead(request);


        // 📌 Assert
        assertNotNull(response);
        assertTrue(response.getError().contains("Errore"));
        assertEquals(EsitoTelefonata.KO_NON_INTERESSATO,response.getEsitoTelefonata());
    }

    @Test
    void testCreateLead_exception() {
        // 📌 Arrange
        EnelLeadRequest request;
        request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                eq(properties.getUrl()),
                any(HttpEntity.class),
                eq(Bitrix24Response.class))
        ).thenThrow(new RuntimeException("Connessione fallita"));

        Bitrix24Service spyService = Mockito.spy(bitrix24Service);
        doReturn(mockRestTemplate).when(spyService).createRestTemplate();

        // 📌 Act
        Bitrix24Response response = spyService.createLead(request);

        // 📌 Assert
        assertNotNull(response);
        assertEquals("Connessione fallita", response.getError());
        assertEquals(EsitoTelefonata.KO_NUMERO_INESISTENTE, response.getEsitoTelefonata());
    }

    @Test
    void testCreateLead_connectionError() {
        // Arrange
        EnelLeadRequest request;
        request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                eq(properties.getUrl()),
                any(HttpEntity.class),
                eq(Bitrix24Response.class))
        ).thenThrow(new RestClientException("Timeout di connessione"));

        Bitrix24Service spyService = Mockito.spy(bitrix24Service);
        doReturn(mockRestTemplate).when(spyService).createRestTemplate();

        // Act
        Bitrix24Response response = spyService.createLead(request);

        // Assert
        assertNotNull(response);
        assertNull(response.getResult());
        assertEquals("Timeout di connessione", response.getError());
        assertEquals(EsitoTelefonata.KO_NUMERO_INESISTENTE, response.getEsitoTelefonata());
    }
}

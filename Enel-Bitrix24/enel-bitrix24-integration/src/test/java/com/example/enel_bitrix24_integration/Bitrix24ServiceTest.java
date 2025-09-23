package com.example.enel_bitrix24_integration;
import com.example.enel_bitrix24_integration.config.Bitrix24Properties;
import com.example.enel_bitrix24_integration.dto.Bitrix24Response;
import com.example.enel_bitrix24_integration.dto.EnelLeadRequest;
import com.example.enel_bitrix24_integration.dto.EsitoTelefonata;
import com.example.enel_bitrix24_integration.service.Bitrix24Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static com.example.enel_bitrix24_integration.dto.EsitoTelefonata.KO_NON_INTERESSATO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Bitrix24ServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

    private Bitrix24Properties properties;
    private Bitrix24Service bitrix24Service;

    @BeforeEach
    void setup() {
        properties = new Bitrix24Properties();
        properties.setUrl("https://bitrix24.test/api/leads");

        restTemplate = Mockito.mock(RestTemplate.class);
        bitrix24Service = new Bitrix24Service(properties, restTemplate);
    }

    private EnelLeadRequest createSampleRequest() {
        EnelLeadRequest request = new EnelLeadRequest();
        request.setCampaign_Id("Mario");
        request.setTelefono_Contatto("Rossi");
        request.setId_Anagrafica("1234567890");
        request.setCod_Contratto("mario.rossi@example.com");
        request.setPod_Pdr("Via Roma 1");
        return request;
    }

    @Test
    void testCreateLead_success() {
        EnelLeadRequest request = createSampleRequest();
        Bitrix24Response mockResponse = new Bitrix24Response();
        mockResponse.setResult("LEAD_ID_123");

        ResponseEntity<Bitrix24Response> responseEntity =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(properties.getUrl()),
                any(HttpEntity.class),
                eq(Bitrix24Response.class))
        ).thenReturn(responseEntity);

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        assertEquals("LEAD_ID_123", response.getResult());
        assertNull(response.getError());
        assertNull(response.getEsitoTelefonata());

        verify(restTemplate).postForEntity(
                eq(properties.getUrl()),
                httpEntityCaptor.capture(),
                eq(Bitrix24Response.class)
        );

        HttpEntity<?> capturedEntity = httpEntityCaptor.getValue();
        assertNotNull(capturedEntity);
        assertTrue(capturedEntity.getBody().toString().contains("Mario")); // esempio
    }

    @Test
    void testCreateLead_errorFromBitrix() {
        EnelLeadRequest request = createSampleRequest();

        Bitrix24Response errorResponse = new Bitrix24Response();
        errorResponse.setError("Errore: codice HTTP 400 BAD_REQUEST");
        errorResponse.setEsitoTelefonata(KO_NON_INTERESSATO);

        ResponseEntity<Bitrix24Response> responseEntity =
                new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenReturn(responseEntity);

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        assertNull(response.getResult());
        assertNotNull(response.getError());
        assertEquals("Errore: codice HTTP 400 BAD_REQUEST", response.getError());
        assertEquals("KO_NON_INTERESSATO", response.getEsitoTelefonata().toString());
    }


    @Test
    void testCreateLead_connectionError() {
        EnelLeadRequest request = createSampleRequest();

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenThrow(new RestClientException("Timeout di connessione"));

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        assertTrue(response.getError().contains("Timeout di connessione"));
        assertEquals(EsitoTelefonata.KO_NUMERO_INESISTENTE, response.getEsitoTelefonata());
    }

    @Test
    void testCreateLead_unexpectedError() {
        EnelLeadRequest request = createSampleRequest();

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenThrow(new RuntimeException("Errore interno"));

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        assertTrue(response.getError().contains("Errore interno"));
        assertEquals(EsitoTelefonata.KO_NUMERO_INESISTENTE, response.getEsitoTelefonata());
    }
}
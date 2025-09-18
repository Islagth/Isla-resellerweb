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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class Bitrix24ServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

    private Bitrix24Properties properties;

    private Bitrix24Service bitrix24Service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        properties = new Bitrix24Properties();
        properties.setUrl("https://bitrix24.test/api/leads");

        // Creiamo un spy sulla service originale per sovrascrivere createRestTemplate
        bitrix24Service = Mockito.spy(new Bitrix24Service(properties));

        // Sovrascriviamo createRestTemplate per restituire il mock restTemplate
        doReturn(restTemplate).when(bitrix24Service).createRestTemplate();
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

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenReturn(responseEntity);

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        assertEquals("LEAD_ID_123", response.getResult());
        // In base al codice originale non dovrebbe essere valorizzato error o esitoTelefonata
        assertNull(response.getError());
        assertNull(response.getEsitoTelefonata());
    }

    @Test
    void testCreateLead_errorFromBitrix() {
        EnelLeadRequest request = createSampleRequest();

        ResponseEntity<Bitrix24Response> responseEntity =
                new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenReturn(responseEntity);

        Bitrix24Response response = bitrix24Service.createLead(request);

        assertNotNull(response);
        // Il codice originale ritorna nuovo Bitrix24Response vuoto senza errori impostati
        assertNull(response.getResult());
        assertNull(response.getError());
        assertNull(response.getEsitoTelefonata());
    }

    @Test
    void testCreateLead_exception() {
        EnelLeadRequest request = createSampleRequest();

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenThrow(new RestClientException("Connessione fallita"));

        // Poiché il metodo rilancia eccezione, aspettiamoci che venga effettivamente sollevata
        assertThrows(RestClientException.class, () -> bitrix24Service.createLead(request));
    }

    @Test
    void testCreateLead_connectionError() {
        EnelLeadRequest request = createSampleRequest();

        when(restTemplate.postForEntity(eq(properties.getUrl()), any(HttpEntity.class), eq(Bitrix24Response.class)))
                .thenThrow(new RestClientException("Timeout di connessione"));

        assertThrows(RestClientException.class, () -> bitrix24Service.createLead(request));
    }
}

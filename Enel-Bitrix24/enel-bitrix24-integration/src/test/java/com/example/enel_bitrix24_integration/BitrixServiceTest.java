package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.dto.LeadRequest;
import com.example.enel_bitrix24_integration.dto.LeadResponse;
import com.example.enel_bitrix24_integration.service.BitrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BitrixServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BitrixService bitrixService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Impostare la baseUrl manualmente (dato che è @Value in BitrixService)
        var baseUrlField = BitrixService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(bitrixService, "http://fakeurl.com");
    }

    @Test
    void testInvioLavoratoSuccess() {
        LeadRequest request = new LeadRequest();
        LeadResponse expectedResponse = new LeadResponse();
        expectedResponse.setSuccess(true);
        // Simula risposta positiva del RestTemplate
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(LeadResponse.class)))
                .thenReturn(expectedResponse);

        LeadResponse response = bitrixService.invioLavorato(request);

        assertTrue(response.isSuccess());
        verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(LeadResponse.class));
    }

    @Test
    void testInvioLavoratoRetriesAndSuccess() {
        LeadRequest request = new LeadRequest();
        LeadResponse expectedResponse = new LeadResponse();
        expectedResponse.setSuccess(true);

        // Simula 2 eccezioni, poi un successo
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(LeadResponse.class)))
                .thenThrow(new RuntimeException("Errore 1"))
                .thenThrow(new RuntimeException("Errore 2"))
                .thenReturn(expectedResponse);

        LeadResponse response = bitrixService.invioLavorato(request);

        assertTrue(response.isSuccess());
        // Deve chiamare almeno 3 volte: 2 fallimenti + 1 successo
        verify(restTemplate, times(3)).postForObject(anyString(), any(HttpEntity.class), eq(LeadResponse.class));
    }

    @Test
    void testInvioLavoratoRetriesAndFail() {
        LeadRequest request = new LeadRequest();

        // Simula sempre eccezione per testare fallimento dopo max retry
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(LeadResponse.class)))
                .thenThrow(new RuntimeException("Errore fatale"));

        LeadResponse response = bitrixService.invioLavorato(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Errore chiamata API dopo"));
        // Deve chiamare 3 volte (max retry)
        verify(restTemplate, times(3)).postForObject(anyString(), any(HttpEntity.class), eq(LeadResponse.class));
    }
}

package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.config.EnelProperties;
import com.example.enel_bitrix24_integration.config.LeadScheduler;
import com.example.enel_bitrix24_integration.controller.EnelController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.enel_bitrix24_integration.dto.*;
import com.example.enel_bitrix24_integration.service.BitrixService;
import com.example.enel_bitrix24_integration.service.BlacklistService;
import com.example.enel_bitrix24_integration.service.LottoService;
import org.mockito.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnelControllerTest {

    @Mock
    private LottoService lottoService;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private BitrixService bitrixService;

    @Mock
    private EnelProperties enelProperties;

    @InjectMocks
    private EnelController enelController;

    private final String validToken = "valid-token";
    private final String authHeader = "Bearer " + validToken;
    private final String validBearerToken = "validToken";
    private final String validApiKey = "validApiKey";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(enelProperties.getClientJwt()).thenReturn(validToken);
    }

    // Test creaContattoLavorato


    @Test
    void creaContattoLavorato_Unauthorized_WhenInvalidToken() {
        LeadRequest request = new LeadRequest();
        ResponseEntity<?> response = enelController.creaContattoLavorato("Bearer invalid", "invalidKey", request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void creaContattoLavorato_Success_WhenValidBearerToken() {
        LeadRequest request = new LeadRequest();
        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setSuccess(true);

        // Mock del servizio Bitrix
        when(bitrixService.invioLavorato(request)).thenReturn(leadResponse);

        // Bearer token valido (deve corrispondere al valore mockato in enelProperties)
        String validBearer = "Bearer validToken";

        // Passa Bearer token valido e null per API-Key (doppio controllo)
        ResponseEntity<?> response = enelController.creaContattoLavorato(validBearer, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(leadResponse, response.getBody());
    }


    @Test
    void creaContattoLavorato_Success_WhenValidApiKey() {
        LeadRequest request = new LeadRequest();
        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setSuccess(true);
        when(bitrixService.invioLavorato(request)).thenReturn(leadResponse);

        ResponseEntity<?> response = enelController.creaContattoLavorato(null, validApiKey, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(leadResponse, response.getBody());
    }

    @Test
    void creaContattoLavorato_BadRequest_WhenServiceFailure() {
        LeadRequest request = new LeadRequest();
        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setSuccess(false);
        when(bitrixService.invioLavorato(request)).thenReturn(leadResponse);

        ResponseEntity<?> response = enelController.creaContattoLavorato("Bearer " + validBearerToken, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(leadResponse, response.getBody());
    }

    @Test
    void getUltimiLotti_Unauthorized() {
        ResponseEntity<?> response = enelController.getUltimiLotti("Bearer invalid", "invalidKey");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void getUltimiLotti_NoContent_WhenEmptyList() {
        when(lottoService.verificaLottiDisponibili()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = enelController.getUltimiLotti("Bearer " + validBearerToken, null);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getUltimiLotti_Ok_WhenLottiPresent() {
        List<LottoDTO> lotti = Arrays.asList(new LottoDTO());
        when(lottoService.verificaLottiDisponibili()).thenReturn(lotti);
        ResponseEntity<?> response = enelController.getUltimiLotti(null, validApiKey);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(lotti, response.getBody());
    }

    @Test
    void scaricaJson_Unauthorized() {
        ResponseEntity<?> response = enelController.scaricaJson("id1", "Bearer invalid", "invalidKey");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void scaricaJson_Success() throws Exception {
        String jsonData = "{\"key\":\"value\"}";
        when(lottoService.scaricaLottoJson("id1")).thenReturn(jsonData);
        ResponseEntity<?> response = enelController.scaricaJson("id1", "Bearer " + validBearerToken, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(jsonData, response.getBody());
    }

    @Test
    void scaricaZip_Unauthorized() {
        ResponseEntity<?> response = enelController.scaricaZip("id1", "Bearer invalid", "invalidKey");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void scaricaZip_Success() throws Exception {
        byte[] fakeZip = new byte[]{1, 2, 3};
        when(lottoService.scaricaLottoZip("id1")).thenReturn(fakeZip);
        ResponseEntity<?> response = enelController.scaricaZip("id1", null, validApiKey);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeZip, (byte[]) response.getBody());
        assertEquals("attachment; filename=lotto_id1.zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void getUltimiLottiBlacklist_Unauthorized() {
        ResponseEntity<?> response = enelController.getUltimiLottiBlacklist("Bearer invalid", "invalidKey");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getUltimiLottiBlacklist_NoContent() {
        when(blacklistService.verificaBlacklistDisponibili()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = enelController.getUltimiLottiBlacklist("Bearer " + validBearerToken, null);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getUltimiLottiBlacklist_Ok() {
        List<LottoBlacklistDTO> lotti = Arrays.asList(new LottoBlacklistDTO());
        when(blacklistService.verificaBlacklistDisponibili()).thenReturn(lotti);
        ResponseEntity<?> response = enelController.getUltimiLottiBlacklist(null, validApiKey);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(lotti, response.getBody());
    }

    @Test
    void scaricaZipBlacklist_Unauthorized() {
        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, "Bearer invalid", "invalidKey");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void scaricaZipBlacklist_Success() throws Exception {
        byte[] fakeZip = new byte[]{4, 5, 6};
        when(blacklistService.scaricaLottoBlacklistZip(123L)).thenReturn(fakeZip);
        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, "Bearer " + validBearerToken, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeZip, (byte[]) response.getBody());
        assertEquals("attachment; filename=lotto_blacklist123.zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void confermaLotto_Unauthorized() {
        ResponseEntity<?> response = enelController.confermaLotto(10L, "Bearer invalid", "invalidKey");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void confermaLotto_Success() throws Exception {
        ResponseEntity<?> response = enelController.confermaLotto(10L, "Bearer " + validBearerToken, null);
        verify(blacklistService).confermaLotto(10L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) body.get("success"));
    }

    @Test
    void confermaLotto_BadRequest() throws Exception {
        doThrow(new IllegalArgumentException()).when(blacklistService).confermaLotto(10L);
        ResponseEntity<?> response = enelController.confermaLotto(10L, null, validApiKey);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}


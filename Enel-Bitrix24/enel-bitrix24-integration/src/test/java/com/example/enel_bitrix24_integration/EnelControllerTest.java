package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.config.EnelProperties;
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
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(enelProperties.getClientJwt()).thenReturn(validToken);
    }

    // Test creaContattoLavorato

    @Test
    void creaContattoLavorato_Unauthorized_WhenInvalidToken() {
        LeadRequest request = new LeadRequest();
        ResponseEntity<?> response = enelController.creaContattoLavorato("Bearer invalid", request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void creaContattoLavorato_Success_WhenValidTokenAndServiceSuccess() {
        LeadRequest request = new LeadRequest();
        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setSuccess(true);
        when(bitrixService.invioLavorato(request)).thenReturn(leadResponse);

        ResponseEntity<?> response = enelController.creaContattoLavorato(authHeader, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(leadResponse, response.getBody());
    }

    @Test
    void creaContattoLavorato_BadRequest_WhenValidTokenAndServiceFailure() {
        LeadRequest request = new LeadRequest();
        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setSuccess(false);
        when(bitrixService.invioLavorato(request)).thenReturn(leadResponse);

        ResponseEntity<?> response = enelController.creaContattoLavorato(authHeader, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(leadResponse, response.getBody());
    }

    // Test getUltimiLotti

    @Test
    void getUltimiLotti_Unauthorized() {
        ResponseEntity<?> response = enelController.getUltimiLotti("Bearer invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void getUltimiLotti_NoContent_WhenEmptyList() {
        when(lottoService.verificaLottiDisponibili()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = enelController.getUltimiLotti(authHeader);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getUltimiLotti_Ok_WhenLottiPresent() {
        List<LottoDTO> lotti = Arrays.asList(new LottoDTO());
        when(lottoService.verificaLottiDisponibili()).thenReturn(lotti);

        ResponseEntity<?> response = enelController.getUltimiLotti(authHeader);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(lotti, response.getBody());
    }

    // Test scaricaJson

    @Test
    void scaricaJson_Unauthorized() {
        ResponseEntity<?> response = enelController.scaricaJson("id1", "Bearer invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void scaricaJson_Success() throws Exception {
        String jsonData = "{\"key\":\"value\"}";
        when(lottoService.scaricaLottoJson("id1")).thenReturn(jsonData);

        ResponseEntity<?> response = enelController.scaricaJson("id1", authHeader);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(jsonData, response.getBody());
    }

    @Test
    void scaricaJson_NotFound() throws Exception {
        when(lottoService.scaricaLottoJson("id1")).thenThrow(new Exception("Slice Id not found"));

        ResponseEntity<?> response = enelController.scaricaJson("id1", authHeader);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void scaricaJson_Forbidden() throws Exception {
        when(lottoService.scaricaLottoJson("id1")).thenThrow(new Exception("Slice Id not available"));

        ResponseEntity<?> response = enelController.scaricaJson("id1", authHeader);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void scaricaJson_InternalError() throws Exception {
        when(lottoService.scaricaLottoJson("id1")).thenThrow(new Exception("Other error"));

        ResponseEntity<?> response = enelController.scaricaJson("id1", authHeader);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // Test scaricaZip

    @Test
    void scaricaZip_Unauthorized() {
        ResponseEntity<?> response = enelController.scaricaZip("id1", "Bearer invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void scaricaZip_Success() throws Exception {
        byte[] fakeZip = new byte[]{1, 2, 3};
        when(lottoService.scaricaLottoZip("id1")).thenReturn(fakeZip);

        ResponseEntity<?> response = enelController.scaricaZip("id1", authHeader);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeZip, (byte[]) response.getBody());
        assertEquals("attachment; filename=lotto_id1.zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void scaricaZip_NotFound() throws Exception {
        when(lottoService.scaricaLottoZip("id1")).thenThrow(new RuntimeException("Slice Id not found"));

        ResponseEntity<?> response = enelController.scaricaZip("id1", authHeader);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void scaricaZip_Forbidden() throws Exception {
        when(lottoService.scaricaLottoZip("id1")).thenThrow(new RuntimeException("Slice Id not available"));

        ResponseEntity<?> response = enelController.scaricaZip("id1", authHeader);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void scaricaZip_InternalError() throws Exception {
        when(lottoService.scaricaLottoZip("id1")).thenThrow(new RuntimeException("Unknown error"));

        ResponseEntity<?> response = enelController.scaricaZip("id1", authHeader);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // Test getUltimiLottiBlacklist

    @Test
    void getUltimiLottiBlacklist_Unauthorized() {
        ResponseEntity<?> response = enelController.getUltimiLottiBlacklist("Bearer invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getUltimiLottiBlacklist_NoContent() {
        when(blacklistService.verificaBlacklistDisponibili()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = enelController.getUltimiLottiBlacklist(authHeader);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getUltimiLottiBlacklist_Ok() {
        List<LottoBlacklistDTO> lottiBlacklist = Arrays.asList(new LottoBlacklistDTO());
        when(blacklistService.verificaBlacklistDisponibili()).thenReturn(lottiBlacklist);

        ResponseEntity<?> response = enelController.getUltimiLottiBlacklist(authHeader);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(lottiBlacklist, response.getBody());
    }

    // Test scaricaZipBlacklist

    @Test
    void scaricaZipBlacklist_Unauthorized() {
        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, "Bearer invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void scaricaZipBlacklist_Success() throws Exception {
        byte[] fakeZip = new byte[]{4, 5, 6};
        when(blacklistService.scaricaLottoBlacklistZip(123L)).thenReturn(fakeZip);

        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, authHeader);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeZip, (byte[]) response.getBody());
        assertEquals("attachment; filename=lotto_blacklist123.zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void scaricaZipBlacklist_NotFound() throws Exception {
        when(blacklistService.scaricaLottoBlacklistZip(123L)).thenThrow(new RuntimeException("Slice Id not found"));

        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, authHeader);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void scaricaZipBlacklist_Forbidden() throws Exception {
        when(blacklistService.scaricaLottoBlacklistZip(123L)).thenThrow(new RuntimeException("Slice Id not available"));

        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, authHeader);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void scaricaZipBlacklist_InternalError() throws Exception {
        when(blacklistService.scaricaLottoBlacklistZip(123L)).thenThrow(new RuntimeException("Unknown error"));

        ResponseEntity<?> response = enelController.scaricaZipBlacklist(123L, authHeader);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // Test confermaLotto

    @Test
    void confermaLotto_Unauthorized() {
        ResponseEntity<?> response = enelController.confermaLotto(10L, "Bearer invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void confermaLotto_Success() throws Exception {
        ResponseEntity<?> response = enelController.confermaLotto(10L, authHeader);
        verify(blacklistService).confermaLotto(10L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) body.get("success"));
    }

    @Test
    void confermaLotto_BadRequest() throws Exception {
        doThrow(new IllegalArgumentException()).when(blacklistService).confermaLotto(10L);
        ResponseEntity<?> response = enelController.confermaLotto(10L, authHeader);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void confermaLotto_InternalError() throws Exception {
        doThrow(new RuntimeException("Server Error")).when(blacklistService).confermaLotto(10L);
        ResponseEntity<?> response = enelController.confermaLotto(10L, authHeader);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

}

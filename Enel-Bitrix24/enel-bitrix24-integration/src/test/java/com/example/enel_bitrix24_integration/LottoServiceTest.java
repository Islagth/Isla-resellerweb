package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.dto.LottoDTO;
import com.example.enel_bitrix24_integration.service.LottoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LottoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LottoService lottoService;

    private final String baseUrl = "http://fakeurl.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Configura baseUrl riflettendo il valore @Value nella classe
        // Poiché è private, useremo reflection o setter se presente
        // Qui userò reflection
        try {
            var field = LottoService.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(lottoService, baseUrl);
        } catch (Exception e) {
            fail("Impostazione baseUrl fallita: " + e.getMessage());
        }
    }

    @Test
    void testVerificaLottiDisponibili_success() throws Exception {
        String url = baseUrl + "/partner-api/v5/slices";
        String jsonResponse = "[{\"id\":\"1\"},{\"id\":\"2\"}]";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(new URI(url), String.class)).thenReturn(responseEntity);
        when(objectMapper.readValue(jsonResponse, LottoDTO[].class)).thenReturn(new LottoDTO[2]);

        List<LottoDTO> result = lottoService.verificaLottiDisponibili();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(restTemplate, times(1)).getForEntity(new URI(url), String.class);
        verify(objectMapper, times(1)).readValue(jsonResponse, LottoDTO[].class);
    }

    @Test
    void testVerificaLottiDisponibili_apiError() throws Exception {
        String url = baseUrl + "/partner-api/v5/slices";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.getForEntity(new URI(url), String.class)).thenReturn(responseEntity);

        List<LottoDTO> result = lottoService.verificaLottiDisponibili();

        assertNotNull(result);
        // Ultimi lotti non aggiornati in caso di errore, quindi lista vuota o precedente stato
        verify(restTemplate, times(1)).getForEntity(new URI(url), String.class);
        verify(objectMapper, times(0)).readValue(anyString(), (Class<Object>) any());
    }

    @Test
    void testScaricaLottoJson_success() throws Exception {
        String idLotto = "123";
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".json";
        String jsonResponse = "{\"id\":\"123\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(new URI(url), String.class)).thenReturn(responseEntity);

        String result = lottoService.scaricaLottoJson(idLotto);
        assertEquals(jsonResponse, result);
    }

    @Test
    void testScaricaLottoJson_notFound() throws Exception {
        String idLotto = "404";
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".json";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.NOT_FOUND);

        when(restTemplate.getForEntity(new URI(url), String.class)).thenReturn(responseEntity);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> lottoService.scaricaLottoJson(idLotto));
        assertEquals("Slice Id not found", thrown.getMessage());
    }

    @Test
    void testScaricaLottoZip_success() throws Exception {
        String idLotto = "zip123";
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".zip";
        byte[] fakeZip = new byte[]{0x50, 0x4B, 0x03, 0x04}; // inizio file ZIP
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(fakeZip, HttpStatus.OK);

        when(restTemplate.exchange(new URI(url), HttpMethod.GET, null, byte[].class)).thenReturn(responseEntity);

        byte[] result = lottoService.scaricaLottoZip(idLotto);

        assertArrayEquals(fakeZip, result);
    }

    @Test
    void testScaricaLottoZip_notFound() throws Exception {
        String idLotto = "notfound";
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".zip";
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(new URI(url), HttpMethod.GET, null, byte[].class)).thenReturn(responseEntity);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> lottoService.scaricaLottoZip(idLotto));
        assertEquals("Slice Id not found", thrown.getMessage());
    }

    @Test
    void testScaricaLottoZip_emptyBody() throws Exception {
        String idLotto = "emptyzip";
        String url = baseUrl + "/partner-api/v5/slices/" + idLotto + ".zip";
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(new byte[0], HttpStatus.OK);

        when(restTemplate.exchange(new URI(url), HttpMethod.GET, null, byte[].class)).thenReturn(responseEntity);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> lottoService.scaricaLottoZip(idLotto));
        assertEquals("Errore scaricamento ZIP per lotto " + idLotto, thrown.getMessage());
    }
}


package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.dto.LottoBlacklistDTO;
import com.example.enel_bitrix24_integration.service.BlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlacklistServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject baseUrl value manually for tests
        TestUtils.setField(blacklistService, "baseUrl", "http://localhost/api");
    }

    @Test
    void testVerificaBlacklistDisponibiliSuccess() throws Exception {
        String jsonResponse = "[{\"id\":1,\"name\":\"lotto1\"},{\"id\":2,\"name\":\"lotto2\"}]";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(any(URI.class), eq(String.class))).thenReturn(responseEntity);
        LottoBlacklistDTO[] dtoArray = new LottoBlacklistDTO[]{
                new LottoBlacklistDTO(),
                new LottoBlacklistDTO()
        };
        when(objectMapper.readValue(jsonResponse, LottoBlacklistDTO[].class)).thenReturn(dtoArray);

        List<LottoBlacklistDTO> result = blacklistService.verificaBlacklistDisponibili();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(restTemplate, times(1)).getForEntity(any(URI.class), eq(String.class));
        verify(objectMapper, times(1)).readValue(jsonResponse, LottoBlacklistDTO[].class);
    }

    @Test
    void testScaricaLottoBlacklistZipSuccess() throws Exception {
        long lottoId = 123L;
        byte[] data = new byte[]{1, 2, 3};
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(data, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), isNull(), eq(byte[].class)))
                .thenReturn(responseEntity);

        byte[] result = blacklistService.scaricaLottoBlacklistZip(lottoId);

        assertArrayEquals(data, result);
        verify(restTemplate, times(1)).exchange(any(URI.class), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    }

    @Test
    void testScaricaLottoBlacklistZipNotFound() {
        long lottoId = 123L;
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), isNull(), eq(byte[].class)))
                .thenReturn(responseEntity);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> blacklistService.scaricaLottoBlacklistZip(lottoId));
        assertEquals("Slice Id not found", thrown.getMessage());
    }

    @Test
    void testConfermaLottoSuccess() throws Exception {
        long lottoId = 456L;
        String responseJson = "{\"success\":true}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), isNull(), eq(String.class)))
                .thenReturn(responseEntity);

        assertDoesNotThrow(() -> blacklistService.confermaLotto(lottoId));
    }

    @Test
    void testConfermaLottoFailure() throws Exception {
        long lottoId = 456L;
        String responseJson = "{\"success\":false,\"message\":\"Error message\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), isNull(), eq(String.class)))
                .thenReturn(responseEntity);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> blacklistService.confermaLotto(lottoId));
        assertTrue(thrown.getMessage().contains("Errore conferma lotto"));
    }
}

class TestUtils {
    static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


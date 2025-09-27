package com.example.enel_bitrix24_integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.*;

import com.example.enel_bitrix24_integration.dto.ContactDTO;
import com.example.enel_bitrix24_integration.service.ContactService;
import com.example.enel_bitrix24_integration.service.LottoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class ContactServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LottoService lottoService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContactService contactService;

    private final String baseUrl = "http://bitrix24.api";

    private final String accessToken = "fakeAccessToken";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // Creare spy invece di istanza diretta
        contactService = Mockito.spy(new ContactService(restTemplate, baseUrl, lottoService, objectMapper));
    }


    @Test
    public void testCreaContattoSuccess() throws Exception {
        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setNAME("Mario");
        contactDTO.setLAST_NAME("Rossi");

        Map<String, Object> responseMap = Map.of("result", "1");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = contactService.creaContatto(contactDTO, accessToken);

        assertTrue(result.contains("Creazione contatto riuscita"));
    }


    @Test
    public void testAggiornaContattoSuccess() throws Exception {
        Map<String, Object> fields = Map.of("PHONE", "+3901234567");
        Map<String, Object> responseMap = Map.of("result", "true");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = contactService.aggiornaContatto(1, fields, null, accessToken);
        assertTrue(result.contains("Aggiornamento contatto riuscita"));
    }

    @Test
    public void testGetContattoByIdSuccess() throws Exception {
        Map<String, Object> responseMap = Map.of("result", Map.of("ID", 1, "NAME", "Mario"));

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Map<String, Object> result = contactService.getContattoById(1, accessToken);
        assertEquals(responseMap, result);
    }

    @Test
    public void testListaContattiSuccess() throws Exception {
        Map<String, Object> responseMap = Map.of("result", List.of());
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Map<String, Object> result = contactService.listaContatti(null, null, null, null, accessToken);
        assertEquals(responseMap, result);
    }

    @Test
    public void testEliminaContattoSuccess() throws Exception {
        Map<String, Object> responseMap = Map.of("result", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        boolean deleted = contactService.eliminaContatto(1, accessToken);
        assertTrue(deleted);
    }

    @Test
    public void testEliminaContattoUnexpectedResponseThrows() throws Exception {
        Map<String, Object> responseMap = Map.of("result", "notBoolean");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        assertThrows(RuntimeException.class, () -> {
            contactService.eliminaContatto(1, accessToken);
        });
    }

    private ContactDTO createContactDTO(String name, String lastName) {
        ContactDTO dto = new ContactDTO();
        dto.setNAME(name);
        dto.setLAST_NAME(lastName);
        return dto;
    }
}

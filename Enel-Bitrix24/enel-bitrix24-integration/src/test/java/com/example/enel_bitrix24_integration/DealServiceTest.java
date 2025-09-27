package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.dto.DealDTO;
import com.example.enel_bitrix24_integration.service.DealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class DealServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DealService dealService;

    private static final String TEST_ACCESS_TOKEN = "fake-access-token";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private ResponseEntity<Map> createResponse(Object body) {
        return new ResponseEntity<>((Map<String, Object>) body, HttpStatus.OK);
    }

    @Test
    void testAddDealSuccess() {
        DealDTO dto = new DealDTO(1,"D1");
        dto.setTitle("test");
        Map<String, Object> responseBody = Map.of("result", 123);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(createResponse(responseBody));

        Integer id = dealService.addDeal(dto, null, TEST_ACCESS_TOKEN);
        assertEquals(123, id);
    }

    @Test
    void testAddDealError() {
        DealDTO dto = new DealDTO(1,"D1");
        dto.setTitle("test");
        Map<String, Object> responseBody = Map.of(
                "error", "ID is not defined or invalid",
                "error_description", "ID mancante"
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(createResponse(responseBody));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dealService.addDeal(dto, null, TEST_ACCESS_TOKEN));
        assertTrue(ex.getMessage().contains("ID mancante"));
    }

    @Test
    void testUpdateDealSuccess() {
        DealDTO dto = new DealDTO(1,"D1");
        dto.setId(1);
        dto.setTitle("updated");

        Map<String, Object> responseBody = Map.of("result", true);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(createResponse(responseBody));

        assertTrue(dealService.updateDeal(dto, null, TEST_ACCESS_TOKEN));
    }

    @Test
    void testUpdateDealInvalidId() {
        DealDTO dto = new DealDTO(1,"D1");
        dto.setId(0); // ID non valido

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dealService.updateDeal(dto, null, TEST_ACCESS_TOKEN));
        assertTrue(ex.getMessage().contains("ID del deal deve essere valido"));
    }

    @Test
    void testGetDealByIdSuccess() {
        Integer id = 1;
        Map<String, Object> dealMap = new HashMap<>();
        dealMap.put("ID", "1");
        dealMap.put("TITLE", "Sample");

        Map<String, Object> responseBody = Map.of("result", dealMap);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(createResponse(responseBody));

        DealDTO dto = dealService.getDealById(id, TEST_ACCESS_TOKEN);
        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("Sample", dto.getTitle());
    }

    @Test
    void testGetDealByIdInvalidId() {
        Integer id = -5;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dealService.getDealById(id, TEST_ACCESS_TOKEN));
        assertTrue(ex.getMessage().contains("ID del deal deve essere valido"));
    }

    @Test
    void testGetDealsListSuccess() {
        List<String> select = List.of("ID", "TITLE");
        Map<String, Object> filter = Map.of("CATEGORY_ID", 1);
        Map<String, String> order = Map.of("TITLE", "ASC");
        int start = 0;

        List<Map<String, Object>> deals = List.of(
                Map.of("ID", "1", "TITLE", "Deal1"),
                Map.of("ID", "2", "TITLE", "Deal2")
        );

        Map<String, Object> responseBody = Map.of("result", deals);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(createResponse(responseBody));

        List<DealDTO> result = dealService.getDealsList(select, filter, order, start, TEST_ACCESS_TOKEN);
        assertEquals(2, result.size());
        assertEquals("Deal1", result.get(0).getTitle());
        assertEquals("Deal2", result.get(1).getTitle());
    }

    @Test
    void testDeleteDealSuccess() {
        Integer id = 1;
        Map<String, Object> responseBody = Map.of("result", true);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(createResponse(responseBody));

        assertTrue(dealService.deleteDeal(id, TEST_ACCESS_TOKEN));
    }

    @Test
    void testDeleteDealInvalidId() {
        Integer id = 0;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dealService.deleteDeal(id, TEST_ACCESS_TOKEN));
        assertTrue(ex.getMessage().contains("ID del deal deve essere valido"));
    }
}

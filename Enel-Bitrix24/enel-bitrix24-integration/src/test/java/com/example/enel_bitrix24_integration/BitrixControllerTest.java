package com.example.enel_bitrix24_integration;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;

import com.example.enel_bitrix24_integration.config.BitrixOAuthProperties;
import com.example.enel_bitrix24_integration.config.TokenRecord;
import com.example.enel_bitrix24_integration.config.TokenResponse;
import com.example.enel_bitrix24_integration.controller.BitrixController;
import com.example.enel_bitrix24_integration.dto.DealDTO;
import com.example.enel_bitrix24_integration.security.OAuthService;
import com.example.enel_bitrix24_integration.security.TokenStorageService;
import com.example.enel_bitrix24_integration.service.ContactService;
import com.example.enel_bitrix24_integration.service.DealService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(BitrixController.class)
public class BitrixControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DealService dealService;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private OAuthService oAuthService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private BitrixOAuthProperties oAuthProperties;

    @MockitoBean
    private TokenRecord tokenRecord;

    @MockitoSpyBean
    private BitrixController bitrixController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final String validAuthHeader = "Bearer valid_access_token";
    private final String invalidAuthHeader = "Bearer invalid_access_token";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper per simulare isAuthorized
    private void mockAuthorization(boolean authorized) {
        doReturn(authorized).when(bitrixController).isAuthorized(anyString());
    }

    @Test
    public void testCreateDealAuthorized() throws Exception {
        mockAuthorization(true);
        when(dealService.addDeal(any(), anyMap(), anyString())).thenReturn(101);

        String body = "{\"title\":\"Test Deal\"}";

        mockMvc.perform(post("/api/enel-leads/addDeal")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealId").value(101));
    }

    @Test
    public void testCreateDealUnauthorized() throws Exception {
        mockAuthorization(false);

        String body = "{\"title\":\"Test Deal\"}";

        mockMvc.perform(post("/api/enel-leads/addDeal")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", invalidAuthHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testUpdateDealSuccess() throws Exception {
        mockAuthorization(true);
        when(dealService.updateDeal(any(), anyMap(), anyString())).thenReturn(true);

        String body = "{\"id\":1,\"title\":\"Update\"}";

        mockMvc.perform(put("/api/enel-leads/updateDeal")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true));
    }

    @Test
    public void testGetDealById() throws Exception {
        mockAuthorization(true);

        DealDTO dealDTO = new DealDTO(1,"D1");
        dealDTO.setId(7);
        dealDTO.setTitle("Deal Seven");

        when(dealService.getDealById(eq(7), anyString())).thenReturn(dealDTO);

        mockMvc.perform(get("/api/enel-leads/deal/7")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Deal Seven"));
    }

    @Test
    public void testGetDealsList() throws Exception {
        mockAuthorization(true);

        List<DealDTO> deals = List.of(new DealDTO(1, "D1"), new DealDTO(2, "D2"));
        Map<String, Object> requestBody = Map.of(
                "select", List.of("ID", "TITLE"),
                "filter", Map.of("CATEGORY_ID", 1),
                "order", Map.of("TITLE", "ASC"),
                "start", 0
        );

        when(dealService.getDealsList(any(), any(), any(), anyInt(), anyString()))
                .thenReturn(deals);

        mockMvc.perform(post("/api/enel-leads/deal-list")
                        .content(objectMapper.writeValueAsString(requestBody))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].title").value("D2"));
    }

    @Test
    public void testDeleteDeal() throws Exception {
        mockAuthorization(true);

        when(dealService.deleteDeal(eq(5), anyString())).thenReturn(true);

        mockMvc.perform(delete("/api/enel-leads/delete-deal/5")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    public void testCreaContattiDalLottoSuccess() throws Exception {
        mockAuthorization(true);
        doNothing().when(contactService).creaContattiDaLotto(anyString(), anyString());

        mockMvc.perform(post("/api/enel-leads/someId/add-contact")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testAggiornaContattoSuccess() throws Exception {
        mockAuthorization(true);

        Map<String, Object> payload = Map.of(
                "fields", Map.of("PHONE", "123456"),
                "params", Map.of("param1", "val1")
        );

        when(contactService.aggiornaContatto(anyInt(), anyMap(), anyMap(), anyString())).thenReturn("aggiornamento riuscito");

        mockMvc.perform(put("/api/enel-leads/update-contact")
                        .param("id", "33")
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("aggiornamento riuscito"));
    }

    @Test
    public void testGetContattoById() throws Exception {
        mockAuthorization(true);

        Map<String, Object> contactData = Map.of("ID", 12, "NAME", "Mario");

        when(contactService.getContattoById(eq(12), anyString())).thenReturn(contactData);

        mockMvc.perform(get("/api/enel-leads/contact/12")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ID").value(12))
                .andExpect(jsonPath("$.NAME").value("Mario"));
    }

    @Test
    public void testListaContatti() throws Exception {
        mockAuthorization(true);

        Map<String, Object> requestBody = Map.of(
                "select", List.of("ID", "NAME"),
                "filter", Map.of("ACTIVE", "Y"),
                "order", Map.of("ID", "DESC"),
                "start", 0
        );

        Map<String, Object> resultMap = Map.of(
                "result", List.of(
                        Map.of("ID", 1, "NAME", "Luca"),
                        Map.of("ID", 2, "NAME", "Anna")
                )
        );

        when(contactService.listaContatti(anyMap(), anyMap(), anyList(), any(), anyString()))
                .thenReturn(resultMap);

        mockMvc.perform(post("/api/enel-leads/contact-list")
                        .content(objectMapper.writeValueAsString(requestBody))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists());
    }

    @Test
    public void testEliminaContattoSuccess() throws Exception {
        mockAuthorization(true);

        when(contactService.eliminaContatto(eq(5), anyString())).thenReturn(true);

        mockMvc.perform(delete("/api/enel-leads/delete-contact/5")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("Contatto eliminato con successo."));
    }

    @Test
    public void testEliminaContattoFailure() throws Exception {
        mockAuthorization(true);

        when(contactService.eliminaContatto(eq(5), anyString())).thenReturn(false);

        mockMvc.perform(delete("/api/enel-leads/delete-contact/5")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Impossibile eliminare il contatto."));
    }

    @Test
    public void testOauthAuthorizeRedirect() throws Exception {
        mockMvc.perform(get("/api/enel-leads/oauth/authorize"))
                .andExpect(status().is3xxRedirection());
    }


    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testOauthCallbackSuccess() throws Exception {
        TokenResponse tokens = new TokenResponse();
        tokens.setAccess_token("access");
        tokens.setRefresh_token("refresh");
        tokens.setExpires_in(3600);

        // Usa ArgumentMatchers.any() per accettare qualsiasi valore
        when(oAuthService.getTokens(any(), any(), any(), any())).thenReturn(tokens);
        doNothing().when(tokenStorageService).saveTokens(any());

        mockMvc.perform(get("/api/enel-leads/oauth/callback")
                        .param("code", "code123")
                        .param("state", "statevalue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        // Verifica che getTokens sia stato chiamato almeno una volta
        verify(oAuthService, times(1)).getTokens(any(), any(), any(), any());
    }

    @Test
    public void testOauthCallbackError() throws Exception {
        when(oAuthService.getTokens(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Errore OAuth"));

        mockMvc.perform(get("/api/enel-leads/oauth/callback")
                        .param("code", "code123")
                        .param("state", "statevalue"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Errore ottenimento token OAuth"));
    }
}

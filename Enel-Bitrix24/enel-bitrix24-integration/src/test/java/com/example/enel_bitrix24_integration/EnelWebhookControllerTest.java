package com.example.enel_bitrix24_integration;

import com.example.enel_bitrix24_integration.config.EnelProperties;
import com.example.enel_bitrix24_integration.controller.EnelWebhookController;
import com.example.enel_bitrix24_integration.dto.EnelLeadRequest;
import com.example.enel_bitrix24_integration.service.Bitrix24Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnelWebhookController.class)
public class EnelWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private Bitrix24Service bitrix24Service;

    @Mock
    private EnelProperties enelProperties;

    @InjectMocks
    private EnelWebhookController enelWebhookController;

    private EnelLeadRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new EnelLeadRequest();
        validRequest.setCampaign_Id("Mario");
        validRequest.setTelefono_Contatto("Rossi");
        validRequest.setId_Anagrafica("1234567890");
        validRequest.setCod_Contratto("mario.rossi@example.com");
        validRequest.setPod_Pdr("Via Roma 1");

        Mockito.when(bitrix24Service.createLead(any()))
                .thenReturn(null); // Mock: non interessa il dettaglio della risposta
    }

    @Test
    void shouldReturn400WhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/enel-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        Mockito.when(enelProperties.getClientJwt()).thenReturn("VALID_TOKEN");

        mockMvc.perform(post("/api/enel-leads")
                        .header("Authorization", "Bearer WRONG_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WhenTokenIsValid() throws Exception {
        Mockito.when(enelProperties.getClientJwt()).thenReturn("VALID_TOKEN");

        mockMvc.perform(post("/api/enel-leads")
                        .header("Authorization", "Bearer VALID_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }
}

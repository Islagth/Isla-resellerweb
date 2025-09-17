package com.example.enel_bitrix24_integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class EnelLeadRequest {

    @NotBlank
    private String campaign_Id;

    @NotBlank
    private String telefono_Contatto;

    @NotBlank
    private String id_Anagrafica;

    @NotBlank
    private String cod_Contratto;

    @NotBlank
    private String pod_Pdr;


}

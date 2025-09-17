package com.example.enel_bitrix24_integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class Bitrix24Response {

    private String campaign_Id;
    private String telefono_Contatto;
    private String id_Anagrafica;
    private String cod_Contratto;
    private String pod_Pdr;
    private EsitoTelefonata esitoTelefonata;

    private String result;
    private String error;

}

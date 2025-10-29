package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

@Data
public class CampaignDTO {

    private int id_campagna;
    private int id_pianificazione_campagna;
    private int id_config_campagna;
    private String pianificazione;
    private String campagna;
    private boolean masked;
    private int pianificate;
    private int scaricate;
    private int scaricabili;
    private Boolean slice_by_tag;
}

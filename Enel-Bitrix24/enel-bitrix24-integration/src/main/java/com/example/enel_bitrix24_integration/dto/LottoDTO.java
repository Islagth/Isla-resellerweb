package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

@Data
public class LottoDTO {

    private String id_lotto;
    private String tipologia_lotto;
    private String data_lotto;
    private int contatti;
    private int anagrafiche;
    private String id_campagna;
    private boolean masked;
    private Boolean slice_by_tag;
}

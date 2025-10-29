package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

@Data
public class SliceResponse {

    private int id_lotto;
    private Boolean masked;
    private Boolean success;
    private String message;
}

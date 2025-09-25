package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

@Data
public class LottoBlacklistDTO {

    private long id;
    private String creation_date;
    private int size;
    private String confirmation_date;

}

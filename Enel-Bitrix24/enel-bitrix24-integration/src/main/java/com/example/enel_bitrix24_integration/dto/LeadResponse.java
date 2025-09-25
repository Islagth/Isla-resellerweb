package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

@Data
public class LeadResponse {

    private boolean success;
    private Long workedId;
    private String message;

}

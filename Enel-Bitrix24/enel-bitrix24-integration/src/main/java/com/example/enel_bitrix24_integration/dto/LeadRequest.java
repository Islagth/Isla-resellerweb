package com.example.enel_bitrix24_integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadRequest {

    private String workedCode;
    private String worked_Date;
    private String worked_End_Date;
    private String resultCode;
    private String caller;
    private String workedType;
    private Long campaignId;
    private Long contactId;
    private String chatHistory;


}

package com.example.enel_bitrix24_integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeadRequest {

    private String workedCode;
    private LocalDateTime worked_Date;
    private LocalDateTime worked_End_Date;
    private String resultCode;
    private String caller;
    private String workedType;
    private Long campaignId;
    private Long contactId;
    private String chatHistory;


}

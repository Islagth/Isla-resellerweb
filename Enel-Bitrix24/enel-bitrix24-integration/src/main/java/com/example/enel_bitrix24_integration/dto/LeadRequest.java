package com.example.enel_bitrix24_integration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;


public class LeadRequest {

    @JsonProperty("workedCode")
    private String workedCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("workedDate")
    private LocalDateTime worked_Date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("workedEndDate")
    private LocalDateTime worked_End_Date;

    @JsonProperty("resultCode")
    private ResultCode resultCode;

    @JsonProperty("caller")
    private String caller;

    @JsonProperty("workedType")
    private String workedType;

    @JsonProperty("campaignId")
    private Long campaignId;

    @JsonProperty("contactId")
    private Long contactId;


    public String getWorkedCode() {
        return workedCode;
    }

    public void setWorkedCode(String workedCode) {
        this.workedCode = workedCode;
    }

    public LocalDateTime getWorked_Date() {
        return worked_Date;
    }

    public void setWorked_Date(LocalDateTime worked_Date) {
        this.worked_Date = worked_Date;
    }

    public LocalDateTime getWorked_End_Date() {
        return worked_End_Date;
    }

    public void setWorked_End_Date(LocalDateTime worked_End_Date) {
        this.worked_End_Date = worked_End_Date;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public void setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getWorkedType() {
        return workedType;
    }

    public void setWorkedType(String workedType) {
        this.workedType = workedType;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    @Override
    public String toString() {
        return "LeadRequest{" +
                "workedCode='" + workedCode + '\'' +
                ", worked_Date=" + worked_Date +
                ", worked_End_Date=" + worked_End_Date +
                ", resultCode=" + (resultCode != null ? resultCode.name() : null) +
                ", caller='" + caller + '\'' +
                ", workedType='" + workedType + '\'' +
                ", campaignId=" + campaignId +
                ", contactId=" + contactId +
                '}';
    }

}



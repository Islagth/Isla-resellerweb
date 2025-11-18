package com.example.enel_bitrix24_integration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;


public class LeadRequest {


    @JsonProperty("workedCode")
    private String workedCode;

     @JsonProperty("workedDate")
    private String workedDate;

    @JsonProperty("workedEndDate")
    private String workedEndDate;
    
    @JsonProperty("resultCode")
    private ResultCode resultCode;  // puoi usare enum se vuoi

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

  public String getWorkedDate() {
        return workedDate;
    }

    public void setWorkedDate(String workedDate) {
        this.workedDate = workedDate;
    }

    public String getWorkedEndDate() {
        return workedEndDate;
    }

    public void setWorkedEndDate(String workedEndDate) {
        this.workedEndDate = workedEndDate;
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
                ", workedDate=" + workedDate +
                ", workedEndDate=" + workedEndDate +
                ", resultCode=" + (resultCode != null ? resultCode.name() : null) +
                ", caller='" + caller + '\'' +
                ", workedType='" + workedType + '\'' +
                ", campaignId=" + campaignId +
                ", contactId=" + contactId +
                '}';
    }

}

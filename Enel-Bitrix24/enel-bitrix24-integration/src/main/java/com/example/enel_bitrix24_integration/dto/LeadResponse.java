package com.example.enel_bitrix24_integration.dto;

import com.fasterxml.jackson.annotation.JsonSetter;


public class LeadResponse {

    private Boolean success;   // Boolean, non boolean!
    private Long workedId;
    private String message;

    @JsonSetter("workedId")
    public void setWorkedId(Long workedId) {
        this.workedId = workedId;

        if (this.success == null && workedId != null) {
            this.success = true;
        }
    }

    @JsonSetter("success")
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getSuccess() {
        return success != null ? success : false;
    }

    public Long getWorkedId() { return workedId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSuccess() {
        return getSuccess();
    }
}



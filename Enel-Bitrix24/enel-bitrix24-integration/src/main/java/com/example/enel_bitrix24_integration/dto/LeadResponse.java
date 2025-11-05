package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

public class LeadResponse {

    private boolean success;
    private Long workedId;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getWorkedId() {
        return workedId;
    }

    public void setWorkedId(Long workedId) {
        this.workedId = workedId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


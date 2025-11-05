package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

public class LottoBlacklistDTO {

    private long id;
    private String creation_date;
    private int size;
    private String confirmation_date;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(String creation_date) {
        this.creation_date = creation_date;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getConfirmation_date() {
        return confirmation_date;
    }

    public void setConfirmation_date(String confirmation_date) {
        this.confirmation_date = confirmation_date;
    }
}

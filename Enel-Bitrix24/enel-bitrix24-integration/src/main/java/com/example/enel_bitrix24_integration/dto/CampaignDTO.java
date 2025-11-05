package com.example.enel_bitrix24_integration.dto;

import lombok.Data;


public class CampaignDTO {

    private int id_campagna;
    private int id_pianificazione_campagna;
    private int id_config_campagna;
    private String pianificazione;
    private String campagna;
    private boolean masked;
    private int pianificate;
    private int scaricate;
    private int scaricabili;
    private Boolean slice_by_tag;

    public int getId_campagna() {
        return id_campagna;
    }

    public void setId_campagna(int id_campagna) {
        this.id_campagna = id_campagna;
    }

    public int getId_pianificazione_campagna() {
        return id_pianificazione_campagna;
    }

    public void setId_pianificazione_campagna(int id_pianificazione_campagna) {
        this.id_pianificazione_campagna = id_pianificazione_campagna;
    }

    public int getId_config_campagna() {
        return id_config_campagna;
    }

    public void setId_config_campagna(int id_config_campagna) {
        this.id_config_campagna = id_config_campagna;
    }

    public String getPianificazione() {
        return pianificazione;
    }

    public void setPianificazione(String pianificazione) {
        this.pianificazione = pianificazione;
    }

    public String getCampagna() {
        return campagna;
    }

    public void setCampagna(String campagna) {
        this.campagna = campagna;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }

    public int getPianificate() {
        return pianificate;
    }

    public void setPianificate(int pianificate) {
        this.pianificate = pianificate;
    }

    public int getScaricate() {
        return scaricate;
    }

    public void setScaricate(int scaricate) {
        this.scaricate = scaricate;
    }

    public int getScaricabili() {
        return scaricabili;
    }

    public void setScaricabili(int scaricabili) {
        this.scaricabili = scaricabili;
    }

    public Boolean getSlice_by_tag() {
        return slice_by_tag;
    }

    public void setSlice_by_tag(Boolean slice_by_tag) {
        this.slice_by_tag = slice_by_tag;
    }
}


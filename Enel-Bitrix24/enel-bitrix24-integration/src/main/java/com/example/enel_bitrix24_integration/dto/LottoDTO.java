package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

public class LottoDTO {

    private String id_lotto;
    private String tipologia_lotto;
    private String data_lotto;
    private int contatti;
    private int anagrafiche;
    private String id_campagna;
    private boolean masked;
    private Boolean slice_by_tag;

    public String getId_lotto() {
        return id_lotto;
    }

    public void setId_lotto(String id_lotto) {
        this.id_lotto = id_lotto;
    }

    public String getTipologia_lotto() {
        return tipologia_lotto;
    }

    public void setTipologia_lotto(String tipologia_lotto) {
        this.tipologia_lotto = tipologia_lotto;
    }

    public String getData_lotto() {
        return data_lotto;
    }

    public void setData_lotto(String data_lotto) {
        this.data_lotto = data_lotto;
    }

    public int getContatti() {
        return contatti;
    }

    public void setContatti(int contatti) {
        this.contatti = contatti;
    }

    public int getAnagrafiche() {
        return anagrafiche;
    }

    public void setAnagrafiche(int anagrafiche) {
        this.anagrafiche = anagrafiche;
    }

    public String getId_campagna() {
        return id_campagna;
    }

    public void setId_campagna(String id_campagna) {
        this.id_campagna = id_campagna;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }

    public Boolean getSlice_by_tag() {
        return slice_by_tag;
    }

    public void setSlice_by_tag(Boolean slice_by_tag) {
        this.slice_by_tag = slice_by_tag;
    }
}

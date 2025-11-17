package com.example.enel_bitrix24_integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class ContactDTO {

    // --- Campi Bitrix24 ---
    @JsonProperty("HONORIFIC")
    private String HONORIFIC;

    @JsonProperty("NAME")
    private String NAME;

    @JsonProperty("SECOND_NAME")
    private String SECOND_NAME;

    @JsonProperty("LAST_NAME")
    private String LAST_NAME;

    @JsonProperty("BIRTHDATE")
    private String BIRTHDATE;

    @JsonProperty("TYPE_ID")
    private String TYPE_ID;

    @JsonProperty("SOURCE_ID")
    private String SOURCE_ID;

    @JsonProperty("SOURCE_DESCRIPTION")
    private String SOURCE_DESCRIPTION;

    @JsonProperty("POST")
    private String POST;

    @JsonProperty("COMMENTS")
    private String COMMENTS;

    @JsonProperty("OPENED")
    private String OPENED;

    @JsonProperty("EXPORT")
    private String EXPORT;

    @JsonProperty("ASSIGNED_BY_ID")
    private Integer ASSIGNED_BY_ID;

    @JsonProperty("COMPANY_ID")
    private Integer COMPANY_ID;

    @JsonProperty("COMPANY_IDS")
    private List<Integer> COMPANY_IDS;

    @JsonProperty("UTM_SOURCE")
    private String UTM_SOURCE;

    @JsonProperty("UTM_MEDIUM")
    private String UTM_MEDIUM;

    @JsonProperty("UTM_CAMPAIGN")
    private String UTM_CAMPAIGN;

    @JsonProperty("UTM_CONTENT")
    private String UTM_CONTENT;

    @JsonProperty("UTM_TERM")
    private String UTM_TERM;

    @JsonProperty("PHONE")
    private List<MultiField> PHONE;

    @JsonProperty("EMAIL")
    private List<MultiField> EMAIL;

    @JsonProperty("WEB")
    private List<MultiField> WEB;

    @JsonProperty("IM")
    private List<MultiField> IM;

    @JsonProperty("LINK")
    private List<MultiField> LINK;

    @JsonProperty("UF")
    private Map<String, Object> UF;

    @JsonProperty("RESULT_CODE")
    private ResultCode RESULT_CODE;

    @JsonProperty("DATE_CREATE")
    private LocalDateTime DATE_CREATE;

    @JsonProperty("DATE_MODIFY")
    private LocalDateTime DATE_MODIFY;

    @JsonProperty("CREATED_BY_ID")
    private Integer CREATED_BY_ID;

    @JsonProperty("MODIFY_BY_ID")
    private Integer MODIFY_BY_ID;

    // --- Campi custom JSON del lotto ---
    @JsonProperty("ID_ANAGRAFICA")
    private Long idAnagrafica;

    @JsonProperty("TELEFONO")
    private String telefono;

    @JsonProperty("EXTERNAL_ID")
    private String externalId;

    @JsonProperty("DATA_SCADENZA")
    private String dataScadenza;


    // --- Post-process conversione ---
    @JsonIgnore
    public void normalizeForBitrix() {
        // Se arriva solo idAnagrafica, usalo come nome
       if (this.idAnagrafica != null && (this.NAME == null || this.NAME.isEmpty())) {
            this.NAME = String.valueOf(this.idAnagrafica);
        }

        // Se arriva telefono, costruisci lista MultiField
        if (this.telefono != null && (this.PHONE == null || this.PHONE.isEmpty())) {
            this.PHONE = List.of(new MultiField(this.telefono, "WORK"));
        }

        // puoi aggiungere altre regole (es. SOURCE_ID default, COMMENTS, ecc.)
    }

    // --- Inner class per multifield ---
     @Data
    public static class MultiField {
        private String VALUE;
        private String VALUE_TYPE;

        public MultiField() {}

        public MultiField(String VALUE, String VALUE_TYPE) {
            this.VALUE = VALUE;
            this.VALUE_TYPE = VALUE_TYPE;
        }
    }

    public String getHONORIFIC() {
        return HONORIFIC;
    }

    public void setHONORIFIC(String HONORIFIC) {
        this.HONORIFIC = HONORIFIC;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getSECOND_NAME() {
        return SECOND_NAME;
    }

    public void setSECOND_NAME(String SECOND_NAME) {
        this.SECOND_NAME = SECOND_NAME;
    }

    public String getLAST_NAME() {
        return LAST_NAME;
    }

    public void setLAST_NAME(String LAST_NAME) {
        this.LAST_NAME = LAST_NAME;
    }

    public String getBIRTHDATE() {
        return BIRTHDATE;
    }

    public void setBIRTHDATE(String BIRTHDATE) {
        this.BIRTHDATE = BIRTHDATE;
    }

    public String getTYPE_ID() {
        return TYPE_ID;
    }

    public void setTYPE_ID(String TYPE_ID) {
        this.TYPE_ID = TYPE_ID;
    }

    public String getSOURCE_ID() {
        return SOURCE_ID;
    }

    public void setSOURCE_ID(String SOURCE_ID) {
        this.SOURCE_ID = SOURCE_ID;
    }

    public String getSOURCE_DESCRIPTION() {
        return SOURCE_DESCRIPTION;
    }

    public void setSOURCE_DESCRIPTION(String SOURCE_DESCRIPTION) {
        this.SOURCE_DESCRIPTION = SOURCE_DESCRIPTION;
    }

    public String getPOST() {
        return POST;
    }

    public void setPOST(String POST) {
        this.POST = POST;
    }

    public String getCOMMENTS() {
        return COMMENTS;
    }

    public void setCOMMENTS(String COMMENTS) {
        this.COMMENTS = COMMENTS;
    }

    public String getOPENED() {
        return OPENED;
    }

    public void setOPENED(String OPENED) {
        this.OPENED = OPENED;
    }

    public String getEXPORT() {
        return EXPORT;
    }

    public void setEXPORT(String EXPORT) {
        this.EXPORT = EXPORT;
    }

    public Integer getCOMPANY_ID() {
        return COMPANY_ID;
    }

    public void setCOMPANY_ID(Integer COMPANY_ID) {
        this.COMPANY_ID = COMPANY_ID;
    }

    public Integer getASSIGNED_BY_ID() {
        return ASSIGNED_BY_ID;
    }

    public void setASSIGNED_BY_ID(Integer ASSIGNED_BY_ID) {
        this.ASSIGNED_BY_ID = ASSIGNED_BY_ID;
    }

    public List<Integer> getCOMPANY_IDS() {
        return COMPANY_IDS;
    }

    public void setCOMPANY_IDS(List<Integer> COMPANY_IDS) {
        this.COMPANY_IDS = COMPANY_IDS;
    }

    public String getUTM_SOURCE() {
        return UTM_SOURCE;
    }

    public void setUTM_SOURCE(String UTM_SOURCE) {
        this.UTM_SOURCE = UTM_SOURCE;
    }

    public String getUTM_MEDIUM() {
        return UTM_MEDIUM;
    }

    public void setUTM_MEDIUM(String UTM_MEDIUM) {
        this.UTM_MEDIUM = UTM_MEDIUM;
    }

    public String getUTM_CAMPAIGN() {
        return UTM_CAMPAIGN;
    }

    public void setUTM_CAMPAIGN(String UTM_CAMPAIGN) {
        this.UTM_CAMPAIGN = UTM_CAMPAIGN;
    }

    public String getUTM_CONTENT() {
        return UTM_CONTENT;
    }

    public void setUTM_CONTENT(String UTM_CONTENT) {
        this.UTM_CONTENT = UTM_CONTENT;
    }

    public String getUTM_TERM() {
        return UTM_TERM;
    }

    public void setUTM_TERM(String UTM_TERM) {
        this.UTM_TERM = UTM_TERM;
    }

    public List<MultiField> getPHONE() {
        return PHONE;
    }

    public void setPHONE(List<MultiField> PHONE) {
        this.PHONE = PHONE;
    }

    public List<MultiField> getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(List<MultiField> EMAIL) {
        this.EMAIL = EMAIL;
    }

    public List<MultiField> getWEB() {
        return WEB;
    }

    public void setWEB(List<MultiField> WEB) {
        this.WEB = WEB;
    }

    public List<MultiField> getIM() {
        return IM;
    }

    public void setIM(List<MultiField> IM) {
        this.IM = IM;
    }

    public List<MultiField> getLINK() {
        return LINK;
    }

    public void setLINK(List<MultiField> LINK) {
        this.LINK = LINK;
    }

    public ResultCode getRESULT_CODE() {
        return RESULT_CODE;
    }

    public void setRESULT_CODE(ResultCode RESULT_CODE) {
        this.RESULT_CODE = RESULT_CODE;
    }

    public Map<String, Object> getUF() {
        return UF;
    }

    public void setUF(Map<String, Object> UF) {
        this.UF = UF;
    }

    public LocalDateTime getDATE_CREATE() {
        return DATE_CREATE;
    }

    public void setDATE_CREATE(LocalDateTime DATE_CREATE) {
        this.DATE_CREATE = DATE_CREATE;
    }

    public LocalDateTime getDATE_MODIFY() {
        return DATE_MODIFY;
    }

    public void setDATE_MODIFY(LocalDateTime DATE_MODIFY) {
        this.DATE_MODIFY = DATE_MODIFY;
    }

    public Integer getCREATED_BY_ID() {
        return CREATED_BY_ID;
    }

    public void setCREATED_BY_ID(Integer CREATED_BY_ID) {
        this.CREATED_BY_ID = CREATED_BY_ID;
    }

    public Integer getMODIFY_BY_ID() {
        return MODIFY_BY_ID;
    }

    public void setMODIFY_BY_ID(Integer MODIFY_BY_ID) {
        this.MODIFY_BY_ID = MODIFY_BY_ID;
    }

     public Long getIdAnagrafica() {
        return idAnagrafica;
    }

    public void setIdAnagrafica(Long idAnagrafica) {
        this.idAnagrafica = idAnagrafica;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDataScadenza() {
        return dataScadenza;
    }

    public void setDataScadenza(String dataScadenza) {
        this.dataScadenza = dataScadenza;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }    

 
}



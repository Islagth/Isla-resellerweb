package com.example.enel_bitrix24_integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ContactDTO {

    // --- Campi Bitrix24 ---
    private String HONORIFIC;
    private String NAME;
    private String SECOND_NAME;
    private String LAST_NAME;
    private String BIRTHDATE;
    private String TYPE_ID;
    private String SOURCE_ID;
    private String SOURCE_DESCRIPTION;
    private String POST;
    private String COMMENTS;
    private String OPENED;
    private String EXPORT;
    private Integer ASSIGNED_BY_ID;
    private Integer COMPANY_ID;
    private List<Integer> COMPANY_IDS;
    private String UTM_SOURCE;
    private String UTM_MEDIUM;
    private String UTM_CAMPAIGN;
    private String UTM_CONTENT;
    private String UTM_TERM;
    private List<MultiField> PHONE;
    private List<MultiField> EMAIL;
    private List<MultiField> WEB;
    private List<MultiField> IM;
    private List<MultiField> LINK;
    private Map<String, Object> UF;
    private String RESULT_CODE;
    private Date DATE_CREATE;
    private Date DATE_MODIFY;
    private Integer CREATED_BY_ID;
    private Integer MODIFY_BY_ID;

    // --- Campi custom JSON del lotto ---
    @JsonProperty("idAnagrafica")
    private String idAnagrafica;

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("dataScadenza")
    private String dataScadenza;

    // --- Post-process conversione ---
    @JsonIgnore
    public void normalizeForBitrix() {
        // Se arriva solo idAnagrafica, usalo come nome
        if (this.idAnagrafica != null && (this.NAME == null || this.NAME.isEmpty())) {
            this.NAME = this.idAnagrafica;
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
}


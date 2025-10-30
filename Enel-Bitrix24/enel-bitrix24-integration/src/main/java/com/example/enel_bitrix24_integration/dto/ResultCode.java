package com.example.enel_bitrix24_integration.dto;

public enum ResultCode {

    // KO
    D109("KO - Gia' contattato"),
    D106("KO - Ha cambiato da poco"),
    D103("KO - Migliori opportunita' dal competitor"),
    D102("KO - Non interessato"),
    D105("KO - Numero inesistente (Occupato Veloce)"),
    D101("KO - Rifiuta dialogo"),
    D107("KO - Titolare della fornitura non disponibile"),
    D108("KO - Vendita ingannevole da Enel"),

    // OK
    D201("OK - Accetta l'offerta con attivazione a distanza"),

    // WIP
    D003("WIP - Appuntamento Generico"),
    D004("WIP - Appuntamento Personale"),
    D010("WIP - Chiamata muta"),
    D001("WIP - Non Risponde / Occupato"),
    D002("WIP - Segreteria / Fax"),
    UNKNOWN("nullo");

    private String esito;

    ResultCode(String esito) {
        this.esito = esito;
    }

    public static ResultCode fromString(String value) {
        if (value == null) return ResultCode.UNKNOWN;
        try {
            return ResultCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResultCode.UNKNOWN;
        }
    }

    public String getEsito() {
        return esito;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public static ResultCode fromCode(String code) {
        for (ResultCode rc : values()) {
            if (rc.name().equals(code)) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Codice risultato non valido: " + code);
    }
}


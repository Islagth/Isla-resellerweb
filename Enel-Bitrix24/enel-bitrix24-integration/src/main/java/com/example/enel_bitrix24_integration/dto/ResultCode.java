package com.example.enel_bitrix24_integration.dto;

public enum ResultCode {

    // KO
    S109("KO - Gia' contattato"),
    S106("KO - Ha cambiato da poco"),
    S103("KO - Migliori opportunita' dal competitor"),
    S102("KO - Non Interessato"),
    S105("KO - Numero Inesistente (Occupato Veloce)"),
    S104("KO - Opposizione chiamate commerciali"),
    S101("KO - Rifiuta Dialogo"),
    S107("KO - Titolare della fornitura non disponibile"),
    S108("KO - Vendita ingannevole da Enel"),

    // OK
    S202("OK - Accetta l'offerta, Attiva in Negozio"),
    S201("OK - Accetta l'offerta con attivazione a distanza"),

    // WIP
    S003("WIP - Appuntamento Generico"),
    S004("WIP - Appuntamento Personale"),
    S001("WIP - Non Risponde / Occupato"),
    S002("WIP - Segreteria / Fax");

    private String esito;

    ResultCode(String esito) {
        this.esito = esito;
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


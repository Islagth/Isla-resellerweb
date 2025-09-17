package com.example.enel_bitrix24_integration.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EsitoTelefonata {

    KO_GIA_CONTATTATO("KO - Gia' contattato"),
    KO_CAMBIATO_DA_POCO("KO - Ha cambiato da poco"),
    KO_COMPETITOR("KO - Migliori opportunita' dal competitor"),
    KO_NON_INTERESSATO("KO - Non Interessato"),
    KO_NUMERO_INESISTENTE("KO - Numero Inesistente (Occupato Veloce)"),
    KO_OPPOSIZIONE("KO - Opposizione chiamate commerciali"),
    KO_RIFIUTA_DIALOGO("KO - Rifiuta Dialogo"),
    KO_TITOLARE_NON_DISPONIBILE("KO - Titolare della fornitura non disponibile"),
    KO_VENDITA_INGANNEVOLE("KO - Vendita ingannevole da Enel"),

    OK_NEGOZIO("OK - Accetta l'offerta, Attiva in Negozio"),
    OK_A_DISTANZA("OK - Accetta l'offerta con attivazione a distanza"),

    WIP_APPUNTAMENTO_GENERICO("WIP - Appuntamento Generico"),
    WIP_APPUNTAMENTO_PERSONALE("WIP - Appuntamento Personale"),
    WIP_NON_RISPONDE("WIP - Non Risponde / Occupato"),
    WIP_SEGRETERIA("WIP - Segreteria / Fax");

    private final String esitoTelefonata;

    EsitoTelefonata(String esitoTelefonata) {
        this.esitoTelefonata = esitoTelefonata;

    }

    @JsonValue
    public String getEsitoTelefonata() {
        return esitoTelefonata;
    }

    public String setEsitoTelefonata(String esitoTelefonata) {
        return esitoTelefonata;
    };
}

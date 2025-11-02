package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Data
public class DealDTO {

    // === Identificativi ===
    private Integer id;                 // ID Bitrix (opzionale per update)
    private String idAnagrafica;        // ID anagrafica per collegamento con contatto

    // === Campi principali ===
    private String title;               // TITLE
    private String typeId;              // TYPE_ID
    private Integer categoryId;         // CATEGORY_ID
    private String stageId;             // STAGE_ID
    private String stageSemanticId;     // STAGE_SEMANTIC_ID

    // === Flag logici ===
    private String isNew;               // Y/N
    private String isRecurring;         // IS_RECURRING (Y/N)
    private String isReturnCustomer;    // IS_RETURN_CUSTOMER (Y/N)
    private String isRepeatedApproach;  // IS_REPEATED_APPROACH (Y/N)

    // === Dati economici ===
    private Integer probability;        // PROBABILITY %
    private String currencyId;          // CURRENCY_ID
    private Double opportunity;         // OPPORTUNITY (importo)
    private String isManualOpportunity; // IS_MANUAL_OPPORTUNITY (Y/N)
    private Double taxValue;            // TAX_VALUE

    // === Relazioni ===
    private Integer companyId;          // COMPANY_ID
    private Integer contactId;          // CONTACT_ID (deprecated)
    private List<Integer> contactIds;   // CONTACT_IDS (lista di contatti)
    private Map<String, Integer> parentIdSPAs; // PARENT_ID_{id} relazione SPA

    // === Date ===
    private LocalDate beginDate;        // BEGINDATE (ISO date)
    private LocalDate closeDate;        // CLOSEDATE (ISO date)

    // === Stato ===
    private String opened;              // OPENED (Y/N)
    private String closed;              // CLOSED (Y/N)
    private String comments;            // COMMENTS

    // === Assegnazione ===
    private Integer assignedById;       // ASSIGNED_BY_ID

    // === Origine lead ===
    private String sourceId;            // SOURCE_ID
    private String sourceDescription;   // SOURCE_DESCRIPTION

    // === Marketing ===
    private String utmSource;           // UTM_SOURCE
    private String utmMedium;           // UTM_MEDIUM
    private String utmCampaign;         // UTM_CAMPAIGN
    private String utmContent;          // UTM_CONTENT
    private String utmTerm;             // UTM_TERM
    private String trace;               // TRACE

    // === Altri ===
    private String additionalInfo;      // ADDITIONAL_INFO
    private Integer locationId;         // LOCATION_ID
    private String originatorId;        // ORIGINATOR_ID
    private String originId;            // ORIGIN_ID
    private String parentIdSPA;         // Eventuale singolo campo parent

    // === Campi custom UF_CRM_ ===
    private String ufCrmCustomField;    // esempio singolo
    private Map<String, Object> customFields; // mappa di campi personalizzati

    // === Costruttori ===
    public DealDTO() {}

    public DealDTO(Integer id, String title) {
        this.id = id;
        this.title = title;
    }
}

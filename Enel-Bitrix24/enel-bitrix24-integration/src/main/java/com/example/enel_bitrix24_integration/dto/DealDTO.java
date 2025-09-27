package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DealDTO {

    private Integer id;                        // ID opzionale, usato solo per update
    private String title;                       // TITLE
    private String typeId;                      // TYPE_ID
    private Integer categoryId;                 // CATEGORY_ID
    private String stageId;                     // STAGE_ID
    private String stageSemanticId;           // STAGE_SEMANTIC_ID
    private String isNew;
    private String isRecurring;                 // IS_RECURRING (Y/N)
    private String isReturnCustomer;            // IS_RETURN_CUSTOMER (Y/N)
    private String isRepeatedApproach;          // IS_REPEATED_APPROACH (Y/N)
    private Integer probability;                // PROBABILITY %
    private String currencyId;                  // CURRENCY_ID
    private Double opportunity;                 // OPPORTUNITY
    private String isManualOpportunity;         // IS_MANUAL_OPPORTUNITY (Y/N)
    private Double taxValue;                    // TAX_VALUE
    private Integer companyId;                  // COMPANY_ID
    private Integer contactId;                  // CONTACT_ID (deprecated)
    private List<Integer> contactIds;           // CONTACT_IDS (lista di contatti)
    private String beginDate;                   // BEGINDATE (ISO date)
    private String closeDate;                   // CLOSEDATE (ISO date)
    private String opened;                      // OPENED (Y/N)
    private String closed;                      // CLOSED (Y/N)
    private String comments;                    // COMMENTS
    private Integer assignedById;               // ASSIGNED_BY_ID
    private String sourceId;                    // SOURCE_ID
    private String sourceDescription;           // SOURCE_DESCRIPTION
    private String additionalInfo;               // ADDITIONAL_INFO
    private Integer locationId;                 // LOCATION_ID
    private String originatorId;                // ORIGINATOR_ID
    private String originId;                    // ORIGIN_ID
    private String utmSource;                   // UTM_SOURCE
    private String utmMedium;                   // UTM_MEDIUM
    private String utmCampaign;                 // UTM_CAMPAIGN
    private String utmContent;                  // UTM_CONTENT
    private String utmTerm;                     // UTM_TERM
    private String trace;                       // TRACE
    private String parentIdSPA;                 // PARENT_ID_{id} (creare campo specifico se serve)
    // Per i campi UF_CRM_ custom, usare mappa o singoli campi specifici
    private String ufCrmCustomField;
    // Mappa per campi personalizzati UF_CRM_
    private Map<String, Object> customFields;
    private Map<String, Integer> parentIdSPAs; // PARENT_ID_{id} relazione SPA

    public DealDTO() {}

    public DealDTO(int id, String title) {
        this.id = id;
        this.title = title;
    }

}

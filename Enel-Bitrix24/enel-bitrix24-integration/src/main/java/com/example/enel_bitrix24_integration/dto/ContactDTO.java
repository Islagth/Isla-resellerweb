package com.example.enel_bitrix24_integration.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ContactDTO {

    private String HONORIFIC;                  // Salutation (crm_status)
    private String NAME;                      // First name
    private String SECOND_NAME;               // Middle name
    private String LAST_NAME;                 // Last name
    private String BIRTHDATE;                 // yyyy-MM-dd
    private String TYPE_ID;                   // Contact type (crm_status)
    private String SOURCE_ID;                 // Source (crm_status)
    private String SOURCE_DESCRIPTION;       // Additional source info
    private String POST;                      // Position
    private String COMMENTS;                  // Comments BB code supported
    private String OPENED;                    // Y or N (available to everyone)
    private String EXPORT;                    // Y or N (included in export)
    private Integer ASSIGNED_BY_ID;           // Responsible user id
    private Integer COMPANY_ID;                // Main company id
    private List<Integer> COMPANY_IDS;         // Linked companies ids
    private String UTM_SOURCE;                // Advertising source
    private String UTM_MEDIUM;                // Traffic type (CPC, CPM)
    private String UTM_CAMPAIGN;              // Advertising campaign
    private String UTM_CONTENT;               // Campaign content
    private String UTM_TERM;                  // Campaign search term
    private List<MultiField> PHONE;           // Phone numbers
    private List<MultiField> EMAIL;           // Emails
    private List<MultiField> WEB;             // Websites
    private List<MultiField> IM;              // Messengers
    private List<MultiField> LINK;            // Links (service field)
    private Map<String, Object> UF;          // Custom fields UF_CRM_...
    // Relationship fields PARENT_ID_xxx are omitted for brevity (can be added as Map<String,Object> if needed)

    // Import related fields (available when IMPORT = 'Y' in params)
    private Date DATE_CREATE;
    private Date DATE_MODIFY;
    private Integer CREATED_BY_ID;
    private Integer MODIFY_BY_ID;

    // Inner static class for multifield entries like PHONE, EMAIL

    public static class MultiField {
        private String VALUE;
        private String VALUE_TYPE;

        public MultiField() {
        }

        public MultiField(String VALUE, String VALUE_TYPE) {
            this.VALUE = VALUE;
            this.VALUE_TYPE = VALUE_TYPE;
        }
    }
}

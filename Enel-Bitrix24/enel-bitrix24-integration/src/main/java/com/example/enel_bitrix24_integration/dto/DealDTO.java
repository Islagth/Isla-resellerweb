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
    @JsonProperty("ID_ANAGRAFICA")
    private String idAnagrafica;      // ID anagrafica per collegamento con contatto

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

     public Map<String, Object> getRawData() {
        return Map.of();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdAnagrafica() {
        return idAnagrafica;
    }

    public void setIdAnagrafica(String idAnagrafica) {
        this.idAnagrafica = idAnagrafica;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getStageSemanticId() {
        return stageSemanticId;
    }

    public void setStageSemanticId(String stageSemanticId) {
        this.stageSemanticId = stageSemanticId;
    }

    public String getIsNew() {
        return isNew;
    }

    public void setIsNew(String isNew) {
        this.isNew = isNew;
    }

    public String getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(String isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getIsReturnCustomer() {
        return isReturnCustomer;
    }

    public void setIsReturnCustomer(String isReturnCustomer) {
        this.isReturnCustomer = isReturnCustomer;
    }

    public String getIsRepeatedApproach() {
        return isRepeatedApproach;
    }

    public void setIsRepeatedApproach(String isRepeatedApproach) {
        this.isRepeatedApproach = isRepeatedApproach;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public Double getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Double opportunity) {
        this.opportunity = opportunity;
    }

    public String getIsManualOpportunity() {
        return isManualOpportunity;
    }

    public void setIsManualOpportunity(String isManualOpportunity) {
        this.isManualOpportunity = isManualOpportunity;
    }

    public Double getTaxValue() {
        return taxValue;
    }

    public void setTaxValue(Double taxValue) {
        this.taxValue = taxValue;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public List<Integer> getContactIds() {
        return contactIds;
    }

    public void setContactIds(List<Integer> contactIds) {
        this.contactIds = contactIds;
    }

    public Map<String, Integer> getParentIdSPAs() {
        return parentIdSPAs;
    }

    public void setParentIdSPAs(Map<String, Integer> parentIdSPAs) {
        this.parentIdSPAs = parentIdSPAs;
    }

    public LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    public String getClosed() {
        return closed;
    }

    public void setClosed(String closed) {
        this.closed = closed;
    }

    public String getOpened() {
        return opened;
    }

    public void setOpened(String opened) {
        this.opened = opened;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Integer getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(Integer assignedById) {
        this.assignedById = assignedById;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }

    public String getUtmSource() {
        return utmSource;
    }

    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    public String getUtmCampaign() {
        return utmCampaign;
    }

    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }

    public String getUtmMedium() {
        return utmMedium;
    }

    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    public String getUtmContent() {
        return utmContent;
    }

    public void setUtmContent(String utmContent) {
        this.utmContent = utmContent;
    }

    public String getUtmTerm() {
        return utmTerm;
    }

    public void setUtmTerm(String utmTerm) {
        this.utmTerm = utmTerm;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public String getOriginatorId() {
        return originatorId;
    }

    public void setOriginatorId(String originatorId) {
        this.originatorId = originatorId;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public String getParentIdSPA() {
        return parentIdSPA;
    }

    public void setParentIdSPA(String parentIdSPA) {
        this.parentIdSPA = parentIdSPA;
    }

    public String getUfCrmCustomField() {
        return ufCrmCustomField;
    }

    public void setUfCrmCustomField(String ufCrmCustomField) {
        this.ufCrmCustomField = ufCrmCustomField;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}

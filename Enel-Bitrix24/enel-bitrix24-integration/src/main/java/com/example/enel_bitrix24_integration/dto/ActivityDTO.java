package com.example.enel_bitrix24_integration.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityDTO {
    private Long id;
    private Long ownerId;           // ID del deal o contatto
    private Integer ownerTypeId;    // Tipo oggetto CRM (2 = Deal)
    private String typeId;          // Tipo attività (CALL, MEETING, TASK, ecc.)
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime deadline;
    private LocalDateTime dateModify;
    private String subject;
    private String status;
    private Long responsibleId;
}


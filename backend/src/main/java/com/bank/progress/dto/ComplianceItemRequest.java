package com.bank.progress.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class ComplianceItemRequest {
    private String nodeId;
    private String docType;
    private Boolean isKey;
    private LocalDate requiredAt;
    private LocalDate submittedAt;
    private String status;
    private Integer overdueDays;
    private String attachmentUrl;
    private Map<String, Object> meta;
}

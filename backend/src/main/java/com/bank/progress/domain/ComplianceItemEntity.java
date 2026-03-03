package com.bank.progress.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "compliance_items")
@Getter
@Setter
public class ComplianceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "is_key", nullable = false)
    private Boolean isKey = false;

    @Column(name = "required_at", nullable = false)
    private LocalDate requiredAt;

    @Column(name = "submitted_at")
    private LocalDate submittedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "overdue_days", nullable = false)
    private Integer overdueDays = 0;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String meta;
}

package com.bank.progress.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "warnings")
@Getter
@Setter
public class WarningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Double deviation;

    @Column(nullable = false)
    private String reason;

    @Column(name = "deviation_top_factors", columnDefinition = "jsonb", nullable = false)
    private String deviationTopFactors;

    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String assignees;

    @Column(name = "action_log", columnDefinition = "jsonb", nullable = false)
    private String actionLog;

    @PrePersist
    public void prePersist() {
        if (triggeredAt == null) {
            triggeredAt = LocalDateTime.now();
        }
    }
}

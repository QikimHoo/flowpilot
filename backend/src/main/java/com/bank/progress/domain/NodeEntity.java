package com.bank.progress.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nodes")
@Getter
@Setter
public class NodeEntity {

    @Id
    private String id;

    @Column(name = "node_type", nullable = false)
    private String nodeType;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "owner_user_id")
    private String ownerUserId;

    @Column(name = "owner_dept")
    private String ownerDept;

    @Column(name = "parent_id")
    private String parentId;

    @Column(nullable = false)
    private String path;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "as_of_date")
    private LocalDate asOfDate;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String weights;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String plan;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String actual;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String computed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

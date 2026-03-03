package com.bank.progress.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class NodeUpsertRequest {
    private String id;
    private String nodeType;
    private String nodeName;
    private String ownerUserId;
    private String ownerDept;
    private String parentId;
    private Integer sortOrder;
    private LocalDate asOfDate;
    private Map<String, Object> weights;
    private Map<String, Object> plan;
    private Map<String, Object> actual;
}

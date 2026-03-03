package com.bank.progress.service;

import com.bank.progress.domain.ComplianceItemEntity;
import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.UserEntity;
import com.bank.progress.dto.ComplianceItemRequest;
import com.bank.progress.repository.ComplianceItemRepository;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComplianceService {

    private final ComplianceItemRepository complianceItemRepository;
    private final NodeRepository nodeRepository;
    private final AccessControlService accessControlService;
    private final ComputeService computeService;
    private final AuditService auditService;

    public ComplianceService(ComplianceItemRepository complianceItemRepository,
                             NodeRepository nodeRepository,
                             AccessControlService accessControlService,
                             ComputeService computeService,
                             AuditService auditService) {
        this.complianceItemRepository = complianceItemRepository;
        this.nodeRepository = nodeRepository;
        this.accessControlService = accessControlService;
        this.computeService = computeService;
        this.auditService = auditService;
    }

    public List<Map<String, Object>> list(String nodeId, UserEntity user) {
        if (nodeId == null || nodeId.isBlank()) {
            return complianceItemRepository.findAll().stream().map(this::toDto).toList();
        }
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));
        if (!accessControlService.canViewNode(user, node) && !accessControlService.isCompliance(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return complianceItemRepository.findByNodeIdOrderByRequiredAtAsc(nodeId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public Map<String, Object> create(ComplianceItemRequest req, UserEntity user, HttpServletRequest request) {
        if (!accessControlService.canWrite(user) && !accessControlService.isCompliance(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        ComplianceItemEntity item = new ComplianceItemEntity();
        apply(req, item);
        complianceItemRepository.save(item);
        computeService.recalcNode(item.getNodeId());
        auditService.log(user.getUsername(), "COMPLIANCE_CREATE", "COMPLIANCE", String.valueOf(item.getId()), null, toDto(item), request);
        return toDto(item);
    }

    @Transactional
    public Map<String, Object> update(Long id, ComplianceItemRequest req, UserEntity user, HttpServletRequest request) {
        if (!accessControlService.canWrite(user) && !accessControlService.isCompliance(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        ComplianceItemEntity item = complianceItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "compliance item not found"));
        Map<String, Object> before = toDto(item);
        apply(req, item);
        complianceItemRepository.save(item);
        computeService.recalcNode(item.getNodeId());
        auditService.log(user.getUsername(), "COMPLIANCE_UPDATE", "COMPLIANCE", String.valueOf(id), before, toDto(item), request);
        return toDto(item);
    }

    private void apply(ComplianceItemRequest req, ComplianceItemEntity item) {
        item.setNodeId(req.getNodeId());
        item.setDocType(req.getDocType());
        item.setIsKey(Boolean.TRUE.equals(req.getIsKey()));
        item.setRequiredAt(req.getRequiredAt() == null ? LocalDate.now() : req.getRequiredAt());
        item.setSubmittedAt(req.getSubmittedAt());
        item.setStatus(req.getStatus() == null ? "PENDING" : req.getStatus());
        item.setAttachmentUrl(req.getAttachmentUrl());
        item.setMeta(JsonUtil.write(req.getMeta()));

        if (req.getOverdueDays() != null) {
            item.setOverdueDays(req.getOverdueDays());
        } else {
            item.setOverdueDays(calcOverdueDays(item.getRequiredAt(), item.getSubmittedAt()));
        }
    }

    private int calcOverdueDays(LocalDate requiredAt, LocalDate submittedAt) {
        LocalDate compareDate = submittedAt == null ? LocalDate.now() : submittedAt;
        if (compareDate.isAfter(requiredAt)) {
            return (int) requiredAt.until(compareDate).getDays();
        }
        return 0;
    }

    private Map<String, Object> toDto(ComplianceItemEntity item) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", item.getId());
        dto.put("nodeId", item.getNodeId());
        dto.put("docType", item.getDocType());
        dto.put("isKey", item.getIsKey());
        dto.put("requiredAt", item.getRequiredAt());
        dto.put("submittedAt", item.getSubmittedAt());
        dto.put("status", item.getStatus());
        dto.put("overdueDays", item.getOverdueDays());
        dto.put("attachmentUrl", item.getAttachmentUrl());
        dto.put("meta", JsonUtil.readMap(item.getMeta()));
        return dto;
    }
}

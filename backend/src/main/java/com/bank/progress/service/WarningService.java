package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.UserEntity;
import com.bank.progress.domain.WarningEntity;
import com.bank.progress.domain.WarningStatus;
import com.bank.progress.dto.WarningActionRequest;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.repository.WarningRepository;
import com.bank.progress.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WarningService {

    private final WarningRepository warningRepository;
    private final NodeRepository nodeRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    public WarningService(WarningRepository warningRepository,
                          NodeRepository nodeRepository,
                          AccessControlService accessControlService,
                          AuditService auditService) {
        this.warningRepository = warningRepository;
        this.nodeRepository = nodeRepository;
        this.accessControlService = accessControlService;
        this.auditService = auditService;
    }

    public List<Map<String, Object>> list(UserEntity user,
                                          String level,
                                          String status,
                                          String rootId,
                                          String owner) {
        List<WarningEntity> all = warningRepository.findAll();

        String rootPath = null;
        if (rootId != null && !rootId.isBlank()) {
            rootPath = nodeRepository.findById(rootId).map(NodeEntity::getPath)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "root not found"));
        }

        String finalRootPath = rootPath;
        return all.stream()
                .filter(w -> level == null || w.getLevel().equalsIgnoreCase(level))
                .filter(w -> status == null || w.getStatus().equalsIgnoreCase(status))
                .filter(w -> {
                    NodeEntity node = nodeRepository.findById(w.getNodeId()).orElse(null);
                    if (node == null || !accessControlService.canViewNode(user, node)) {
                        return false;
                    }
                    if (finalRootPath != null && !node.getPath().startsWith(finalRootPath)) {
                        return false;
                    }
                    if (owner != null && !owner.equals(node.getOwnerUserId())) {
                        return false;
                    }
                    return true;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> get(Long id, UserEntity user) {
        WarningEntity warning = warningRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "warning not found"));
        NodeEntity node = nodeRepository.findById(warning.getNodeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));
        if (!accessControlService.canViewNode(user, node)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return toDto(warning);
    }

    @Transactional
    public Map<String, Object> action(Long id, WarningActionRequest req, UserEntity user, HttpServletRequest request) {
        WarningEntity warning = warningRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "warning not found"));

        Map<String, Object> before = toDto(warning);
        if (req.getStatus() != null) {
            warning.setStatus(req.getStatus());
            if (WarningStatus.RESOLVED.name().equals(req.getStatus()) || WarningStatus.CLOSED.name().equals(req.getStatus())) {
                warning.setResolvedAt(LocalDateTime.now());
            }
        }

        List<Map<String, Object>> actions = JsonUtil.read(warning.getActionLog(), List.class);
        if (actions == null) {
            actions = new ArrayList<>();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("at", LocalDateTime.now().toString());
        row.put("operator", user.getUsername());
        row.put("note", req.getNote());
        row.put("attachment", req.getAttachment());
        actions.add(row);
        warning.setActionLog(JsonUtil.write(actions));

        warningRepository.save(warning);
        auditService.log(user.getUsername(), "WARNING_ACTION", "WARNING", String.valueOf(id), before, toDto(warning), request);
        return toDto(warning);
    }

    private Map<String, Object> toDto(WarningEntity warning) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", warning.getId());
        dto.put("nodeId", warning.getNodeId());
        dto.put("level", warning.getLevel());
        dto.put("status", warning.getStatus());
        dto.put("deviation", warning.getDeviation());
        dto.put("reason", warning.getReason());
        dto.put("deviationTopFactors", JsonUtil.read(warning.getDeviationTopFactors(), List.class));
        dto.put("slaDueAt", warning.getSlaDueAt());
        dto.put("triggeredAt", warning.getTriggeredAt());
        dto.put("resolvedAt", warning.getResolvedAt());
        dto.put("assignees", JsonUtil.read(warning.getAssignees(), List.class));
        dto.put("actionLog", JsonUtil.read(warning.getActionLog(), List.class));
        return dto;
    }
}

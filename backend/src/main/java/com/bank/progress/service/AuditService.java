package com.bank.progress.service;

import com.bank.progress.domain.AuditLogEntity;
import com.bank.progress.repository.AuditLogRepository;
import com.bank.progress.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String userId,
                    String action,
                    String entityType,
                    String entityId,
                    Object before,
                    Object after,
                    HttpServletRequest request) {
        AuditLogEntity log = new AuditLogEntity();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBefore(JsonUtil.write(before));
        log.setAfter(JsonUtil.write(after));
        log.setDiffSummary(diffSummary(before, after));
        if (request != null) {
            log.setIp(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        auditLogRepository.save(log);
    }

    private String diffSummary(Object before, Object after) {
        if (before == null || after == null) {
            return "created_or_deleted";
        }
        Map<String, Object> beforeMap = JsonUtil.mapper().convertValue(before, Map.class);
        Map<String, Object> afterMap = JsonUtil.mapper().convertValue(after, Map.class);
        Set<String> keys = new TreeSet<>(beforeMap.keySet());
        keys.addAll(afterMap.keySet());
        return keys.stream()
                .filter(k -> !Objects.equals(beforeMap.get(k), afterMap.get(k)))
                .sorted()
                .collect(Collectors.joining(","));
    }
}

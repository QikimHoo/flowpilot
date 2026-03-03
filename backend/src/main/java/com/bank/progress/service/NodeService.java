package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.UserEntity;
import com.bank.progress.dto.MoveNodeRequest;
import com.bank.progress.dto.NodeUpsertRequest;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;
    private final AccessControlService accessControlService;
    private final ComputeService computeService;
    private final AuditService auditService;

    public NodeService(NodeRepository nodeRepository,
                       AccessControlService accessControlService,
                       ComputeService computeService,
                       AuditService auditService) {
        this.nodeRepository = nodeRepository;
        this.accessControlService = accessControlService;
        this.computeService = computeService;
        this.auditService = auditService;
    }

    public Object getTree(String rootId, UserEntity user) {
        List<NodeEntity> all = nodeRepository.findAllByOrderByPathAsc();
        Map<String, NodeEntity> byId = all.stream().collect(Collectors.toMap(NodeEntity::getId, n -> n));

        String rootPath = null;
        if (rootId != null && !rootId.isBlank()) {
            NodeEntity root = byId.get(rootId);
            if (root == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "root not found");
            }
            if (!accessControlService.canViewNode(user, root)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
            }
            rootPath = root.getPath();
        }

        String finalRootPath = rootPath;
        List<NodeEntity> visible = all.stream()
                .filter(n -> accessControlService.canViewNode(user, n))
                .filter(n -> finalRootPath == null || n.getPath().startsWith(finalRootPath))
                .toList();

        Map<String, Map<String, Object>> dtoMap = new LinkedHashMap<>();
        for (NodeEntity node : visible) {
            dtoMap.put(node.getId(), toDto(node, accessControlService.isCompliance(user)));
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (NodeEntity node : visible) {
            Map<String, Object> dto = dtoMap.get(node.getId());
            String parentId = node.getParentId();
            if (parentId != null && dtoMap.containsKey(parentId)) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) dtoMap.get(parentId)
                        .computeIfAbsent("children", k -> new ArrayList<>());
                children.add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }

    public Map<String, Object> getNode(String nodeId, UserEntity user) {
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));
        if (!accessControlService.canViewNode(user, node)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return toDto(node, accessControlService.isCompliance(user));
    }

    @Transactional
    public Map<String, Object> createNode(NodeUpsertRequest req, UserEntity user, HttpServletRequest request) {
        if (!accessControlService.canWrite(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        if (req.getId() == null || req.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required");
        }
        if (nodeRepository.existsById(req.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "node id exists");
        }

        NodeEntity node = new NodeEntity();
        node.setId(req.getId());
        node.setNodeType(valueOr(req.getNodeType(), "req"));
        node.setNodeName(valueOr(req.getNodeName(), req.getId()));
        node.setOwnerUserId(req.getOwnerUserId());
        node.setOwnerDept(req.getOwnerDept());
        node.setParentId(req.getParentId());
        node.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        node.setAsOfDate(req.getAsOfDate() == null ? LocalDate.now() : req.getAsOfDate());
        if (req.getWeights() != null) {
            validateWeights(req.getWeights());
        }
        node.setWeights(JsonUtil.write(req.getWeights()));
        node.setPlan(JsonUtil.write(req.getPlan()));
        node.setActual(JsonUtil.write(req.getActual()));
        node.setComputed(JsonUtil.write(Map.of()));
        node.setPath(computePath(node.getParentId(), node.getId()));

        nodeRepository.save(node);
        computeService.recalcNode(node.getId());
        NodeEntity after = nodeRepository.findById(node.getId()).orElseThrow();
        auditService.log(user.getUsername(), "NODE_CREATE", "NODE", node.getId(), null, toDto(after, false), request);
        return toDto(after, false);
    }

    @Transactional
    public Map<String, Object> updateNode(String nodeId, NodeUpsertRequest req, UserEntity user, HttpServletRequest request) {
        if (!accessControlService.canWrite(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));

        Map<String, Object> before = toDto(node, false);
        if (req.getNodeName() != null) {
            node.setNodeName(req.getNodeName());
        }
        if (req.getOwnerUserId() != null) {
            node.setOwnerUserId(req.getOwnerUserId());
        }
        if (req.getOwnerDept() != null) {
            node.setOwnerDept(req.getOwnerDept());
        }
        if (req.getAsOfDate() != null) {
            node.setAsOfDate(req.getAsOfDate());
        }
        if (req.getSortOrder() != null) {
            node.setSortOrder(req.getSortOrder());
        }
        if (req.getWeights() != null) {
            validateWeights(req.getWeights());
            node.setWeights(JsonUtil.write(req.getWeights()));
        }
        if (req.getPlan() != null) {
            node.setPlan(JsonUtil.write(req.getPlan()));
        }
        if (req.getActual() != null) {
            node.setActual(JsonUtil.write(req.getActual()));
        }

        nodeRepository.save(node);
        computeService.recalcNode(node.getId());
        NodeEntity after = nodeRepository.findById(node.getId()).orElseThrow();
        auditService.log(user.getUsername(), "NODE_UPDATE", "NODE", nodeId, before, toDto(after, false), request);
        return toDto(after, false);
    }

    @Transactional
    public Map<String, Object> moveNode(String nodeId, MoveNodeRequest req, UserEntity user, HttpServletRequest request) {
        if (!accessControlService.canWrite(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));
        String oldPath = node.getPath();
        String newPath = computePath(req.getNewParentId(), node.getId());
        Map<String, Object> before = toDto(node, false);

        node.setParentId(req.getNewParentId());
        node.setSortOrder(req.getNewOrder() == null ? node.getSortOrder() : req.getNewOrder());
        node.setPath(newPath);
        nodeRepository.save(node);

        List<NodeEntity> descendants = nodeRepository.findByPathStartingWithOrderByPathAsc(oldPath + "/");
        for (NodeEntity child : descendants) {
            child.setPath(newPath + child.getPath().substring(oldPath.length()));
            nodeRepository.save(child);
        }

        computeService.recalcNode(node.getId());
        Map<String, Object> after = toDto(nodeRepository.findById(nodeId).orElseThrow(), false);
        auditService.log(user.getUsername(), "NODE_MOVE", "NODE", nodeId, before, after, request);
        return after;
    }

    @Transactional
    public Map<String, Object> importNode(Map<String, Object> payload, UserEntity user, HttpServletRequest request) {
        if (!accessControlService.canWrite(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        String nodeId = String.valueOf(payload.get("nodeId"));
        if (nodeId == null || "null".equals(nodeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nodeId required");
        }
        saveTreeFromImport(payload, null);
        computeService.recalcNode(nodeId);
        NodeEntity node = nodeRepository.findById(nodeId).orElseThrow();
        auditService.log(user.getUsername(), "NODE_IMPORT", "NODE", nodeId, null, toDto(node, false), request);
        return toDto(node, false);
    }

    public Map<String, Object> exportNode(String nodeId, UserEntity user) {
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));
        if (!accessControlService.canViewNode(user, node)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return exportRecursive(node);
    }

    private Map<String, Object> exportRecursive(NodeEntity node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nodeId", node.getId());
        m.put("nodeType", node.getNodeType());
        m.put("nodeName", node.getNodeName());
        m.put("owner", Map.of(
                "dept", valueOr(node.getOwnerDept(), ""),
                "userId", valueOr(node.getOwnerUserId(), ""),
                "person", valueOr(node.getOwnerUserId(), "")
        ));
        m.put("asOfDate", node.getAsOfDate() == null ? null : node.getAsOfDate().toString());
        m.put("weights", JsonUtil.readMap(node.getWeights()));
        m.put("plan", JsonUtil.readMap(node.getPlan()));
        m.put("actual", JsonUtil.readMap(node.getActual()));
        m.put("computed", JsonUtil.readMap(node.getComputed()));
        List<Map<String, Object>> children = nodeRepository.findByParentIdOrderBySortOrderAsc(node.getId())
                .stream().map(this::exportRecursive).toList();
        m.put("children", children);
        return m;
    }

    private void saveTreeFromImport(Map<String, Object> m, String parentId) {
        String nodeId = String.valueOf(m.get("nodeId"));
        NodeEntity node = nodeRepository.findById(nodeId).orElseGet(NodeEntity::new);
        node.setId(nodeId);
        node.setNodeType(valueOr((String) m.get("nodeType"), "req"));
        node.setNodeName(valueOr((String) m.get("nodeName"), nodeId));

        Object owner = m.get("owner");
        if (owner instanceof Map<?, ?> ownerMap) {
            Object dept = ownerMap.containsKey("dept") ? ownerMap.get("dept") : "";
            Object userId = ownerMap.containsKey("userId") ? ownerMap.get("userId") : "";
            node.setOwnerDept(String.valueOf(dept));
            node.setOwnerUserId(String.valueOf(userId));
        }
        node.setParentId(parentId);
        node.setPath(computePath(parentId, nodeId));
        node.setSortOrder(0);
        Object asOfDate = m.get("asOfDate");
        if (asOfDate != null && !String.valueOf(asOfDate).isBlank()) {
            node.setAsOfDate(LocalDate.parse(String.valueOf(asOfDate)));
        }
        if (m.get("weights") instanceof Map<?, ?> rawWeights) {
            validateWeights((Map<String, Object>) rawWeights);
        }
        node.setWeights(JsonUtil.write(m.get("weights")));
        node.setPlan(JsonUtil.write(m.get("plan")));
        node.setActual(JsonUtil.write(m.get("actual")));
        node.setComputed(JsonUtil.write(m.get("computed")));
        nodeRepository.save(node);

        Object children = m.get("children");
        if (children instanceof List<?> list) {
            int order = 0;
            for (Object child : list) {
                if (child instanceof Map<?, ?> childMap) {
                    Map<String, Object> map = (Map<String, Object>) childMap;
                    map.put("sortOrder", order++);
                    saveTreeFromImport(map, nodeId);
                }
            }
        }
    }

    private Map<String, Object> toDto(NodeEntity node, boolean masked) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", node.getId());
        dto.put("nodeType", node.getNodeType());
        dto.put("nodeName", node.getNodeName());
        dto.put("owner", Map.of(
                "userId", valueOr(node.getOwnerUserId(), ""),
                "dept", valueOr(node.getOwnerDept(), "")
        ));
        dto.put("parentId", node.getParentId());
        dto.put("path", node.getPath());
        dto.put("sortOrder", node.getSortOrder());
        dto.put("asOfDate", node.getAsOfDate());
        dto.put("weights", JsonUtil.readMap(node.getWeights()));

        if (!masked) {
            dto.put("plan", JsonUtil.readMap(node.getPlan()));
            dto.put("actual", JsonUtil.readMap(node.getActual()));
        } else {
            dto.put("plan", Map.of("kpi", Map.of("compliance", extractDim(JsonUtil.readMap(node.getPlan()), "compliance"))));
            dto.put("actual", Map.of("kpi", Map.of("compliance", extractDim(JsonUtil.readMap(node.getActual()), "compliance"))));
        }

        dto.put("computed", JsonUtil.readMap(node.getComputed()));
        dto.put("children", new ArrayList<>());
        return dto;
    }

    private Object extractDim(Map<String, Object> map, String dim) {
        Object kpi = map.get("kpi");
        if (kpi instanceof Map<?, ?> kpiMap) {
            return kpiMap.containsKey(dim) ? kpiMap.get(dim) : Map.of();
        }
        return Map.of();
    }

    private String computePath(String parentId, String nodeId) {
        if (parentId == null || parentId.isBlank()) {
            return "/" + nodeId;
        }
        NodeEntity parent = nodeRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "parent not found"));
        return parent.getPath() + "/" + nodeId;
    }

    private void validateWeights(Map<String, Object> weights) {
        double sum = weights.values().stream()
                .map(v -> {
                    if (v instanceof Number n) {
                        return n.doubleValue();
                    }
                    return Double.parseDouble(String.valueOf(v));
                })
                .mapToDouble(Double::doubleValue)
                .sum();
        if (Math.abs(sum - 1.0) > 0.0001) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weights sum must be 1");
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

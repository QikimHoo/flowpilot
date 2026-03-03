package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.TrafficLight;
import com.bank.progress.domain.WarningEntity;
import com.bank.progress.domain.WarningStatus;
import com.bank.progress.repository.ComplianceItemRepository;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.repository.WarningRepository;
import com.bank.progress.util.BusinessDayUtil;
import com.bank.progress.util.JsonUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComputeService {

    private static final List<String> DIMS = List.of("req", "dev", "test", "milestone", "compliance");

    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            "req", 0.15,
            "dev", 0.35,
            "test", 0.25,
            "milestone", 0.15,
            "compliance", 0.10
    );

    private final NodeRepository nodeRepository;
    private final ComplianceItemRepository complianceItemRepository;
    private final WarningRepository warningRepository;
    private final NotificationService notificationService;

    @Value("${app.leader-users:u_leader}")
    private List<String> leaderUsers;

    public ComputeService(NodeRepository nodeRepository,
                          ComplianceItemRepository complianceItemRepository,
                          WarningRepository warningRepository,
                          NotificationService notificationService) {
        this.nodeRepository = nodeRepository;
        this.complianceItemRepository = complianceItemRepository;
        this.warningRepository = warningRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void recalcNode(String nodeId) {
        NodeEntity current = nodeRepository.findById(nodeId).orElseThrow();
        while (current != null) {
            computeAndPersistRecursive(current);
            if (current.getParentId() == null) {
                current = null;
            } else {
                current = nodeRepository.findById(current.getParentId()).orElse(null);
            }
        }
    }

    private Metrics computeAndPersistRecursive(NodeEntity node) {
        List<NodeEntity> children = nodeRepository.findByParentIdOrderBySortOrderAsc(node.getId());
        List<Metrics> childMetrics = children.stream().map(this::computeAndPersistRecursive).toList();

        Metrics metrics;
        if (hasOwnKpi(node)) {
            metrics = metricsFromOwn(node);
        } else if (!childMetrics.isEmpty()) {
            metrics = metricsFromChildren(node, childMetrics);
        } else {
            metrics = Metrics.empty(weights(node));
        }

        boolean complianceKeyOverdue = complianceItemRepository
                .existsByNodeIdAndIsKeyTrueAndOverdueDaysGreaterThan(node.getId(), 0);

        TrafficLight light = decideLight(metrics.deviation, complianceKeyOverdue);
        List<Map<String, Object>> topFactors = topFactors(metrics);

        Map<String, Object> computed = new LinkedHashMap<>();
        computed.put("planScore", round(metrics.planScore));
        computed.put("actualScore", round(metrics.actualScore));
        computed.put("deviation", round(metrics.deviation));
        computed.put("trafficLight", light.name());
        computed.put("deviationTopFactors", topFactors);
        computed.put("complianceKeyOverdue", complianceKeyOverdue);

        node.setComputed(JsonUtil.write(computed));
        nodeRepository.save(node);

        syncWarning(node, metrics.deviation, light, topFactors, complianceKeyOverdue);
        return metrics;
    }

    private void syncWarning(NodeEntity node,
                             double deviation,
                             TrafficLight light,
                             List<Map<String, Object>> topFactors,
                             boolean complianceKeyOverdue) {
        Optional<WarningEntity> latestOpen = warningRepository
                .findFirstByNodeIdAndStatusInOrderByTriggeredAtDesc(
                        node.getId(),
                        List.of(WarningStatus.OPEN.name(), WarningStatus.IN_PROGRESS.name())
                );

        if (light == TrafficLight.GREEN) {
            latestOpen.ifPresent(w -> {
                w.setStatus(WarningStatus.RESOLVED.name());
                w.setResolvedAt(LocalDateTime.now());
                w.setReason("回归绿色");
                appendAction(w, "系统自动关闭：节点已回绿");
                warningRepository.save(w);
            });
            return;
        }

        WarningEntity target = latestOpen.orElseGet(WarningEntity::new);
        if (target.getId() == null) {
            target.setNodeId(node.getId());
            target.setStatus(WarningStatus.OPEN.name());
            target.setTriggeredAt(LocalDateTime.now());
            target.setActionLog(JsonUtil.write(new ArrayList<>()));
        }

        String previousLevel = target.getLevel();
        target.setLevel(light.name());
        target.setDeviation(round(deviation));
        target.setDeviationTopFactors(JsonUtil.write(topFactors));
        target.setAssignees(JsonUtil.write(resolveAssignees(node, light)));
        target.setSlaDueAt(slaDueAt(light));
        target.setReason(complianceKeyOverdue ? "关键合规材料超期，强制红灯" : "计划与实际偏差触发");

        if (previousLevel != null && !previousLevel.equals(light.name())) {
            appendAction(target, "系统升级：" + previousLevel + " -> " + light.name());
        }
        warningRepository.save(target);

        // 发送通知
        notificationService.sendWarningNotification(
                node.getId(),
                light.name(),
                deviation,
                resolveAssignees(node, light)
        );
    }

    private LocalDateTime slaDueAt(TrafficLight level) {
        LocalDate base = LocalDate.now();
        if (level == TrafficLight.RED) {
            return BusinessDayUtil.plusWorkingDays(base, 1).atStartOfDay();
        }
        return BusinessDayUtil.plusWorkingDays(base, 2).atStartOfDay();
    }

    private List<String> resolveAssignees(NodeEntity node, TrafficLight level) {
        Set<String> users = new LinkedHashSet<>();
        if (node.getOwnerUserId() != null) {
            users.add(node.getOwnerUserId());
        }
        if (level == TrafficLight.RED) {
            users.addAll(leaderUsers);
        }
        return new ArrayList<>(users);
    }

    private void appendAction(WarningEntity warning, String action) {
        List<Map<String, Object>> actionLog = JsonUtil.read(warning.getActionLog(), List.class);
        if (actionLog == null) {
            actionLog = new ArrayList<>();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("at", LocalDateTime.now().toString());
        row.put("action", action);
        actionLog.add(row);
        warning.setActionLog(JsonUtil.write(actionLog));
    }

    private TrafficLight decideLight(double deviation, boolean complianceKeyOverdue) {
        if (complianceKeyOverdue) {
            return TrafficLight.RED;
        }
        if (deviation <= 0.05) {
            return TrafficLight.GREEN;
        }
        if (deviation <= 0.15) {
            return TrafficLight.YELLOW;
        }
        return TrafficLight.RED;
    }

    private Metrics metricsFromOwn(NodeEntity node) {
        Map<String, Double> weights = weights(node);
        Map<String, Double> p = extractCompletion(node.getPlan(), "expectedDone");
        Map<String, Double> a = extractCompletion(node.getActual(), "done");
        return buildMetrics(weights, p, a);
    }

    private Metrics metricsFromChildren(NodeEntity node, List<Metrics> childMetrics) {
        Map<String, Double> weights = weights(node);
        Map<String, Double> p = new LinkedHashMap<>();
        Map<String, Double> a = new LinkedHashMap<>();

        for (String dim : DIMS) {
            double pAvg = childMetrics.stream().mapToDouble(m -> m.planDim.getOrDefault(dim, 0.0)).average().orElse(0.0);
            double aAvg = childMetrics.stream().mapToDouble(m -> m.actualDim.getOrDefault(dim, 0.0)).average().orElse(0.0);
            p.put(dim, round(pAvg));
            a.put(dim, round(aAvg));
        }
        return buildMetrics(weights, p, a);
    }

    private Metrics buildMetrics(Map<String, Double> weights, Map<String, Double> p, Map<String, Double> a) {
        double planScore = DIMS.stream().mapToDouble(d -> weights.getOrDefault(d, 0.0) * p.getOrDefault(d, 0.0)).sum();
        double actualScore = DIMS.stream().mapToDouble(d -> weights.getOrDefault(d, 0.0) * a.getOrDefault(d, 0.0)).sum();
        double deviation = planScore == 0 ? 0 : (planScore - actualScore) / planScore;
        return new Metrics(weights, p, a, planScore, actualScore, deviation);
    }

    private List<Map<String, Object>> topFactors(Metrics metrics) {
        return DIMS.stream()
                .map(dim -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    double contrib = metrics.weights.getOrDefault(dim, 0.0)
                            * (metrics.planDim.getOrDefault(dim, 0.0) - metrics.actualDim.getOrDefault(dim, 0.0));
                    item.put("dim", dim);
                    item.put("contrib", round(contrib));
                    return item;
                })
                .sorted((a, b) -> Double.compare((double) b.get("contrib"), (double) a.get("contrib")))
                .limit(3)
                .collect(Collectors.toList());
    }

    private boolean hasOwnKpi(NodeEntity node) {
        Map<String, Object> plan = JsonUtil.readMap(node.getPlan());
        Map<String, Object> actual = JsonUtil.readMap(node.getActual());
        return extractKpi(plan).size() > 0 && extractKpi(actual).size() > 0;
    }

    private Map<String, Double> weights(NodeEntity node) {
        Map<String, Object> raw = JsonUtil.readMap(node.getWeights());
        Map<String, Double> weights = new LinkedHashMap<>();
        for (String dim : DIMS) {
            weights.put(dim, toUnitNumber(raw.get(dim), DEFAULT_WEIGHTS.get(dim)));
        }
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.0001 && sum > 0) {
            for (String dim : DIMS) {
                weights.put(dim, weights.get(dim) / sum);
            }
        }
        return weights;
    }

    private Map<String, Double> extractCompletion(String json, String key) {
        Map<String, Object> map = JsonUtil.readMap(json);
        Map<String, Object> kpi = extractKpi(map);
        Map<String, Double> result = new LinkedHashMap<>();
        for (String dim : DIMS) {
            Object obj = kpi.get(dim);
            if (obj instanceof Map<?, ?> dimMap) {
                result.put(dim, toUnitNumber(dimMap.get(key), 0));
            } else {
                result.put(dim, 0.0);
            }
        }
        return result;
    }

    private Map<String, Object> extractKpi(Map<String, Object> map) {
        Object plan = map.get("kpi");
        if (plan instanceof Map<?, ?> planMap) {
            return (Map<String, Object>) planMap;
        }
        return new LinkedHashMap<>();
    }

    private double toUnitNumber(Object raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number n) {
            return clamp(n.doubleValue());
        }
        if (raw instanceof String s) {
            String value = s.trim();
            if (value.endsWith("%")) {
                return clamp(Double.parseDouble(value.replace("%", "")) / 100.0);
            }
            return clamp(Double.parseDouble(value));
        }
        return fallback;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static class Metrics {
        private final Map<String, Double> weights;
        private final Map<String, Double> planDim;
        private final Map<String, Double> actualDim;
        private final double planScore;
        private final double actualScore;
        private final double deviation;

        public Metrics(Map<String, Double> weights,
                       Map<String, Double> planDim,
                       Map<String, Double> actualDim,
                       double planScore,
                       double actualScore,
                       double deviation) {
            this.weights = weights;
            this.planDim = planDim;
            this.actualDim = actualDim;
            this.planScore = planScore;
            this.actualScore = actualScore;
            this.deviation = deviation;
        }

        public static Metrics empty(Map<String, Double> weights) {
            Map<String, Double> zero = DIMS.stream().collect(Collectors.toMap(k -> k, k -> 0.0));
            return new Metrics(weights, zero, zero, 0, 0, 0);
        }
    }
}

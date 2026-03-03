package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.WarningEntity;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.repository.WarningRepository;
import com.bank.progress.util.BusinessDayUtil;
import com.bank.progress.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WarningEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarningEscalationScheduler.class);

    private final WarningRepository warningRepository;
    private final NodeRepository nodeRepository;
    private final NotificationService notificationService;

    public WarningEscalationScheduler(WarningRepository warningRepository,
                                     NodeRepository nodeRepository,
                                     NotificationService notificationService) {
        this.warningRepository = warningRepository;
        this.nodeRepository = nodeRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void escalateYellowWarnings() {
        List<WarningEntity> openYellows = warningRepository.findByStatusAndLevel("OPEN", "YELLOW");
        for (WarningEntity warning : openYellows) {
            NodeEntity node = nodeRepository.findById(warning.getNodeId()).orElse(null);
            if (node == null) {
                continue;
            }
            Map<String, Object> computed = JsonUtil.readMap(node.getComputed());
            String currentLight = String.valueOf(computed.getOrDefault("trafficLight", "GREEN"));
            double currentDeviation = Double.parseDouble(String.valueOf(computed.getOrDefault("deviation", 0)));

            boolean noBackToGreenFiveDays = BusinessDayUtil.workingDaysBetween(
                    warning.getTriggeredAt().toLocalDate(),
                    LocalDate.now()
            ) >= 5 && "YELLOW".equals(currentLight);

            boolean deviationExpanded = currentDeviation > warning.getDeviation() + 0.02;

            if (noBackToGreenFiveDays || deviationExpanded) {
                warning.setLevel("RED");
                warning.setReason("YELLOW 连续 5 个工作日未回绿或偏差扩大，系统升级 RED");
                warning.setSlaDueAt(BusinessDayUtil.plusWorkingDays(LocalDate.now(), 1).atStartOfDay());

                List<Map<String, Object>> actionLog = JsonUtil.read(warning.getActionLog(), List.class);
                if (actionLog == null) {
                    actionLog = new ArrayList<>();
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("at", LocalDateTime.now().toString());
                row.put("action", "scheduler_escalate_to_red");
                row.put("deviation", currentDeviation);
                actionLog.add(row);
                warning.setActionLog(JsonUtil.write(actionLog));

                warningRepository.save(warning);
                log.info("warning escalated to RED, id={}", warning.getId());

                // 发送升级通知
                notificationService.sendEscalationNotification(
                        warning.getNodeId(),
                        "YELLOW 连续 5 个工作日未回绿或偏差扩大"
                );
            }
        }
    }
}

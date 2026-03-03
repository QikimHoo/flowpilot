package com.bank.progress.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${app.notification.webhook-url:}")
    private String webhookUrl;

    @Value("${app.notification.enabled:false}")
    private boolean enabled;

    @Value("${app.leader-users:u_leader}")
    private List<String> leaderUsers;

    /**
     * 发送预警通知
     * MVP: 控制台日志 + 预留 webhook
     */
    public void sendWarningNotification(String nodeId, String level, double deviation, List<String> assignees) {
        String message = String.format(
                "【预警通知】节点: %s, 级别: %s, 偏差: %.1f%%, 责任人: %s",
                nodeId, level, deviation * 100, String.join(",", assignees)
        );

        // 控制台日志（MVP 必须）
        if ("RED".equals(level)) {
            log.error(message);
        } else {
            log.warn(message);
        }

        // Webhook 通知（预留扩展）
        if (enabled && webhookUrl != null && !webhookUrl.isBlank()) {
            sendWebhook(message, Map.of(
                    "nodeId", nodeId,
                    "level", level,
                    "deviation", deviation,
                    "assignees", assignees
            ));
        }
    }

    /**
     * 发送升级通知（YELLOW -> RED）
     */
    public void sendEscalationNotification(String nodeId, String reason) {
        String message = String.format("【预警升级】节点: %s, 原因: %s", nodeId, reason);
        log.error(message);

        if (enabled && webhookUrl != null && !webhookUrl.isBlank()) {
            sendWebhook(message, Map.of(
                    "nodeId", nodeId,
                    "reason", reason,
                    "type", "escalation"
            ));
        }
    }

    /**
     * 发送合规材料超期通知
     */
    public void sendComplianceOverdueNotification(String nodeId, String docType, int overdueDays) {
        String message = String.format(
                "【合规超期】节点: %s, 材料: %s, 超期: %d 天",
                nodeId, docType, overdueDays
        );
        log.error(message);

        if (enabled && webhookUrl != null && !webhookUrl.isBlank()) {
            sendWebhook(message, Map.of(
                    "nodeId", nodeId,
                    "docType", docType,
                    "overdueDays", overdueDays,
                    "type", "compliance_overdue"
            ));
        }
    }

    private void sendWebhook(String message, Map<String, Object> payload) {
        try {
            // TODO: 实现 HTTP POST 到 webhook URL
            // 可使用 RestTemplate 或 WebClient
            log.info("Webhook 发送: {} -> {}", webhookUrl, message);
        } catch (Exception e) {
            log.error("Webhook 发送失败: {}", e.getMessage());
        }
    }
}

package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.UserEntity;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.util.JsonUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final NodeRepository nodeRepository;
    private final AccessControlService accessControlService;

    public ReportService(NodeRepository nodeRepository, AccessControlService accessControlService) {
        this.nodeRepository = nodeRepository;
        this.accessControlService = accessControlService;
    }

    public String exportCsv(String rootId, UserEntity user, String period) {
        List<NodeEntity> nodes = nodeRepository.findAllByOrderByPathAsc();
        String rootPath = null;
        if (rootId != null && !rootId.isBlank()) {
            rootPath = nodeRepository.findById(rootId).map(NodeEntity::getPath).orElse(null);
        }
        try (StringWriter sw = new StringWriter();
             CSVPrinter csv = new CSVPrinter(sw, CSVFormat.DEFAULT
                     .builder()
                     .setHeader("period", "nodeId", "nodeType", "nodeName", "owner", "planScore", "actualScore", "deviation", "trafficLight")
                     .build())) {
            for (NodeEntity node : nodes) {
                if (!accessControlService.canViewNode(user, node)) {
                    continue;
                }
                if (rootPath != null && !node.getPath().startsWith(rootPath)) {
                    continue;
                }
                Map<String, Object> computed = JsonUtil.readMap(node.getComputed());
                csv.printRecord(
                        period,
                        node.getId(),
                        node.getNodeType(),
                        node.getNodeName(),
                        node.getOwnerUserId(),
                        computed.getOrDefault("planScore", 0),
                        computed.getOrDefault("actualScore", 0),
                        computed.getOrDefault("deviation", 0),
                        computed.getOrDefault("trafficLight", "GREEN")
                );
            }
            csv.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export CSV", e);
        }
    }
}

package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.repository.ComplianceItemRepository;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.repository.WarningRepository;
import com.bank.progress.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComputeServiceTest {

    @Mock
    private NodeRepository nodeRepository;
    @Mock
    private ComplianceItemRepository complianceItemRepository;
    @Mock
    private WarningRepository warningRepository;

    @InjectMocks
    private ComputeService computeService;

    private NodeEntity leaf;

    @BeforeEach
    void setup() {
        leaf = new NodeEntity();
        leaf.setId("REQ-1");
        leaf.setNodeType("req");
        leaf.setNodeName("test");
        leaf.setParentId(null);
        leaf.setPath("/REQ-1");
        leaf.setWeights("{\"req\":0.15,\"dev\":0.35,\"test\":0.25,\"milestone\":0.15,\"compliance\":0.10}");
        leaf.setAsOfDate(LocalDate.now());

        when(nodeRepository.findById("REQ-1")).thenReturn(Optional.of(leaf));
        when(nodeRepository.findByParentIdOrderBySortOrderAsc(anyString())).thenReturn(java.util.List.of());
        when(nodeRepository.save(any(NodeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(warningRepository.findFirstByNodeIdAndStatusInOrderByTriggeredAtDesc(anyString(), anyList())).thenReturn(Optional.empty());
    }

    @Test
    void shouldJudgeThresholdBoundariesAndPZero() {
        leaf.setPlan(plan(1.0));
        leaf.setActual(actual(0.95));
        when(complianceItemRepository.existsByNodeIdAndIsKeyTrueAndOverdueDaysGreaterThan("REQ-1", 0)).thenReturn(false);
        computeService.recalcNode("REQ-1");

        Map<String, Object> computed1 = JsonUtil.readMap(leaf.getComputed());
        assertThat(computed1.get("trafficLight")).isEqualTo("GREEN");

        leaf.setActual(actual(0.85));
        computeService.recalcNode("REQ-1");
        Map<String, Object> computed2 = JsonUtil.readMap(leaf.getComputed());
        assertThat(computed2.get("trafficLight")).isEqualTo("YELLOW");

        leaf.setPlan(plan(0.0));
        leaf.setActual(actual(0.0));
        computeService.recalcNode("REQ-1");
        Map<String, Object> computed3 = JsonUtil.readMap(leaf.getComputed());
        assertThat(computed3.get("deviation")).isEqualTo(0.0);
        assertThat(computed3.get("trafficLight")).isEqualTo("GREEN");
    }

    @Test
    void shouldForceRedWhenComplianceKeyOverdue() {
        leaf.setPlan(plan(0.6));
        leaf.setActual(actual(0.6));
        when(complianceItemRepository.existsByNodeIdAndIsKeyTrueAndOverdueDaysGreaterThan("REQ-1", 0)).thenReturn(true);

        computeService.recalcNode("REQ-1");

        ArgumentCaptor<NodeEntity> captor = ArgumentCaptor.forClass(NodeEntity.class);
        org.mockito.Mockito.verify(nodeRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Map<String, Object> computed = JsonUtil.readMap(captor.getValue().getComputed());
        assertThat(computed.get("trafficLight")).isEqualTo("RED");
        assertThat(computed.get("complianceKeyOverdue")).isEqualTo(true);
    }

    private String plan(double done) {
        return JsonUtil.write(Map.of("kpi", Map.of(
                "req", Map.of("expectedDone", done),
                "dev", Map.of("expectedDone", done),
                "test", Map.of("expectedDone", done),
                "milestone", Map.of("expectedDone", done),
                "compliance", Map.of("expectedDone", done)
        )));
    }

    private String actual(double done) {
        return JsonUtil.write(Map.of("kpi", Map.of(
                "req", Map.of("done", done),
                "dev", Map.of("done", done),
                "test", Map.of("done", done),
                "milestone", Map.of("done", done),
                "compliance", Map.of("done", done)
        )));
    }
}

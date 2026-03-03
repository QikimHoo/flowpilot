package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.WarningEntity;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.repository.WarningRepository;
import com.bank.progress.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarningEscalationSchedulerTest {

    @Mock
    private WarningRepository warningRepository;
    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private WarningEscalationScheduler scheduler;

    @Test
    void shouldEscalateAfterFiveWorkingDaysWithoutBackToGreen() {
        WarningEntity warning = new WarningEntity();
        warning.setId(1L);
        warning.setNodeId("REQ-1");
        warning.setLevel("YELLOW");
        warning.setStatus("OPEN");
        warning.setDeviation(0.08);
        warning.setTriggeredAt(LocalDateTime.now().minusDays(8));
        warning.setActionLog("[]");

        NodeEntity node = new NodeEntity();
        node.setId("REQ-1");
        node.setComputed(JsonUtil.write(java.util.Map.of("trafficLight", "YELLOW", "deviation", 0.09)));

        when(warningRepository.findByStatusAndLevel("OPEN", "YELLOW")).thenReturn(List.of(warning));
        when(nodeRepository.findById("REQ-1")).thenReturn(Optional.of(node));
        when(warningRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.escalateYellowWarnings();

        ArgumentCaptor<WarningEntity> captor = ArgumentCaptor.forClass(WarningEntity.class);
        verify(warningRepository).save(captor.capture());
        assertThat(captor.getValue().getLevel()).isEqualTo("RED");
    }

    @Test
    void shouldEscalateWhenDeviationExpands() {
        WarningEntity warning = new WarningEntity();
        warning.setId(2L);
        warning.setNodeId("REQ-2");
        warning.setLevel("YELLOW");
        warning.setStatus("OPEN");
        warning.setDeviation(0.10);
        warning.setTriggeredAt(LocalDateTime.now().minusDays(1));
        warning.setActionLog("[]");

        NodeEntity node = new NodeEntity();
        node.setId("REQ-2");
        node.setComputed(JsonUtil.write(java.util.Map.of("trafficLight", "YELLOW", "deviation", 0.13)));

        when(warningRepository.findByStatusAndLevel("OPEN", "YELLOW")).thenReturn(List.of(warning));
        when(nodeRepository.findById("REQ-2")).thenReturn(Optional.of(node));
        when(warningRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.escalateYellowWarnings();

        ArgumentCaptor<WarningEntity> captor = ArgumentCaptor.forClass(WarningEntity.class);
        verify(warningRepository).save(captor.capture());
        assertThat(captor.getValue().getLevel()).isEqualTo("RED");
    }
}

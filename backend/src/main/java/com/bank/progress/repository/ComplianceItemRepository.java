package com.bank.progress.repository;

import com.bank.progress.domain.ComplianceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceItemRepository extends JpaRepository<ComplianceItemEntity, Long> {
    List<ComplianceItemEntity> findByNodeIdOrderByRequiredAtAsc(String nodeId);

    boolean existsByNodeIdAndIsKeyTrueAndOverdueDaysGreaterThan(String nodeId, Integer overdueDays);
}

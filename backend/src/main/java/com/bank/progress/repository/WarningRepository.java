package com.bank.progress.repository;

import com.bank.progress.domain.WarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarningRepository extends JpaRepository<WarningEntity, Long> {
    Optional<WarningEntity> findFirstByNodeIdAndStatusInOrderByTriggeredAtDesc(String nodeId, List<String> statuses);

    List<WarningEntity> findByStatusAndLevel(String status, String level);
}

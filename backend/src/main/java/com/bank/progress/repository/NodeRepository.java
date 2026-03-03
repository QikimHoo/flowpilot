package com.bank.progress.repository;

import com.bank.progress.domain.NodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeRepository extends JpaRepository<NodeEntity, String> {
    List<NodeEntity> findByParentIdOrderBySortOrderAsc(String parentId);

    List<NodeEntity> findByPathStartingWithOrderByPathAsc(String pathPrefix);

    List<NodeEntity> findAllByOrderByPathAsc();
}

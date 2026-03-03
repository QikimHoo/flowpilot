package com.bank.progress.repository;

import com.bank.progress.domain.UserNodeAclEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNodeAclRepository extends JpaRepository<UserNodeAclEntity, Long> {
    List<UserNodeAclEntity> findByUserId(String userId);
}

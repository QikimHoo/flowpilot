package com.bank.progress.service;

import com.bank.progress.domain.NodeEntity;
import com.bank.progress.domain.RoleType;
import com.bank.progress.domain.UserEntity;
import com.bank.progress.domain.UserNodeAclEntity;
import com.bank.progress.repository.NodeRepository;
import com.bank.progress.repository.UserNodeAclRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccessControlService {

    private final AuthService authService;
    private final UserNodeAclRepository aclRepository;
    private final NodeRepository nodeRepository;

    public AccessControlService(AuthService authService, UserNodeAclRepository aclRepository, NodeRepository nodeRepository) {
        this.authService = authService;
        this.aclRepository = aclRepository;
        this.nodeRepository = nodeRepository;
    }

    public boolean canViewAll(UserEntity user) {
        Set<RoleType> roles = authService.rolesOf(user);
        return roles.contains(RoleType.ROLE_LEADER) || roles.contains(RoleType.ROLE_PMO);
    }

    public boolean isCompliance(UserEntity user) {
        return authService.rolesOf(user).contains(RoleType.ROLE_COMPLIANCE);
    }

    public boolean canWrite(UserEntity user) {
        Set<RoleType> roles = authService.rolesOf(user);
        return roles.contains(RoleType.ROLE_PMO) || roles.contains(RoleType.ROLE_PM)
                || roles.contains(RoleType.ROLE_MODULE_OWNER);
    }

    public boolean canViewNode(UserEntity user, NodeEntity node) {
        Set<RoleType> roles = authService.rolesOf(user);
        if (roles.contains(RoleType.ROLE_LEADER) || roles.contains(RoleType.ROLE_PMO) || roles.contains(RoleType.ROLE_COMPLIANCE)) {
            return true;
        }
        if (roles.contains(RoleType.ROLE_PM) && user.getUsername().equals(node.getOwnerUserId())) {
            return true;
        }
        Set<String> allowed = resolvedAclNodeIds(user.getUsername());
        for (String nodeId : allowed) {
            NodeEntity root = nodeRepository.findById(nodeId).orElse(null);
            if (root != null && node.getPath().startsWith(root.getPath())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> resolvedAclNodeIds(String userId) {
        List<UserNodeAclEntity> rows = aclRepository.findByUserId(userId);
        Set<String> ids = new HashSet<>();
        for (UserNodeAclEntity row : rows) {
            ids.add(row.getNodeId());
        }
        return ids;
    }
}

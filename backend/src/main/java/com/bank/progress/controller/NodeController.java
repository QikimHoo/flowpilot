package com.bank.progress.controller;

import com.bank.progress.domain.UserEntity;
import com.bank.progress.dto.MoveNodeRequest;
import com.bank.progress.dto.NodeUpsertRequest;
import com.bank.progress.service.AuthService;
import com.bank.progress.service.NodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeService nodeService;
    private final AuthService authService;

    public NodeController(NodeService nodeService, AuthService authService) {
        this.nodeService = nodeService;
        this.authService = authService;
    }

    @GetMapping("/tree")
    public Object tree(@RequestParam(required = false) String rootId, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.getTree(rootId, user);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.getNode(id, user);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody NodeUpsertRequest req, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.createNode(req, user, request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody NodeUpsertRequest req, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.updateNode(id, req, user, request);
    }

    @PostMapping("/{id}/move")
    public Map<String, Object> move(@PathVariable String id, @RequestBody MoveNodeRequest req, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.moveNode(id, req, user, request);
    }

    @PostMapping("/import")
    public Map<String, Object> importNode(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.importNode(payload, user, request);
    }

    @GetMapping("/{id}/export")
    public Map<String, Object> export(@PathVariable String id, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return nodeService.exportNode(id, user);
    }
}

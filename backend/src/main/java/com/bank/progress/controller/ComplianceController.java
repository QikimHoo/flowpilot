package com.bank.progress.controller;

import com.bank.progress.domain.UserEntity;
import com.bank.progress.dto.ComplianceItemRequest;
import com.bank.progress.service.AuthService;
import com.bank.progress.service.ComplianceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance/items")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final AuthService authService;

    public ComplianceController(ComplianceService complianceService, AuthService authService) {
        this.complianceService = complianceService;
        this.authService = authService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String nodeId,
                                          HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return complianceService.list(nodeId, user);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody ComplianceItemRequest req, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return complianceService.create(req, user, request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                      @RequestBody ComplianceItemRequest req,
                                      HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return complianceService.update(id, req, user, request);
    }
}

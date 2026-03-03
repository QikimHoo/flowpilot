package com.bank.progress.controller;

import com.bank.progress.service.AuthService;
import com.bank.progress.service.ComputeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/compute")
public class ComputeController {

    private final ComputeService computeService;
    private final AuthService authService;

    public ComputeController(ComputeService computeService, AuthService authService) {
        this.computeService = computeService;
        this.authService = authService;
    }

    @PostMapping("/recalc/{id}")
    public Map<String, Object> recalc(@PathVariable String id, HttpServletRequest request) {
        authService.requireUser(request);
        computeService.recalcNode(id);
        return Map.of("ok", true, "nodeId", id);
    }
}

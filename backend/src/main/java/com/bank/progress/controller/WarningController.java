package com.bank.progress.controller;

import com.bank.progress.domain.UserEntity;
import com.bank.progress.dto.WarningActionRequest;
import com.bank.progress.service.AuthService;
import com.bank.progress.service.WarningService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    private final WarningService warningService;
    private final AuthService authService;

    public WarningController(WarningService warningService, AuthService authService) {
        this.warningService = warningService;
        this.authService = authService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String level,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String rootId,
                                          @RequestParam(required = false) String owner,
                                          HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return warningService.list(user, level, status, rootId, owner);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return warningService.get(id, user);
    }

    @PostMapping("/{id}/action")
    public Map<String, Object> action(@PathVariable Long id,
                                      @RequestBody WarningActionRequest req,
                                      HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return warningService.action(id, req, user, request);
    }
}

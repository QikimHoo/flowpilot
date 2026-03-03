package com.bank.progress.controller;

import com.bank.progress.domain.UserEntity;
import com.bank.progress.dto.LoginRequest;
import com.bank.progress.repository.UserRepository;
import com.bank.progress.service.AuthService;
import com.bank.progress.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid user"));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", "mock-" + user.getUsername());
        resp.put("user", toDto(user));
        return resp;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        return toDto(user);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    private Map<String, Object> toDto(UserEntity user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("displayName", user.getDisplayName());
        dto.put("dept", user.getDept());
        dto.put("roles", JsonUtil.read(user.getRoles(), List.class));
        return dto;
    }
}

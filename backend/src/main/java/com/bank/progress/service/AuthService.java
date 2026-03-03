package com.bank.progress.service;

import com.bank.progress.domain.RoleType;
import com.bank.progress.domain.UserEntity;
import com.bank.progress.repository.UserRepository;
import com.bank.progress.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    public static final String USER_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity requireUser(HttpServletRequest request) {
        String userId = request.getHeader(USER_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        return userRepository.findByUsername(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public UserEntity requireUser(String userId) {
        return userRepository.findByUsername(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public Set<RoleType> rolesOf(UserEntity user) {
        List<String> raw = JsonUtil.read(user.getRoles(), List.class);
        return raw.stream().map(RoleType::valueOf).collect(Collectors.toSet());
    }
}

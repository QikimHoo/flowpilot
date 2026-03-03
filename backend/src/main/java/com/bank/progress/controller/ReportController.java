package com.bank.progress.controller;

import com.bank.progress.domain.UserEntity;
import com.bank.progress.service.AuthService;
import com.bank.progress.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    @GetMapping("/weekly.csv")
    public ResponseEntity<String> weekly(@RequestParam(required = false) String rootId, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        String csv = reportService.exportCsv(rootId, user, "weekly");
        return csvResponse(csv, "weekly-report.csv");
    }

    @GetMapping("/monthly.csv")
    public ResponseEntity<String> monthly(@RequestParam(required = false) String rootId, HttpServletRequest request) {
        UserEntity user = authService.requireUser(request);
        String csv = reportService.exportCsv(rootId, user, "monthly");
        return csvResponse(csv, "monthly-report.csv");
    }

    private ResponseEntity<String> csvResponse(String csv, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.valueOf("text/csv"))
                .body(csv);
    }
}

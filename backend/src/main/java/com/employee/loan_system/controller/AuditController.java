package com.employee.loan_system.controller;

import com.employee.loan_system.auth.SecurityAuditService;
import com.employee.loan_system.auth.dto.AuditLogResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/auth/audit")
@CrossOrigin(originPatterns = "http://localhost:*")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private static final DateTimeFormatter FILE_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private SecurityAuditService securityAuditService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return new ResponseEntity<>(securityAuditService.searchLogs(action, actor, from, to, limit), HttpStatus.OK);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportLogsCsv(
            @RequestParam(defaultValue = "500") int limit,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        byte[] csv = securityAuditService.exportLogsCsv(action, actor, from, to, limit);
        String filename = "security-audit-" + FILE_TS_FORMATTER.format(LocalDateTime.now()) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(csv.length);

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }
}

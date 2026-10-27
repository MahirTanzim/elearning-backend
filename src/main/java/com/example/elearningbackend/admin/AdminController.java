package com.example.elearningbackend.admin;

import com.example.elearningbackend.admin.AdminDtos.AuditLogResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogRepository auditLogs;

    @GetMapping("/api/admin/ping")
    public Map<String, String> ping() {
        return Map.of("message", "hello admin");
    }

    @GetMapping("/api/admin/audit-logs")
    public List<AuditLogResponse> auditLogs() {
        return auditLogs.findTop50ByOrderByIdDesc().stream().map(AuditLogResponse::from).toList();
    }
}
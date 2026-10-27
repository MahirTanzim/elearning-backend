package com.example.elearningbackend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    /** Called inside the caller's transaction, so the log entry is saved or rolled back together with the action. */
    public void log(Long adminId, String action, String targetType, Long targetId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAdminId(adminId);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetails(details == null ? null
            : details.substring(0, Math.min(details.length(), 1000)));
        repository.save(entry);
    }
}
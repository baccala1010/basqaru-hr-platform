package kz.whosnext.hr.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.whosnext.hr.auth.model.entity.LoginAuditLog;
import kz.whosnext.hr.auth.model.enums.AuditAction;
import kz.whosnext.hr.auth.repository.LoginAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final LoginAuditLogRepository auditRepository;

    @Async
    public void log(UUID userId, String email, AuditAction action, boolean success,
                    String details, HttpServletRequest request) {
        var entry = LoginAuditLog.builder()
                .userId(userId)
                .email(email)
                .action(action)
                .success(success)
                .details(details)
                .ipAddress(extractIp(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();
        auditRepository.save(entry);
        log.debug("Audit: action={}, email={}, success={}", action, email, success);
    }

    public void log(UUID userId, String email, AuditAction action, boolean success, String details) {
        log(userId, email, action, success, details, null);
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


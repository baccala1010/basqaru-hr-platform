package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID employeeId,
        String title,
        String message,
        String type,
        boolean read,
        UUID referenceId,
        String referenceType,
        LocalDateTime createdAt
) {}


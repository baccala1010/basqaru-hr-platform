package kz.whosnext.hr.candidate.model.dto.response;

import kz.whosnext.hr.candidate.model.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType type,
        Boolean isRead,
        UUID referenceId,
        String referenceType,
        LocalDateTime createdAt
) {}


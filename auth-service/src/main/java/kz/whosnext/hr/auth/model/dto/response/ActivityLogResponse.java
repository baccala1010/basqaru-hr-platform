package kz.whosnext.hr.auth.model.dto.response;

import kz.whosnext.hr.auth.model.enums.ActivityAction;
import kz.whosnext.hr.auth.model.enums.ActivitySource;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String actorRole,
        ActivityAction action,
        ActivitySource source,
        String entityType,
        String entityId,
        String details,
        LocalDateTime createdAt
) {}

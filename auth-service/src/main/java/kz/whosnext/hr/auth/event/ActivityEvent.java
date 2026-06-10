package kz.whosnext.hr.auth.event;

import kz.whosnext.hr.auth.model.enums.ActivityAction;
import kz.whosnext.hr.auth.model.enums.ActivitySource;

import java.time.Instant;
import java.util.UUID;

public record ActivityEvent(
        UUID actorId,
        String actorEmail,
        String actorRole,
        ActivityAction action,
        ActivitySource source,
        String entityType,
        String entityId,
        String details,
        Instant timestamp
) {}

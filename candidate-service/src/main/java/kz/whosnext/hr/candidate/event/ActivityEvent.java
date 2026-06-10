package kz.whosnext.hr.candidate.event;

import kz.whosnext.hr.candidate.model.enums.ActivityAction;
import kz.whosnext.hr.candidate.model.enums.ActivitySource;

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

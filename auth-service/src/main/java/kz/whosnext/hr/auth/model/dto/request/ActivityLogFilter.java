package kz.whosnext.hr.auth.model.dto.request;

import kz.whosnext.hr.auth.model.enums.ActivityAction;
import kz.whosnext.hr.auth.model.enums.ActivitySource;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityLogFilter(
        UUID actorId,
        ActivityAction action,
        String entityType,
        ActivitySource source,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
) {}

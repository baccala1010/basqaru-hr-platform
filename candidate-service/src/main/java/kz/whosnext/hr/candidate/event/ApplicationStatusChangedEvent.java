package kz.whosnext.hr.candidate.event;

import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID applicationId,
        UUID candidateId,
        String oldStatus,
        String newStatus
) {}


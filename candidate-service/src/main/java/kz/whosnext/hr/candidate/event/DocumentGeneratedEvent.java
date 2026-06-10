package kz.whosnext.hr.candidate.event;

import java.util.List;
import java.util.UUID;

public record DocumentGeneratedEvent(
        UUID applicationId,
        List<UUID> documentIds
) {}


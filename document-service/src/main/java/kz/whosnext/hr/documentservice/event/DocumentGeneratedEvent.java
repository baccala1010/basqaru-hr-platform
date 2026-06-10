package kz.whosnext.hr.documentservice.event;

import java.util.List;
import java.util.UUID;

public record DocumentGeneratedEvent(
        UUID applicationId,
        List<UUID> documentIds
) {}


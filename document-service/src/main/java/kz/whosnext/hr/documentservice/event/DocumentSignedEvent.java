package kz.whosnext.hr.documentservice.event;

import java.util.UUID;

public record DocumentSignedEvent(
        UUID applicationId,
        UUID candidateId
) {}


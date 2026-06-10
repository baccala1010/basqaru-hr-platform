package kz.whosnext.hr.candidate.event;

import java.util.UUID;

public record DocumentSignedEvent(
        UUID applicationId,
        UUID candidateId
) {}


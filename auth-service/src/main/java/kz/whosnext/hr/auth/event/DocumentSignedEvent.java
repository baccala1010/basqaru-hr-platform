package kz.whosnext.hr.auth.event;

import java.util.UUID;

public record DocumentSignedEvent(
        UUID applicationId,
        UUID candidateId
) {
}


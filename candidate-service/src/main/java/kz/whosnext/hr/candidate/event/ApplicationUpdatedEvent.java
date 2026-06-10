package kz.whosnext.hr.candidate.event;

import java.util.UUID;

public record ApplicationUpdatedEvent(
        UUID applicationId,
        UUID candidateId,
        String candidateEmail
) {
}

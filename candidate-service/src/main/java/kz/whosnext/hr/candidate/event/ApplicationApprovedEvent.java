package kz.whosnext.hr.candidate.event;

import java.util.UUID;

public record ApplicationApprovedEvent(
        UUID applicationId,
        UUID candidateId,
        UUID vacancyId,
        String candidateEmail,
        String firstName,
        String lastName,
        String iin,
        UUID userId
) {}


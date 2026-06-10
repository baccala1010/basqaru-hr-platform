package kz.whosnext.hr.documentservice.event;

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


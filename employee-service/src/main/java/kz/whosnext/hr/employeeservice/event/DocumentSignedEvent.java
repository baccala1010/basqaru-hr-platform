package kz.whosnext.hr.employeeservice.event;

import java.util.UUID;

public record DocumentSignedEvent(
        UUID applicationId,
        UUID candidateId
) {}


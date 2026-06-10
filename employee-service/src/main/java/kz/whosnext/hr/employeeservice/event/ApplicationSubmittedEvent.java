package kz.whosnext.hr.employeeservice.event;

import java.util.UUID;

public record ApplicationSubmittedEvent(
        UUID applicationId,
        UUID candidateId,
        UUID vacancyId,
        String candidateEmail,
        String vacancyTitle
) {
}

package kz.whosnext.hr.employeeservice.event;

import java.util.UUID;

public record ApplicationUpdatedEvent(
        UUID applicationId,
        UUID candidateId,
        String candidateEmail
) {
}

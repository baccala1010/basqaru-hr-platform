package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record IngestAttendanceEventRequest(
        @NotBlank String source,
        @NotBlank String externalEventId,
        UUID employeeId,
        @Size(min = 12, max = 12) String iin,
        @NotBlank String eventType,
        String location,
        LocalDateTime occurredAt,
        @Size(max = 10000) String rawPayload
) {
}


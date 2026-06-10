package kz.whosnext.hr.employeeservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceIngestedEvent(
        UUID ingestionEventId,
        UUID employeeId,
        String source,
        String eventType,
        LocalDateTime occurredAt,
        String status
) {
}


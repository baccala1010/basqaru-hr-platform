package kz.whosnext.hr.employeeservice.model.dto.response;

import java.util.UUID;

public record AttendanceIngestionResponse(
        UUID ingestionEventId,
        UUID attendanceRecordId,
        boolean duplicate,
        String status,
        String message
) {
}


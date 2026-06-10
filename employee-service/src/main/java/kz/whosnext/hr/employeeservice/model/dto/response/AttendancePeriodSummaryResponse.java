package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record AttendancePeriodSummaryResponse(
        UUID employeeId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long workingDays,
        long presentDays
) {
}


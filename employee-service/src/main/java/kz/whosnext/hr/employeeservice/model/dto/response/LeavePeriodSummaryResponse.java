package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record LeavePeriodSummaryResponse(
        UUID employeeId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long annualDays,
        long sickDays,
        long unpaidDays,
        long maternityDays,
        long paternityDays,
        long otherDays
) {
}


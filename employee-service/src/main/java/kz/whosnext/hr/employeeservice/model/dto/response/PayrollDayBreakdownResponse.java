package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;

public record PayrollDayBreakdownResponse(
        LocalDate date,
        String type,
        String note
) {
}


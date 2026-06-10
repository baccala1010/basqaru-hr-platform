package kz.whosnext.hr.employeeservice.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PayrollProfileResponse(
        UUID id,
        UUID employeeId,
        BigDecimal monthlySalary,
        BigDecimal monthlyBonus,
        LocalDate effectiveFrom,
        LocalDateTime updatedAt
) {
}


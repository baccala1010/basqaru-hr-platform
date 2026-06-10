package kz.whosnext.hr.employeeservice.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PayrollSummaryResponse(
        UUID calculationId,
        UUID employeeId,
        String employeeFullName,
        LocalDate periodStart,
        LocalDate periodEnd,
        int workingDays,
        int workedDays,
        int sickDays,
        int unpaidDays,
        int vacationDays,
        int businessTripDays,
        int weekendDays,
        int holidayDays,
        int absentDays,
        BigDecimal monthlySalary,
        BigDecimal monthlyBonus,
        BigDecimal grossSalary,
        BigDecimal opvAmount,
        BigDecimal osmsAmount,
        BigDecimal iitAmount,
        BigDecimal netSalary,
        LocalDateTime generatedAt,
        List<PayrollDayBreakdownResponse> days
) {
}


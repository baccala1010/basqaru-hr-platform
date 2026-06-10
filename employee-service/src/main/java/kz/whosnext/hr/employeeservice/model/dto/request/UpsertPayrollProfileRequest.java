package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertPayrollProfileRequest(
        @NotNull @DecimalMin("0.00") BigDecimal monthlySalary,
        @DecimalMin("0.00") BigDecimal monthlyBonus,
        LocalDate effectiveFrom
) {
}


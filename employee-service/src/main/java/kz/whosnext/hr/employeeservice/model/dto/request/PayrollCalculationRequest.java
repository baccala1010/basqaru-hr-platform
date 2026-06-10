package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PayrollCalculationRequest(
        @NotNull UUID employeeId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        Boolean saveSnapshot
) {
}


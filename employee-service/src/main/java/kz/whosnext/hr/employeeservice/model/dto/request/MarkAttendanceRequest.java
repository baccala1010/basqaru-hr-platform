package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MarkAttendanceRequest(
        @NotNull UUID employeeId,
        @NotNull LocalDate date,
        @NotNull Boolean present
) {}


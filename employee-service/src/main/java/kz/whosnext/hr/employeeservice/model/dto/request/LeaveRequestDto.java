package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeaveRequestDto(
        @NotNull
        String leaveType,
        @NotNull
        LocalDate startDate,
        @NotNull
        LocalDate endDate,
        String reason
) {}


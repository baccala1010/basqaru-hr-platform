package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateWorkScheduleRequest(
        @NotBlank
        String name,
        @NotNull
        String type,
        LocalTime startTime,
        LocalTime endTime,
        String workingDays
) {}


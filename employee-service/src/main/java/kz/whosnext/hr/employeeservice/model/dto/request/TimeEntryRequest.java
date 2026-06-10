package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TimeEntryRequest(
        @NotNull
        UUID employeeId,
        @NotNull
        LocalDate date,
        LocalTime checkIn,
        LocalTime checkOut,
        String status,
        String note
) {}


package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(
        @NotNull
        UUID userId,
        UUID candidateId,
        UUID applicationId,
        UUID departmentId,
        UUID workScheduleId,
        @NotBlank
        String positionName,
        String contractType,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        String middleName,
        String iin,
        @NotBlank
        @Email
        String email,
        String phone,
        String address,
        LocalDate birthDate,
        @NotNull
        LocalDate hireDate
) {}


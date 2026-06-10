package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID userId,
        UUID candidateId,
        UUID applicationId,
        UUID departmentId,
        String departmentName,
        UUID workScheduleId,
        String workScheduleName,
        String positionName,
        String contractType,
        String firstName,
        String lastName,
        String middleName,
        String iin,
        String email,
        String phone,
        String address,
        LocalDate birthDate,
        LocalDate hireDate,
        LocalDate fireDate,
        String status,
        LocalDateTime createdAt,
        String photoUrl
) {}


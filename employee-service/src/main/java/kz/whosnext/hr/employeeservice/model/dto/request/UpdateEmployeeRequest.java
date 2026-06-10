package kz.whosnext.hr.employeeservice.model.dto.request;

import java.util.UUID;

public record UpdateEmployeeRequest(
        UUID departmentId,
        UUID workScheduleId,
        String positionName,
        String contractType,
        String firstName,
        String lastName,
        String middleName,
        String phone,
        String address
) {}


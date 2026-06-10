package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.EmployeeResponse;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmployeeMapper {

    @Mapping(target = "departmentId", expression = "java(employee.getDepartment() != null ? employee.getDepartment().getId() : null)")
    @Mapping(target = "departmentName", expression = "java(employee.getDepartment() != null ? employee.getDepartment().getName() : null)")
    @Mapping(target = "workScheduleId", expression = "java(employee.getWorkSchedule() != null ? employee.getWorkSchedule().getId() : null)")
    @Mapping(target = "workScheduleName", expression = "java(employee.getWorkSchedule() != null ? employee.getWorkSchedule().getName() : null)")
    @Mapping(target = "status", expression = "java(employee.getStatus().name())")
    @Mapping(target = "photoUrl", ignore = true)
    EmployeeResponse toResponse(Employee employee);
}

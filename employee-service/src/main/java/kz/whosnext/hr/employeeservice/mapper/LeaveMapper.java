package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.LeaveResponse;
import kz.whosnext.hr.employeeservice.model.entity.Leave;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LeaveMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeFullName", expression = "java(leave.getEmployee().getLastName() + \" \" + leave.getEmployee().getFirstName())")
    @Mapping(target = "leaveType", expression = "java(leave.getLeaveType().name())")
    @Mapping(target = "status", expression = "java(leave.getStatus().name())")
    @Mapping(target = "daysCount", expression = "java(leave.getDaysCount() != null ? leave.getDaysCount() : 0)")
    LeaveResponse toResponse(Leave leave);
}


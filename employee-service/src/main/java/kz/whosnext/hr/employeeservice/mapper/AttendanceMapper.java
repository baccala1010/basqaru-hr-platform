package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.AttendanceResponse;
import kz.whosnext.hr.employeeservice.model.entity.AttendanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AttendanceMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeFullName", expression = "java(record.getEmployee().getLastName() + \" \" + record.getEmployee().getFirstName())")
    AttendanceResponse toResponse(AttendanceRecord record);
}


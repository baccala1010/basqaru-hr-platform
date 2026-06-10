package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.TimeEntryResponse;
import kz.whosnext.hr.employeeservice.model.entity.TimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TimeEntryMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeFullName", expression = "java(timeEntry.getEmployee().getLastName() + \" \" + timeEntry.getEmployee().getFirstName())")
    @Mapping(target = "status", expression = "java(timeEntry.getStatus().name())")
    TimeEntryResponse toResponse(TimeEntry timeEntry);
}


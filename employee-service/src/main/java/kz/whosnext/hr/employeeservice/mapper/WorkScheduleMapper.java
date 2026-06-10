package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.WorkScheduleResponse;
import kz.whosnext.hr.employeeservice.model.entity.WorkSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkScheduleMapper {

    @Mapping(target = "type", expression = "java(workSchedule.getType().name())")
    WorkScheduleResponse toResponse(WorkSchedule workSchedule);
}


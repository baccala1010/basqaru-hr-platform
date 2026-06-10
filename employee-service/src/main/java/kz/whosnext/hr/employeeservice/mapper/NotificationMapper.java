package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.NotificationResponse;
import kz.whosnext.hr.employeeservice.model.entity.EmployeeNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

    @Mapping(target = "type", expression = "java(n.getType().name())")
    NotificationResponse toResponse(EmployeeNotification n);
}


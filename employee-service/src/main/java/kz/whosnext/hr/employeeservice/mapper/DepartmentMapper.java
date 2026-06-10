package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.DepartmentResponse;
import kz.whosnext.hr.employeeservice.model.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DepartmentMapper {

    @Mapping(target = "headCount", constant = "0L")
    DepartmentResponse toResponse(Department department);
}


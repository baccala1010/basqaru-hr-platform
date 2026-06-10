package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.PositionResponse;
import kz.whosnext.hr.employeeservice.model.entity.Position;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PositionMapper {

    PositionResponse toResponse(Position position);
}


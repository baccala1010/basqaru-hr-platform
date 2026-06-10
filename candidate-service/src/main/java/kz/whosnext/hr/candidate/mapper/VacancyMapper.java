package kz.whosnext.hr.candidate.mapper;

import kz.whosnext.hr.candidate.model.dto.response.VacancyResponse;
import kz.whosnext.hr.candidate.model.entity.Vacancy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VacancyMapper {

    @Mapping(source = "position.id", target = "positionId")
    @Mapping(source = "position.name", target = "positionName")
    @Mapping(source = "contractType.id", target = "contractTypeId")
    @Mapping(source = "contractType.name", target = "contractTypeName")
    VacancyResponse toResponse(Vacancy vacancy);
}


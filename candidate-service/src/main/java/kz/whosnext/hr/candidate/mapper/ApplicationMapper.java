package kz.whosnext.hr.candidate.mapper;

import kz.whosnext.hr.candidate.model.dto.response.ApplicationResponse;
import kz.whosnext.hr.candidate.model.entity.Application;
import kz.whosnext.hr.candidate.model.entity.ApplicationDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "vacancy.id", target = "vacancyId")
    @Mapping(source = "vacancy.title", target = "vacancyTitle")
    @Mapping(source = "vacancy.company", target = "vacancyCompany")
    ApplicationResponse toResponse(Application application);

    @Mapping(target = "documentType", expression = "java(doc.getDocumentType().name())")
    ApplicationResponse.DocumentResponse toDocResponse(ApplicationDocument doc);
}


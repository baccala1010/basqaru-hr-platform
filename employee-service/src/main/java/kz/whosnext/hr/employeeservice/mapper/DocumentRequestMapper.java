package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.DocumentRequestResponse;
import kz.whosnext.hr.employeeservice.model.entity.DocumentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentRequestMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeFullName", expression = "java(dr.getEmployee().getLastName() + \" \" + dr.getEmployee().getFirstName())")
    @Mapping(target = "documentType", expression = "java(dr.getDocumentType().name())")
    @Mapping(target = "status", expression = "java(dr.getStatus().name())")
    DocumentRequestResponse toResponse(DocumentRequest dr);
}


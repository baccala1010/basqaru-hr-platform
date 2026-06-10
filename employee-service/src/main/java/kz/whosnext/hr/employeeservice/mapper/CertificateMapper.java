package kz.whosnext.hr.employeeservice.mapper;

import kz.whosnext.hr.employeeservice.model.dto.response.CertificateResponse;
import kz.whosnext.hr.employeeservice.model.entity.CertificateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CertificateMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeFullName", expression = "java(cr.getEmployee().getLastName() + \" \" + cr.getEmployee().getFirstName())")
    @Mapping(target = "certificateType", expression = "java(cr.getCertificateType().name())")
    @Mapping(target = "status", expression = "java(cr.getStatus().name())")
    CertificateResponse toResponse(CertificateRequest cr);
}


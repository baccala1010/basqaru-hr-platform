package kz.whosnext.hr.documentservice.mapper;

import kz.whosnext.hr.documentservice.model.dto.response.DocumentResponse;
import kz.whosnext.hr.documentservice.model.dto.response.TemplateResponse;
import kz.whosnext.hr.documentservice.model.entity.Document;
import kz.whosnext.hr.documentservice.model.entity.DocumentTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {

    // fileUrl is a presigned MinIO URL generated at service layer, not stored in entity
    @Mapping(target = "fileUrl", ignore = true)
    DocumentResponse toResponse(Document document);

    TemplateResponse toTemplateResponse(DocumentTemplate template);
}


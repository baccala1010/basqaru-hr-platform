package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.whosnext.hr.candidate.model.enums.DocumentType;

import java.util.List;

@Schema(description = "Обновление заявки кандидатом (при статусе REVISION_REQUESTED)")
public record UpdateApplicationRequest(
        @Schema(description = "Телефон")
        String phone,
        @Schema(description = "Адрес")
        String address,
        @Schema(description = "Вид деятельности")
        String activityType,
        @Schema(description = "Контактная информация")
        String contactInfo,
        @Schema(description = "Фото")
        DocumentRef photo,
        @Schema(description = "Удостоверение личности")
        DocumentRef identityDocument,
        @Schema(description = "Справка о несудимости")
        DocumentRef criminalRecord,
        @Schema(description = "Медицинская справка")
        DocumentRef medicalCertificate,
        @Schema(description = "Дипломы")
        List<DocumentRef> diplomas
) {
    public record DocumentRef(
            String fileId,
            String fileName,
            DocumentType documentType
    ) {}
}

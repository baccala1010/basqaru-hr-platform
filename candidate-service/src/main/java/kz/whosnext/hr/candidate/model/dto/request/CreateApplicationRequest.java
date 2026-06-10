package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import kz.whosnext.hr.candidate.model.enums.DocumentType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Подача заявки на вакансию")
public record CreateApplicationRequest(
        @NotNull(message = "ID вакансии обязателен")
        UUID vacancyId,
        @NotBlank(message = "Имя обязательно")
        String firstName,
        @NotBlank(message = "Фамилия обязательна")
        String lastName,
        @NotNull(message = "Дата рождения обязательна")
        LocalDate birthDate,
        String contactInfo,
        String activityType,
        @NotBlank(message = "ИИН обязателен")
        @Pattern(regexp = "^\\d{12}$", message = "ИИН должен содержать ровно 12 цифр")
        String iin,
        @NotBlank(message = "Телефон обязателен")
        @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Некорректный телефон")
        String phone,
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        String email,
        @NotBlank(message = "Адрес обязателен")
        String address,
        @NotNull(message = "Согласие на обработку данных обязательно")
        @AssertTrue(message = "Необходимо согласие на обработку персональных данных")
        Boolean personalDataConsent,
        @NotNull(message = "Документ удостоверения обязателен")
        DocumentRef identityDocument,
        @NotNull(message = "Фотография обязательна")
        DocumentRef photo,
        DocumentRef criminalRecord,
        DocumentRef medicalCertificate,
        List<DocumentRef> diplomas
) {
    public record DocumentRef(
            String fileId,
            String fileName,
            String filePath,
            DocumentType documentType
    ) {}
}


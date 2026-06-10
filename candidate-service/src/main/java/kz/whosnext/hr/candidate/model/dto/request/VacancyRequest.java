package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kz.whosnext.hr.candidate.model.enums.*;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Создание / обновление вакансии")
public record VacancyRequest(
        @NotBlank(message = "Название обязательно")
        String title,
        @NotBlank(message = "Компания обязательна")
        String company,
        String companyLogoUrl,
        @NotNull(message = "Локация обязательна")
        LocationType location,
        @NotNull(message = "Тип занятости обязателен")
        EmploymentType employmentType,
        @NotNull(message = "Уровень опыта обязателен")
        ExperienceLevel experienceLevel,
        @NotNull(message = "Категория обязательна")
        VacancyCategory category,
        @NotBlank(message = "Описание обязательно")
        String description,
        String shortDescription,
        String skills,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        UUID positionId,
        UUID contractTypeId,
        Boolean requiresCriminalRecord,
        Boolean requiresMedicalCert
) {}


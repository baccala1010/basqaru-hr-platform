package kz.whosnext.hr.candidate.model.dto.response;

import kz.whosnext.hr.candidate.model.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VacancyResponse(
        UUID id,
        String title,
        String company,
        String companyLogoUrl,
        LocationType location,
        EmploymentType employmentType,
        ExperienceLevel experienceLevel,
        VacancyCategory category,
        String description,
        String shortDescription,
        String skills,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        UUID positionId,
        String positionName,
        UUID contractTypeId,
        String contractTypeName,
        Boolean requiresCriminalRecord,
        Boolean requiresMedicalCert,
        Boolean active,
        LocalDateTime createdAt
) {}


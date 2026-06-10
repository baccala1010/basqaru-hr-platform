package kz.whosnext.hr.candidate.model.dto.response;

import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import kz.whosnext.hr.candidate.model.enums.InterviewType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id, UUID candidateId,
        UUID vacancyId,
        String vacancyTitle,
        String vacancyCompany,
        ApplicationStatus status,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String iin,
        String phone,
        String email,
        String address,
        String activityType,
        InterviewType interviewType,
        LocalDateTime interviewDate,
        String interviewMessage,
        String rejectionMessage,
        String revisionMessage,
        String revisionComment,
        Boolean documentsReady,
        Boolean documentsSigned,
        List<DocumentResponse> documents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record DocumentResponse(
            UUID id,
            String documentType,
            String fileId,
            String fileName
    ) {}
}


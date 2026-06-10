package kz.whosnext.hr.candidate.service;

import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import kz.whosnext.hr.candidate.exception.ResourceNotFoundException;
import kz.whosnext.hr.candidate.model.dto.response.ApplicationTimelineResponse;
import kz.whosnext.hr.candidate.model.entity.Application;
import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import kz.whosnext.hr.candidate.model.enums.TimelineStepCode;
import kz.whosnext.hr.candidate.model.enums.TimelineStepStatus;
import kz.whosnext.hr.candidate.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationTimelineService {

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public ApplicationTimelineResponse getTimeline(UUID applicationId, UUID userId, String role) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        if ("CANDIDATE".equals(role) && !application.getCandidate().getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к этой заявке");
        }

        List<ApplicationTimelineResponse.Step> steps = new ArrayList<>();
        steps.add(buildSubmittedStep(application));
        steps.add(buildReviewStep(application));
        steps.add(buildInterviewStep(application));
        steps.add(buildDocumentsPreparationStep(application));
        steps.add(buildDocumentsSigningStep(application));
        steps.add(buildHiredStep(application));

        return new ApplicationTimelineResponse(application.getId(), application.getStatus(), steps);
    }

    private ApplicationTimelineResponse.Step buildSubmittedStep(Application application) {
        return new ApplicationTimelineResponse.Step(
                TimelineStepCode.APPLICATION_SUBMITTED,
                "Заявка подана",
                TimelineStepStatus.COMPLETED,
                "Заявка успешно отправлена",
                application.getCreatedAt()
        );
    }

    private ApplicationTimelineResponse.Step buildReviewStep(Application application) {
        if (application.getStatus() == ApplicationStatus.REVISION_REQUESTED) {
            String message = application.getRevisionComment() != null
                    ? "Требуется доработка: " + application.getRevisionComment()
                    : "Требуется доработка заявки";
            return step(TimelineStepCode.HR_REVIEW, "Рассмотрение HR", TimelineStepStatus.ACTION_REQUIRED, message, application.getUpdatedAt());
        }
        if (application.getStatus() == ApplicationStatus.REJECTED) {
            String message = application.getRejectionMessage() != null
                    ? application.getRejectionMessage()
                    : "Заявка отклонена";
            return step(TimelineStepCode.HR_REVIEW, "Рассмотрение HR", TimelineStepStatus.FAILED, message, application.getUpdatedAt());
        }
        if (application.getStatus() == ApplicationStatus.PENDING) {
            return step(TimelineStepCode.HR_REVIEW, "Рассмотрение HR", TimelineStepStatus.CURRENT,
                    "HR рассматривает вашу заявку", null);
        }
        return step(TimelineStepCode.HR_REVIEW, "Рассмотрение HR", TimelineStepStatus.COMPLETED,
                "Заявка одобрена", application.getUpdatedAt());
    }

    private ApplicationTimelineResponse.Step buildInterviewStep(Application application) {
        if (isFailedFlow(application)) {
            return blockedStep(TimelineStepCode.INTERVIEW, "Собеседование");
        }

        LocalDateTime interviewDate = application.getInterviewDate();
        if (interviewDate == null) {
            if (application.getStatus() == ApplicationStatus.APPROVED || application.getStatus() == ApplicationStatus.COMPLETED) {
                return step(TimelineStepCode.INTERVIEW, "Собеседование", TimelineStepStatus.CURRENT,
                        "Ожидается назначение даты собеседования", null);
            }
            return step(TimelineStepCode.INTERVIEW, "Собеседование", TimelineStepStatus.UPCOMING,
                    "После рассмотрения заявки", null);
        }

        if (!Boolean.TRUE.equals(application.getDocumentsReady())) {
            return step(TimelineStepCode.INTERVIEW, "Собеседование", TimelineStepStatus.CURRENT,
                    "Собеседование назначено на " + interviewDate, interviewDate);
        }

        return step(TimelineStepCode.INTERVIEW, "Собеседование", TimelineStepStatus.COMPLETED,
                application.getInterviewMessage(), interviewDate);
    }

    private ApplicationTimelineResponse.Step buildDocumentsPreparationStep(Application application) {
        if (isFailedFlow(application)) {
            return blockedStep(TimelineStepCode.DOCUMENTS_PREPARATION, "Подготовка документов");
        }
        if (Boolean.TRUE.equals(application.getDocumentsReady())) {
            return step(TimelineStepCode.DOCUMENTS_PREPARATION, "Подготовка документов", TimelineStepStatus.COMPLETED,
                    "Документы готовы к подписанию", application.getUpdatedAt());
        }
        if (application.getStatus() == ApplicationStatus.APPROVED || application.getStatus() == ApplicationStatus.COMPLETED) {
            return step(TimelineStepCode.DOCUMENTS_PREPARATION, "Подготовка документов", TimelineStepStatus.CURRENT,
                    "Формируем документы", null);
        }
        return step(TimelineStepCode.DOCUMENTS_PREPARATION, "Подготовка документов", TimelineStepStatus.UPCOMING,
                "Откроется после одобрения заявки", null);
    }

    private ApplicationTimelineResponse.Step buildDocumentsSigningStep(Application application) {
        if (isFailedFlow(application)) {
            return blockedStep(TimelineStepCode.DOCUMENTS_SIGNING, "Подписание документов");
        }
        if (Boolean.TRUE.equals(application.getDocumentsSigned())) {
            return step(TimelineStepCode.DOCUMENTS_SIGNING, "Подписание документов", TimelineStepStatus.COMPLETED,
                    "Все документы подписаны", application.getUpdatedAt());
        }
        if (Boolean.TRUE.equals(application.getDocumentsReady())) {
            return step(TimelineStepCode.DOCUMENTS_SIGNING, "Подписание документов", TimelineStepStatus.CURRENT,
                    "Ожидается подписание документов", null);
        }
        return step(TimelineStepCode.DOCUMENTS_SIGNING, "Подписание документов", TimelineStepStatus.UPCOMING,
                "Откроется после формирования документов", null);
    }

    private ApplicationTimelineResponse.Step buildHiredStep(Application application) {
        if (application.getStatus() == ApplicationStatus.COMPLETED) {
            return step(TimelineStepCode.HIRED, "Трудоустройство", TimelineStepStatus.COMPLETED,
                    "Вы переведены в статус сотрудника", application.getPromotedAt());
        }
        if (isFailedFlow(application)) {
            return blockedStep(TimelineStepCode.HIRED, "Трудоустройство");
        }
        if (Boolean.TRUE.equals(application.getDocumentsSigned())) {
            return step(TimelineStepCode.HIRED, "Трудоустройство", TimelineStepStatus.CURRENT,
                    "Ожидается финальное подтверждение HR", null);
        }
        return step(TimelineStepCode.HIRED, "Трудоустройство", TimelineStepStatus.UPCOMING,
                "Финальный этап после подписания документов", null);
    }

    private boolean isFailedFlow(Application application) {
        return application.getStatus() == ApplicationStatus.REJECTED;
    }

    private ApplicationTimelineResponse.Step blockedStep(TimelineStepCode code, String title) {
        return step(code, title, TimelineStepStatus.BLOCKED, "Этап недоступен", null);
    }

    private ApplicationTimelineResponse.Step step(TimelineStepCode code, String title, TimelineStepStatus status,
                                                  String message, LocalDateTime occurredAt) {
        return new ApplicationTimelineResponse.Step(code, title, status, message, occurredAt);
    }
}


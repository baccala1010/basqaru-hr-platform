package kz.whosnext.hr.candidate.service;

import kz.whosnext.hr.candidate.event.*;
import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import kz.whosnext.hr.candidate.exception.BadRequestException;
import kz.whosnext.hr.candidate.exception.ResourceNotFoundException;
import kz.whosnext.hr.candidate.mapper.ApplicationMapper;
import kz.whosnext.hr.candidate.model.dto.request.*;
import kz.whosnext.hr.candidate.model.dto.response.ApplicationResponse;
import kz.whosnext.hr.candidate.model.dto.response.MessageResponse;
import kz.whosnext.hr.candidate.model.entity.Application;
import kz.whosnext.hr.candidate.model.entity.ApplicationDocument;
import kz.whosnext.hr.candidate.model.entity.Candidate;
import kz.whosnext.hr.candidate.model.entity.Vacancy;
import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import kz.whosnext.hr.candidate.model.enums.CandidateStatus;
import kz.whosnext.hr.candidate.model.enums.DocumentType;
import kz.whosnext.hr.candidate.model.enums.NotificationType;
import kz.whosnext.hr.candidate.model.enums.ActivityAction;
import kz.whosnext.hr.candidate.model.enums.ActivitySource;
import kz.whosnext.hr.candidate.repository.ApplicationRepository;
import kz.whosnext.hr.candidate.repository.CandidateRepository;
import kz.whosnext.hr.candidate.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final VacancyRepository vacancyRepository;
    private final ApplicationMapper applicationMapper;
    private final KafkaProducerService kafkaProducer;
    private final NotificationService notificationService;
    private final ActivityLogProducer activityLogProducer;

    @Transactional
    public ApplicationResponse create(UUID userId, String role, CreateApplicationRequest req) {
        if (!"CANDIDATE".equals(role)) {
            throw new kz.whosnext.hr.candidate.exception.AccessDeniedException(
                    "Подавать заявки на вакансии могут только кандидаты");
        }

        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль кандидата не найден"));
        Vacancy vacancy = vacancyRepository.findById(req.vacancyId())
                .orElseThrow(() -> new ResourceNotFoundException("Вакансия не найдена"));

        if (!Boolean.TRUE.equals(vacancy.getActive())) {
            throw new BadRequestException("Вакансия деактивирована. Подача заявок невозможна");
        }

        Application app = Application.builder()
                .candidate(candidate).vacancy(vacancy)
                .firstName(req.firstName()).lastName(req.lastName())
                .birthDate(req.birthDate()).iin(req.iin())
                .phone(req.phone()).email(req.email()).address(req.address())
                .activityType(req.activityType()).contactInfo(req.contactInfo())
                .personalDataConsent(req.personalDataConsent())
                .build();

        List<ApplicationDocument> docs = new ArrayList<>();
        addDoc(docs, app, req.identityDocument());
        addDoc(docs, app, req.photo());
        if (req.criminalRecord() != null) addDoc(docs, app, req.criminalRecord());
        if (req.medicalCertificate() != null) addDoc(docs, app, req.medicalCertificate());
        if (req.diplomas() != null) req.diplomas().forEach(d -> addDoc(docs, app, d));
        app.setDocuments(docs);

        Application saved = applicationRepository.save(app);

        kafkaProducer.sendApplicationSubmitted(new ApplicationSubmittedEvent(
                saved.getId(), candidate.getId(), vacancy.getId(), candidate.getEmail(), vacancy.getTitle()));

        notificationService.create(userId, "Заявка подана",
                "Ваша заявка на вакансию «" + vacancy.getTitle() + "» успешно отправлена",
                NotificationType.APPLICATION_SUBMITTED, saved.getId(), "APPLICATION");

        activityLogProducer.log(
                ActivityAction.APPLICATION_SUBMITTED, ActivitySource.CANDIDATE_SERVICE,
                userId, candidate.getEmail(), "CANDIDATE",
                "Application", saved.getId().toString(),
                "Application submitted for vacancy: " + vacancy.getTitle());

        return applicationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable) {
        return applicationRepository.findByCandidateUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(applicationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getAllApplications(ApplicationStatus status, Pageable pageable) {
        if (status != null) return applicationRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(applicationMapper::toResponse);
        return applicationRepository.findAllByOrderByCreatedAtDesc(pageable).map(applicationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID id) {
        return applicationMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getApplicationDetails(UUID id) {
        Application app = findOrThrow(id);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("applicationId", app.getId());
        details.put("candidateId", app.getCandidate().getId());
        details.put("vacancyId", app.getVacancy().getId());
        details.put("vacancyTitle", app.getVacancy().getTitle());
        details.put("firstName", app.getFirstName());
        details.put("lastName", app.getLastName());
        details.put("iin", app.getIin());
        details.put("email", app.getEmail());
        details.put("phone", app.getPhone());
        details.put("status", app.getStatus().name());
        details.put("positionName", app.getVacancy().getTitle());
        details.put("userId", app.getCandidate().getUserId());
        details.put("documentsReady", app.getDocumentsReady());
        details.put("documentsSigned", app.getDocumentsSigned());
        return details;
    }

    @Transactional
    public ApplicationResponse approve(UUID id, ApproveApplicationRequest req, UUID hrUserId, String hrEmail, String hrRole) {
        Application app = findOrThrow(id);
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.APPROVED);
        app.setInterviewMessage(req.message() != null ? req.message() : "Поздравляем, ваша заявка одобрена!");
        applicationRepository.save(app);

        kafkaProducer.sendStatusChanged(new ApplicationStatusChangedEvent(app.getId(), app.getCandidate().getId(), oldStatus.name(), "APPROVED"));
        kafkaProducer.sendApplicationApproved(new ApplicationApprovedEvent(app.getId(), app.getCandidate().getId(),
                app.getVacancy().getId(), app.getEmail(), app.getFirstName(), app.getLastName(), app.getIin(),
                app.getCandidate().getUserId()));

        notificationService.create(app.getCandidate().getUserId(), "Заявка одобрена",
                app.getInterviewMessage(), NotificationType.APPLICATION_APPROVED, app.getId(), "APPLICATION");

        activityLogProducer.log(
                ActivityAction.APPLICATION_APPROVED, ActivitySource.CANDIDATE_SERVICE,
                hrUserId, hrEmail, hrRole,
                "Application", app.getId().toString(),
                "Application approved by HR user " + hrUserId);

        return applicationMapper.toResponse(app);
    }

    @Transactional
    public ApplicationResponse reject(UUID id, RejectApplicationRequest req, UUID actorId, String actorEmail, String actorRole) {
        Application app = findOrThrow(id);
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionMessage(req.message() != null ? req.message() : "К сожалению, ваша заявка была отменена");
        applicationRepository.save(app);

        kafkaProducer.sendStatusChanged(new ApplicationStatusChangedEvent(app.getId(), app.getCandidate().getId(), oldStatus.name(), "REJECTED"));
        notificationService.create(app.getCandidate().getUserId(), "Заявка отклонена",
                app.getRejectionMessage(), NotificationType.APPLICATION_REJECTED, app.getId(), "APPLICATION");

        activityLogProducer.log(
                ActivityAction.APPLICATION_REJECTED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Application", app.getId().toString(),
                app.getRejectionMessage());

        return applicationMapper.toResponse(app);
    }

    @Transactional
    public ApplicationResponse requestRevision(UUID id, RevisionRequest req, UUID actorId, String actorEmail, String actorRole) {
        Application app = findOrThrow(id);
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.REVISION_REQUESTED);
        app.setRevisionMessage("Ваша заявка отправлена на доработку");
        app.setRevisionComment(req.comment());
        applicationRepository.save(app);

        kafkaProducer.sendStatusChanged(new ApplicationStatusChangedEvent(app.getId(), app.getCandidate().getId(), oldStatus.name(), "REVISION_REQUESTED"));
        notificationService.create(app.getCandidate().getUserId(), "Требуется доработка",
                "Ваша заявка отправлена на доработку. " + (req.comment() != null ? req.comment() : ""),
                NotificationType.REVISION_REQUESTED, app.getId(), "APPLICATION");

        activityLogProducer.log(
                ActivityAction.APPLICATION_REVISION_REQUESTED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Application", app.getId().toString(),
                "Revision requested: " + (req.comment() != null ? req.comment() : ""));

        return applicationMapper.toResponse(app);
    }

    @Transactional
    public MessageResponse promote(UUID id, UUID hrUserId) {
        Application app = findOrThrow(id);
        if (app.getStatus() != ApplicationStatus.APPROVED) throw new BadRequestException("Заявка должна быть одобрена");
        if (!Boolean.TRUE.equals(app.getDocumentsReady())) throw new BadRequestException("Документы ещё не сформированы");
        if (!Boolean.TRUE.equals(app.getDocumentsSigned())) throw new BadRequestException("Документы не подписаны кандидатом");

        app.setStatus(ApplicationStatus.COMPLETED);
        app.setPromotedAt(LocalDateTime.now());
        app.setPromotedBy(hrUserId);
        applicationRepository.save(app);

        Candidate candidate = app.getCandidate();
        candidate.setStatus(CandidateStatus.PROMOTED);
        candidateRepository.save(candidate);

        kafkaProducer.sendCandidatePromoted(new CandidatePromotedEvent(candidate.getUserId(), "EMPLOYEE"));

        notificationService.create(candidate.getUserId(), "Вы стали сотрудником!",
                "Поздравляем! Вы успешно приняты на работу",
                NotificationType.PROMOTED, app.getId(), "APPLICATION");

        activityLogProducer.log(
                ActivityAction.APPLICATION_PROMOTED, ActivitySource.CANDIDATE_SERVICE,
                hrUserId, "", "",
                "Application", app.getId().toString(),
                "Candidate promoted to employee: " + candidate.getUserId());

        return new MessageResponse("Кандидат переведён в статус сотрудника");
    }

    @Transactional
    public ApplicationResponse updateApplication(UUID id, UUID userId, UpdateApplicationRequest req) {
        Application app = findOrThrow(id);

        if (app.getStatus() != ApplicationStatus.REVISION_REQUESTED)
            throw new BadRequestException("Заявку можно редактировать только в статусе REVISION_REQUESTED");

        if (!app.getCandidate().getUserId().equals(userId))
            throw new AccessDeniedException("Вы можете редактировать только свою заявку");

        if (req.phone() != null) app.setPhone(req.phone());
        if (req.address() != null) app.setAddress(req.address());
        if (req.activityType() != null) app.setActivityType(req.activityType());
        if (req.contactInfo() != null) app.setContactInfo(req.contactInfo());

        boolean hasDocs = req.photo() != null || req.identityDocument() != null
                || req.criminalRecord() != null || req.medicalCertificate() != null
                || (req.diplomas() != null && !req.diplomas().isEmpty());

        if (hasDocs) {
            List<ApplicationDocument> managed = app.getDocuments();
            managed.clear();
            addDoc(managed, app, req.identityDocument());
            addDoc(managed, app, req.photo());
            if (req.criminalRecord() != null) addDoc(managed, app, req.criminalRecord());
            if (req.medicalCertificate() != null) addDoc(managed, app, req.medicalCertificate());
            if (req.diplomas() != null) req.diplomas().forEach(d -> addDoc(managed, app, d));
        }

        app.setStatus(ApplicationStatus.PENDING);
        app.setRevisionMessage(null);
        app.setRevisionComment(null);

        applicationRepository.save(app);

        kafkaProducer.sendStatusChanged(new ApplicationStatusChangedEvent(
                app.getId(), app.getCandidate().getId(), "REVISION_REQUESTED", "PENDING"));

        kafkaProducer.sendApplicationUpdated(new ApplicationUpdatedEvent(
                app.getId(), app.getCandidate().getId(), app.getEmail()));

        activityLogProducer.log(
                ActivityAction.APPLICATION_UPDATED, ActivitySource.CANDIDATE_SERVICE,
                userId, app.getEmail(), "CANDIDATE",
                "Application", app.getId().toString(),
                "Application updated after revision request");

        return applicationMapper.toResponse(app);
    }

    private void addDoc(List<ApplicationDocument> docs, Application app, CreateApplicationRequest.DocumentRef ref) {
        if (ref == null) return;
        docs.add(ApplicationDocument.builder()
                .application(app)
                .documentType(ref.documentType() != null ? ref.documentType() : DocumentType.CERTIFICATE)
                .fileId(ref.fileId()).fileName(ref.fileName()).filePath(ref.filePath()).build());
    }

    private void addDoc(List<ApplicationDocument> docs, Application app, UpdateApplicationRequest.DocumentRef ref) {
        if (ref == null) return;
        docs.add(ApplicationDocument.builder()
                .application(app)
                .documentType(ref.documentType() != null ? ref.documentType() : DocumentType.CERTIFICATE)
                .fileId(ref.fileId()).fileName(ref.fileName()).build());
    }

    private Application findOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
    }
}


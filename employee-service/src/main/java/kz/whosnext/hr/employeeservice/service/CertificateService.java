package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.CertificateMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.CertificateRequestDto;
import kz.whosnext.hr.employeeservice.model.dto.response.CertificateResponse;
import kz.whosnext.hr.employeeservice.model.entity.CertificateRequest;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.enums.CertificateStatus;
import kz.whosnext.hr.employeeservice.model.enums.CertificateType;
import kz.whosnext.hr.employeeservice.model.enums.NotificationType;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.CertificateRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRequestRepository certificateRequestRepository;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final CertificateMapper certificateMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public Page<CertificateResponse> list(UUID employeeId, String status, Pageable pageable) {
        if (employeeId != null) {
            return certificateRequestRepository.findByEmployeeId(employeeId, pageable).map(certificateMapper::toResponse);
        }
        if (status != null) {
            return certificateRequestRepository.findByStatus(CertificateStatus.valueOf(status), pageable).map(certificateMapper::toResponse);
        }
        return certificateRequestRepository.findAll(pageable).map(certificateMapper::toResponse);
    }

    @Transactional
    public CertificateResponse request(CertificateRequestDto req, UUID actorId, String actorEmail, String actorRole) {
        Employee employee = employeeService.findById(req.employeeId());

        CertificateRequest cr = CertificateRequest.builder()
                .employee(employee)
                .certificateType(CertificateType.valueOf(req.certificateType()))
                .status(CertificateStatus.PENDING)
                .build();

        cr = certificateRequestRepository.save(cr);
        log.info("Запрос справки создан: id={}, type={}", cr.getId(), req.certificateType());

        activityLogProducer.log(
                ActivityAction.CERTIFICATE_REQUESTED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "CertificateRequest", cr.getId().toString(),
                "Certificate requested: " + req.certificateType());

        return certificateMapper.toResponse(cr);
    }

    @Transactional
    public CertificateResponse complete(UUID id, UUID documentId, UUID actorId, String actorEmail, String actorRole) {
        CertificateRequest cr = findById(id);
        cr.setStatus(CertificateStatus.GENERATED);
        cr.setGeneratedDocumentId(documentId);
        cr = certificateRequestRepository.save(cr);

        notificationService.createByEmployeeId(cr.getEmployee().getId(),
                "Справка готова",
                "Ваша справка типа " + cr.getCertificateType().name() + " готова",
                NotificationType.CERTIFICATE_READY, cr.getId(), "CERTIFICATE");

        log.info("Справка сформирована: id={}", id);

        activityLogProducer.log(
                ActivityAction.CERTIFICATE_COMPLETED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "CertificateRequest", id.toString(),
                "Certificate completed: " + cr.getCertificateType().name());

        return certificateMapper.toResponse(cr);
    }

    @Transactional
    public CertificateResponse reject(UUID id, String reason, UUID actorId, String actorEmail, String actorRole) {
        CertificateRequest cr = findById(id);
        cr.setStatus(CertificateStatus.REJECTED);
        cr.setRejectReason(reason);
        cr = certificateRequestRepository.save(cr);
        log.info("Справка отклонена: id={}", id);

        activityLogProducer.log(
                ActivityAction.CERTIFICATE_REJECTED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "CertificateRequest", id.toString(),
                "Certificate rejected: " + reason);

        return certificateMapper.toResponse(cr);
    }

    public CertificateRequest findById(UUID id) {
        return certificateRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос справки не найден: " + id));
    }
}


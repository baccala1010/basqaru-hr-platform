package kz.whosnext.hr.candidate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import kz.whosnext.hr.candidate.model.dto.request.*;
import kz.whosnext.hr.candidate.model.dto.response.ApplicationResponse;
import kz.whosnext.hr.candidate.model.dto.response.ApplicationTimelineResponse;
import kz.whosnext.hr.candidate.model.dto.response.MessageResponse;
import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import kz.whosnext.hr.candidate.service.ApplicationService;
import kz.whosnext.hr.candidate.service.ApplicationTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Заявки")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationTimelineService applicationTimelineService;

    @PostMapping
    @Operation(summary = "Подать заявку на вакансию (только CANDIDATE)")
    public ResponseEntity<ApplicationResponse> create(@RequestHeader("X-User-Id") UUID userId,
                                                       @RequestHeader("X-User-Role") String role,
                                                       @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.create(userId, role, request));
    }

    @GetMapping("/my")
    @Operation(summary = "Мои заявки (кандидат)")
    public ResponseEntity<Page<ApplicationResponse>> myApplications(@RequestHeader("X-User-Id") UUID userId,
                                                                      @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(applicationService.getMyApplications(userId, pageable));
    }

    @GetMapping
    @Operation(summary = "Все заявки (HR)")
    public ResponseEntity<Page<ApplicationResponse>> allApplications(@RequestHeader("X-User-Role") String role,
                                                                       @RequestParam(required = false) ApplicationStatus status,
                                                                       @PageableDefault(size = 20) Pageable pageable) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(applicationService.getAllApplications(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали заявки")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Детальная информация о заявке для document-service (HR/ADMIN)")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getApplicationDetails(id));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Timeline этапов заявки")
    public ResponseEntity<ApplicationTimelineResponse> getTimeline(@PathVariable UUID id,
                                                                   @RequestHeader("X-User-Id") UUID userId,
                                                                   @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(applicationTimelineService.getTimeline(id, userId, role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить заявку после доработки (только CANDIDATE, статус REVISION_REQUESTED)")
    public ResponseEntity<ApplicationResponse> update(@PathVariable UUID id,
                                                       @RequestHeader("X-User-Id") UUID userId,
                                                       @RequestHeader("X-User-Role") String role,
                                                       @RequestBody UpdateApplicationRequest request) {
        if (!"CANDIDATE".equals(role))
            throw new AccessDeniedException("Только кандидат может обновлять заявку");
        return ResponseEntity.ok(applicationService.updateApplication(id, userId, request));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Одобрить заявку (HR)")
    public ResponseEntity<ApplicationResponse> approve(@PathVariable UUID id,
                                                         @RequestHeader("X-User-Id") UUID hrUserId,
                                                         @RequestHeader("X-User-Email") String hrEmail,
                                                         @RequestHeader("X-User-Role") String role,
                                                         @RequestBody(required = false) ApproveApplicationRequest request) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(applicationService.approve(id, request != null ? request : new ApproveApplicationRequest(null), hrUserId, hrEmail, role));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Отклонить заявку (HR)")
    public ResponseEntity<ApplicationResponse> reject(@PathVariable UUID id,
                                                        @RequestHeader("X-User-Id") UUID actorId,
                                                        @RequestHeader("X-User-Email") String actorEmail,
                                                        @RequestHeader("X-User-Role") String role,
                                                        @RequestBody(required = false) RejectApplicationRequest request) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(applicationService.reject(id, request != null ? request : new RejectApplicationRequest(null), actorId, actorEmail, role));
    }

    @PostMapping("/{id}/request-revision")
    @Operation(summary = "Запросить доработку (HR)")
    public ResponseEntity<ApplicationResponse> requestRevision(@PathVariable UUID id,
                                                                 @RequestHeader("X-User-Id") UUID actorId,
                                                                 @RequestHeader("X-User-Email") String actorEmail,
                                                                 @RequestHeader("X-User-Role") String role,
                                                                 @RequestBody RevisionRequest request) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(applicationService.requestRevision(id, request, actorId, actorEmail, role));
    }

    @PostMapping("/{id}/promote")
    @Operation(summary = "Перевести в сотрудника (HR, ADMIN)")
    public ResponseEntity<MessageResponse> promote(@PathVariable UUID id,
                                                     @RequestHeader("X-User-Id") UUID hrUserId,
                                                     @RequestHeader("X-User-Role") String role) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(applicationService.promote(id, hrUserId));
    }

    private void checkHrOrAdmin(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для HR и ADMIN");
    }
}


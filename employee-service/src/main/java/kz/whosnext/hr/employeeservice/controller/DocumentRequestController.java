package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateDocumentRequestDto;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateDocumentRequestStatusDto;
import kz.whosnext.hr.employeeservice.model.dto.response.DocumentRequestResponse;
import kz.whosnext.hr.employeeservice.service.DocumentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/document-requests")
@RequiredArgsConstructor
public class DocumentRequestController {

    private final DocumentRequestService documentRequestService;

    @PostMapping
    public ResponseEntity<DocumentRequestResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateDocumentRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentRequestService.create(userId, email, role, req));
    }

    @GetMapping("/my")
    public Page<DocumentRequestResponse> myRequests(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return documentRequestService.getMyRequests(userId, pageable);
    }

    @GetMapping
    public Page<DocumentRequestResponse> all(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        checkHrOrAdminOrDirector(role);
        return documentRequestService.getAll(status, pageable);
    }

    @PatchMapping("/{id}/status")
    public DocumentRequestResponse updateStatus(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID actorId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody UpdateDocumentRequestStatusDto req) {
        checkHrOrAdmin(role);
        return documentRequestService.updateStatus(id, req, actorId, email, role);
    }

    @PostMapping("/{id}/sign")
    public DocumentRequestResponse sign(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID directorUserId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        if (!"DIRECTOR".equals(role))
            throw new AccessDeniedException("Подписание доступно только DIRECTOR");
        return documentRequestService.sign(id, directorUserId, email, role);
    }

    private void checkHrOrAdmin(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для HR и ADMIN");
    }

    private void checkHrOrAdminOrDirector(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role) && !"DIRECTOR".equals(role))
            throw new AccessDeniedException("Доступ только для HR, ADMIN и DIRECTOR");
    }
}


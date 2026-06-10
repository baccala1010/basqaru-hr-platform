package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.model.dto.request.CertificateRequestDto;
import kz.whosnext.hr.employeeservice.model.dto.response.CertificateResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public Page<CertificateResponse> list(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return certificateService.list(employeeId, status, pageable);
    }

    @PostMapping
    public ResponseEntity<CertificateResponse> request(@RequestHeader("X-User-Id") UUID userId,
                                                       @RequestHeader("X-User-Email") String email,
                                                       @RequestHeader("X-User-Role") String role,
                                                       @Valid @RequestBody CertificateRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.request(req, userId, email, role));
    }

    @PatchMapping("/{id}/complete")
    public CertificateResponse complete(@PathVariable UUID id,
                                        @RequestParam(required = false) UUID documentId,
                                        @RequestHeader("X-User-Id") UUID userId,
                                        @RequestHeader("X-User-Email") String email,
                                        @RequestHeader("X-User-Role") String role) {
        return certificateService.complete(id, documentId, userId, email, role);
    }

    @PatchMapping("/{id}/reject")
    public CertificateResponse reject(@PathVariable UUID id,
                                      @RequestParam String reason,
                                      @RequestHeader("X-User-Id") UUID userId,
                                      @RequestHeader("X-User-Email") String email,
                                      @RequestHeader("X-User-Role") String role) {
        return certificateService.reject(id, reason, userId, email, role);
    }
}


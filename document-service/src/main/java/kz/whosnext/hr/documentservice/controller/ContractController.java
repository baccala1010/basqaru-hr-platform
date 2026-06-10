package kz.whosnext.hr.documentservice.controller;

import kz.whosnext.hr.documentservice.model.dto.request.EcpSignRequest;
import kz.whosnext.hr.documentservice.model.dto.request.SignDocumentRequest;
import kz.whosnext.hr.documentservice.model.dto.response.DocumentResponse;
import kz.whosnext.hr.documentservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.documentservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping("/{applicationId}")
    public List<DocumentResponse> getByApplication(
            @PathVariable UUID applicationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role) {
        return contractService.getContractsByApplication(applicationId, userId, role);
    }

    @PostMapping("/{documentId}/sign")
    public DocumentResponse sign(
            @PathVariable UUID documentId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ip) {
        return contractService.signDocument(documentId, userId, ip != null ? ip : "unknown");
    }

    @PostMapping("/{documentId}/sign-ecp")
    public DocumentResponse signEcp(
            @PathVariable UUID documentId,
            @RequestBody EcpSignRequest req,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        return contractService.signWithEcp(documentId, req.signedXml(), userId);
    }

    @GetMapping("/{documentId}/verify")
    public Map<String, Object> verify(@PathVariable UUID documentId) {
        return contractService.verifyDocument(documentId);
    }

    @GetMapping("/{documentId}/signature")
    public Map<String, Object> signature(@PathVariable UUID documentId) {
        return contractService.getSignatureInfo(documentId);
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID documentId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role) {
        return contractService.downloadFile(documentId, userId, role);
    }

    @PostMapping("/regenerate/{applicationId}")
    public List<DocumentResponse> regenerate(
            @PathVariable UUID applicationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        return contractService.regenerate(applicationId, role, userId, email);
    }
}


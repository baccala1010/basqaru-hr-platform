package kz.whosnext.hr.documentservice.controller;

import kz.whosnext.hr.documentservice.model.dto.request.GenerateCertificateRequest;
import kz.whosnext.hr.documentservice.model.dto.response.DocumentResponse;
import kz.whosnext.hr.documentservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.documentservice.service.CertificateService;
import kz.whosnext.hr.documentservice.service.DocumentService;
import kz.whosnext.hr.documentservice.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final DocumentService documentService;
    private final MinioService minioService;

    @PostMapping("/generate")
    public ResponseEntity<DocumentResponse> generate(
            @Valid @RequestBody GenerateCertificateRequest req,
            @RequestHeader("X-User-Id") UUID requestedBy,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        var doc = certificateService.generate(
                req.employeeId(),
                req.certificateType(),
                req.employeeFullName(),
                req.position(),
                req.department(),
                req.hireDate(),
                requestedBy, email, role
        );
        return ResponseEntity.ok(documentService.getById(doc.getId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) throws Exception {
        var doc = documentService.findById(id);
        try (InputStream stream = minioService.downloadFile("hr-documents", doc.getMinioObjectKey())) {
            byte[] bytes = stream.readAllBytes();
            String ct = (doc.getContentType() != null && !doc.getContentType().isBlank())
                    ? doc.getContentType()
                    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .contentType(MediaType.valueOf(ct))
                    .body(bytes);
        }
    }
}


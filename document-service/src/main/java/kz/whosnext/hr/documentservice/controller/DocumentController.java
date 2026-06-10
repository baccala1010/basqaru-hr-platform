package kz.whosnext.hr.documentservice.controller;

import kz.whosnext.hr.documentservice.model.dto.response.DocumentResponse;
import kz.whosnext.hr.documentservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) UUID vacancyId,
            @RequestParam String documentType,
            @RequestHeader("X-User-Id") UUID uploadedBy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(file, applicationId, candidateId, vacancyId, documentType, uploadedBy));
    }

    @GetMapping
    public Page<DocumentResponse> list(
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) String documentType,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return documentService.list(applicationId, candidateId, documentType, role, userId, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") UUID userId) throws Exception {
        try (InputStream stream = documentService.download(id, role, userId)) {
            byte[] bytes = stream.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        documentService.delete(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Документ удалён"));
    }
}

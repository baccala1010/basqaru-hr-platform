package kz.whosnext.hr.documentservice.controller;

import kz.whosnext.hr.documentservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.documentservice.model.dto.response.TemplateResponse;
import kz.whosnext.hr.documentservice.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public List<TemplateResponse> listAll() {
        return templateService.listAll();
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam String documentType,
            @RequestParam(required = false) String description,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.upload(file, name, documentType, description, userId, role));
    }

    @PutMapping("/{id}")
    public TemplateResponse update(
            @PathVariable UUID id,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        return templateService.update(id, file, name, description, role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        templateService.delete(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Шаблон удалён"));
    }
}


package kz.whosnext.hr.candidate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.whosnext.hr.candidate.model.dto.request.UpdateCandidateRequest;
import kz.whosnext.hr.candidate.model.dto.response.CandidateResponse;
import kz.whosnext.hr.candidate.model.dto.response.MessageResponse;
import kz.whosnext.hr.candidate.service.CandidateService;
import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Tag(name = "Профиль кандидата")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping("/me")
    @Operation(summary = "Мой профиль")
    public ResponseEntity<CandidateResponse> getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(candidateService.getMyProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль")
    public ResponseEntity<CandidateResponse> updateProfile(@RequestHeader("X-User-Id") UUID userId,
                                                            @Valid @RequestBody UpdateCandidateRequest request) {
        return ResponseEntity.ok(candidateService.updateProfile(userId, request));
    }

    @DeleteMapping("/by-user/{userId}")
    @Operation(summary = "[ADMIN] Удалить кандидата и все его заявки")
    public ResponseEntity<MessageResponse> deleteByUserId(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID actorId,
            @RequestHeader("X-User-Email") String actorEmail,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role))
            throw new AccessDeniedException("Только ADMIN может удалять кандидатов");
        candidateService.deleteByUserId(userId, actorId, actorEmail);
        return ResponseEntity.ok(new MessageResponse("Кандидат и все его заявки удалены"));
    }
}


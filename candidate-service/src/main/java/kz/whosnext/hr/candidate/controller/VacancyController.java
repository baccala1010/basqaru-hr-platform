package kz.whosnext.hr.candidate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import kz.whosnext.hr.candidate.model.dto.request.VacancyRequest;
import kz.whosnext.hr.candidate.model.dto.response.MessageResponse;
import kz.whosnext.hr.candidate.model.dto.response.VacancyResponse;
import kz.whosnext.hr.candidate.model.enums.*;
import kz.whosnext.hr.candidate.service.VacancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vacancies")
@RequiredArgsConstructor
@Tag(name = "Вакансии")
public class VacancyController {

    private final VacancyService vacancyService;

    @GetMapping
    @Operation(summary = "Поиск вакансий (публичный)")
    public ResponseEntity<Page<VacancyResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocationType location,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) VacancyCategory category,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) Integer daysAgo,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        String search = keyword != null ? keyword : q;
        return ResponseEntity.ok(vacancyService.search(search, location, employmentType,
                experienceLevel, category, salaryMin, salaryMax, daysAgo, isActive, includeInactive, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали вакансии")
    public ResponseEntity<VacancyResponse> getById(@PathVariable UUID id,
                                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || "CANDIDATE".equals(role)) {
            return ResponseEntity.ok(vacancyService.getByIdForCandidate(id));
        }
        return ResponseEntity.ok(vacancyService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Создать вакансию (HR, ADMIN)")
    public ResponseEntity<VacancyResponse> create(@RequestHeader("X-User-Id") UUID userId,
                                                    @RequestHeader("X-User-Role") String role,
                                                    @Valid @RequestBody VacancyRequest request) {
        checkHrOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(vacancyService.create(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить вакансию (HR, ADMIN)")
    public ResponseEntity<VacancyResponse> update(@PathVariable UUID id,
                                                    @RequestHeader("X-User-Id") UUID userId,
                                                    @RequestHeader("X-User-Email") String email,
                                                    @RequestHeader("X-User-Role") String role,
                                                    @Valid @RequestBody VacancyRequest request) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(vacancyService.update(id, request, userId, email, role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Деактивировать вакансию (HR, ADMIN)")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                    @RequestHeader("X-User-Role") String role) {
        checkHrOrAdmin(role);
        vacancyService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Вакансия деактивирована"));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Деактивировать вакансию (HR, ADMIN)")
    public ResponseEntity<VacancyResponse> deactivate(@PathVariable UUID id,
                                                       @RequestHeader("X-User-Id") UUID userId,
                                                       @RequestHeader("X-User-Email") String email,
                                                       @RequestHeader("X-User-Role") String role) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(vacancyService.deactivate(id, userId, email, role));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Активировать вакансию (HR, ADMIN)")
    public ResponseEntity<VacancyResponse> activate(@PathVariable UUID id,
                                                    @RequestHeader("X-User-Id") UUID userId,
                                                    @RequestHeader("X-User-Email") String email,
                                                    @RequestHeader("X-User-Role") String role) {
        checkHrOrAdmin(role);
        return ResponseEntity.ok(vacancyService.activate(id, userId, email, role));
    }

    private void checkHrOrAdmin(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для HR и ADMIN");
    }
}


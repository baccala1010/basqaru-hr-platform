package kz.whosnext.hr.candidate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import kz.whosnext.hr.candidate.model.dto.request.DictionaryRequest;
import kz.whosnext.hr.candidate.model.dto.response.DictionaryResponse;
import kz.whosnext.hr.candidate.model.dto.response.MessageResponse;
import kz.whosnext.hr.candidate.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
@Tag(name = "Справочники")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/positions")
    @Operation(summary = "Список должностей")
    public List<DictionaryResponse> getAllPositions(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        return dictionaryService.getAllPositions();
    }

    @PostMapping("/positions")
    @Operation(summary = "Создать должность")
    public ResponseEntity<DictionaryResponse> createPosition(@RequestHeader("X-User-Id") UUID userId,
                                                             @RequestHeader("X-User-Email") String email,
                                                             @RequestHeader("X-User-Role") String role,
                                                             @Valid @RequestBody DictionaryRequest request) {
        checkAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.createPosition(request, userId, email, role));
    }

    @PutMapping("/positions/{id}")
    @Operation(summary = "Обновить должность")
    public DictionaryResponse updatePosition(@PathVariable UUID id,
                                             @RequestHeader("X-User-Id") UUID userId,
                                             @RequestHeader("X-User-Email") String email,
                                             @RequestHeader("X-User-Role") String role,
                                             @Valid @RequestBody DictionaryRequest request) {
        checkAdmin(role);
        return dictionaryService.updatePosition(id, request, userId, email, role);
    }

    @DeleteMapping("/positions/{id}")
    @Operation(summary = "Удалить должность")
    public ResponseEntity<MessageResponse> deletePosition(@PathVariable UUID id,
                                                          @RequestHeader("X-User-Id") UUID userId,
                                                          @RequestHeader("X-User-Email") String email,
                                                          @RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        dictionaryService.deletePosition(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Должность удалена"));
    }

    @GetMapping("/contract-types")
    @Operation(summary = "Список типов договоров")
    public List<DictionaryResponse> getAllContractTypes(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        return dictionaryService.getAllContractTypes();
    }

    @PostMapping("/contract-types")
    @Operation(summary = "Создать тип договора")
    public ResponseEntity<DictionaryResponse> createContractType(@RequestHeader("X-User-Id") UUID userId,
                                                                 @RequestHeader("X-User-Email") String email,
                                                                 @RequestHeader("X-User-Role") String role,
                                                                 @Valid @RequestBody DictionaryRequest request) {
        checkAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.createContractType(request, userId, email, role));
    }

    @PutMapping("/contract-types/{id}")
    @Operation(summary = "Обновить тип договора")
    public DictionaryResponse updateContractType(@PathVariable UUID id,
                                                 @RequestHeader("X-User-Id") UUID userId,
                                                 @RequestHeader("X-User-Email") String email,
                                                 @RequestHeader("X-User-Role") String role,
                                                 @Valid @RequestBody DictionaryRequest request) {
        checkAdmin(role);
        return dictionaryService.updateContractType(id, request, userId, email, role);
    }

    @DeleteMapping("/contract-types/{id}")
    @Operation(summary = "Удалить тип договора")
    public ResponseEntity<MessageResponse> deleteContractType(@PathVariable UUID id,
                                                              @RequestHeader("X-User-Id") UUID userId,
                                                              @RequestHeader("X-User-Email") String email,
                                                              @RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        dictionaryService.deleteContractType(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Тип договора удалён"));
    }

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для ADMIN");
    }
}


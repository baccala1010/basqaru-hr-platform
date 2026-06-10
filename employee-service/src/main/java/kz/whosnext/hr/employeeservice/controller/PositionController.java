package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.model.dto.request.CreatePositionRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdatePositionRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.PositionResponse;
import kz.whosnext.hr.employeeservice.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @GetMapping
    public List<PositionResponse> listAll() {
        return positionService.listAll();
    }

    @GetMapping("/{id}")
    public PositionResponse getById(@PathVariable UUID id) {
        return positionService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PositionResponse> create(@RequestHeader("X-User-Id") UUID userId,
                                                   @RequestHeader("X-User-Email") String email,
                                                   @RequestHeader("X-User-Role") String role,
                                                   @Valid @RequestBody CreatePositionRequest req) {
        checkHrOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(positionService.create(req, userId, email, role));
    }

    @PutMapping("/{id}")
    public PositionResponse update(@PathVariable UUID id,
                                   @RequestHeader("X-User-Id") UUID userId,
                                   @RequestHeader("X-User-Email") String email,
                                   @RequestHeader("X-User-Role") String role,
                                   @Valid @RequestBody UpdatePositionRequest req) {
        checkHrOrAdmin(role);
        return positionService.update(id, req, userId, email, role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        checkHrOrAdmin(role);
        positionService.delete(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Должность удалена"));
    }

    private void checkHrOrAdmin(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для HR и ADMIN");
    }
}
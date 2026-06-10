package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateEmployeeRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateEmployeeRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.EmployeeResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Page<EmployeeResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return employeeService.list(search, departmentId, status, pageable);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable UUID id) {
        return employeeService.getById(id);
    }

    @GetMapping("/by-user/{userId}")
    public EmployeeResponse getByUserId(@PathVariable UUID userId) {
        return employeeService.getByUserId(userId);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest req,
                                                   @RequestHeader("X-User-Id") UUID userId,
                                                   @RequestHeader("X-User-Email") String email,
                                                   @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(req, userId, email, role));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest req,
                                   @RequestHeader("X-User-Id") UUID userId,
                                   @RequestHeader("X-User-Email") String email,
                                   @RequestHeader("X-User-Role") String role) {
        return employeeService.update(id, req, userId, email, role);
    }

    @PatchMapping("/{id}/dismiss")
    public EmployeeResponse dismiss(@PathVariable UUID id,
                                    @RequestHeader("X-User-Id") UUID actorId,
                                    @RequestHeader("X-User-Email") String email,
                                    @RequestHeader("X-User-Role") String role) {
        return employeeService.dismiss(id, actorId, email, role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @RequestHeader("X-User-Id") UUID actorId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body(new MessageResponse("Нет доступа"));
        employeeService.dismiss(id, actorId, email, role);
        return ResponseEntity.ok(new MessageResponse("Сотрудник уволен"));
    }
}


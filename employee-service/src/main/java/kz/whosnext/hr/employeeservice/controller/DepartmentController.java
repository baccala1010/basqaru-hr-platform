package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateDepartmentRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateDepartmentRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.DepartmentResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentResponse> listAll() {
        return departmentService.listAll();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getById(@PathVariable UUID id) {
        return departmentService.getById(id);
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@RequestHeader("X-User-Id") UUID userId,
                                                     @RequestHeader("X-User-Email") String email,
                                                     @RequestHeader("X-User-Role") String role,
                                                     @Valid @RequestBody CreateDepartmentRequest req) {
        checkHrOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(req, userId, email, role));
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable UUID id,
                                     @RequestHeader("X-User-Id") UUID userId,
                                     @RequestHeader("X-User-Email") String email,
                                     @RequestHeader("X-User-Role") String role,
                                     @Valid @RequestBody UpdateDepartmentRequest req) {
        checkHrOrAdmin(role);
        return departmentService.update(id, req, userId, email, role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        checkHrOrAdmin(role);
        departmentService.delete(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Отдел удалён"));
    }

    private void checkHrOrAdmin(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для HR и ADMIN");
    }
}


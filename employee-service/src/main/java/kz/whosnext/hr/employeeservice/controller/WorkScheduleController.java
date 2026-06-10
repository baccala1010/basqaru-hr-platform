package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateWorkScheduleRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.WorkScheduleResponse;
import kz.whosnext.hr.employeeservice.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService workScheduleService;

    @GetMapping
    public List<WorkScheduleResponse> listAll() {
        return workScheduleService.listAll();
    }

    @PostMapping
    public ResponseEntity<WorkScheduleResponse> create(@RequestHeader("X-User-Id") UUID userId,
                                                       @RequestHeader("X-User-Email") String email,
                                                       @RequestHeader("X-User-Role") String role,
                                                       @Valid @RequestBody CreateWorkScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workScheduleService.create(req, userId, email, role));
    }

    @PutMapping("/{id}")
    public WorkScheduleResponse update(@PathVariable UUID id,
                                       @RequestHeader("X-User-Id") UUID userId,
                                       @RequestHeader("X-User-Email") String email,
                                       @RequestHeader("X-User-Role") String role,
                                       @Valid @RequestBody CreateWorkScheduleRequest req) {
        return workScheduleService.update(id, req, userId, email, role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        workScheduleService.delete(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("График удалён"));
    }
}


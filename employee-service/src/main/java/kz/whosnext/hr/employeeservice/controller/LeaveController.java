package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.model.dto.request.LeaveRequestDto;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateLeaveStatusRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.LeavePeriodSummaryResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.LeaveResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    public Page<LeaveResponse> list(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        checkHrOrAdminOrDirector(role);
        return leaveService.list(employeeId, status, startDate, endDate, pageable);
    }

    @GetMapping("/my")
    public Page<LeaveResponse> myLeaves(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return leaveService.getMyLeaves(userId, pageable);
    }

    @GetMapping("/balance")
    public Map<String, Object> balance(@RequestHeader("X-User-Id") UUID userId) {
        return leaveService.getBalance(userId);
    }

    @GetMapping("/summary")
    public LeavePeriodSummaryResponse getSummary(
            @RequestHeader("X-User-Role") String role,
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        checkAccountingOrHrOrAdminOrDirector(role);
        return leaveService.getLeaveSummary(employeeId, periodStart, periodEnd);
    }

    @GetMapping("/{id}")
    public LeaveResponse getById(@PathVariable UUID id) {
        return leaveService.getById(id);
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody LeaveRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.create(userId, email, role, req));
    }

    @PatchMapping("/{id}/status")
    public LeaveResponse updateStatus(@PathVariable UUID id,
                                      @RequestHeader("X-User-Id") UUID actorId,
                                      @RequestHeader("X-User-Email") String email,
                                      @RequestHeader("X-User-Role") String role,
                                      @Valid @RequestBody UpdateLeaveStatusRequest req) {
        checkHrOrAdmin(role);
        return leaveService.updateStatus(id, req, actorId, email, role);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MessageResponse> cancel(@PathVariable UUID id,
                                                  @RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        leaveService.cancel(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Отпуск отменён"));
    }

    private void checkHrOrAdmin(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Доступ только для HR и ADMIN");
    }

    private void checkHrOrAdminOrDirector(String role) {
        if (!"HR".equals(role) && !"ADMIN".equals(role) && !"DIRECTOR".equals(role))
            throw new AccessDeniedException("Доступ только для HR, ADMIN и DIRECTOR");
    }

    private void checkAccountingOrHrOrAdminOrDirector(String role) {
        if (!"ACCOUNTANT".equals(role) && !"HR".equals(role) && !"ADMIN".equals(role) && !"DIRECTOR".equals(role))
            throw new AccessDeniedException("Доступ только для ACCOUNTANT, HR, ADMIN и DIRECTOR");
    }
}


package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateHolidayRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.PayrollCalculationRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpsertPayrollProfileRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.HolidayCalendarResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.PayrollProfileResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.PayrollSummaryResponse;
import kz.whosnext.hr.employeeservice.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PutMapping("/profiles/{employeeId}")
    public PayrollProfileResponse upsertProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpsertPayrollProfileRequest request) {
        checkAccountingRole(role);
        return payrollService.upsertProfile(employeeId, request, userId, email, role);
    }

    @GetMapping("/profiles/{employeeId}")
    public PayrollProfileResponse getProfile(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID employeeId) {
        checkAccountingRole(role);
        return payrollService.getProfile(employeeId);
    }

    @PostMapping("/calculate")
    public PayrollSummaryResponse calculate(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody PayrollCalculationRequest request) {
        checkAccountingRole(role);
        return payrollService.calculate(request, userId, email, role);
    }

    @GetMapping("/summary")
    public PayrollSummaryResponse summary(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @RequestParam UUID employeeId,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd) {
        checkAccountingRole(role);
        return payrollService.calculate(new PayrollCalculationRequest(employeeId, periodStart, periodEnd, false),
                userId, email, role);
    }

    @PostMapping("/holidays")
    public ResponseEntity<HolidayCalendarResponse> createHoliday(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateHolidayRequest request) {
        checkAccountingRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createHoliday(request, userId, email, role));
    }

    @GetMapping("/holidays")
    public List<HolidayCalendarResponse> listHolidays(
            @RequestHeader("X-User-Role") String role,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        checkAccountingRole(role);
        return payrollService.listHolidays(from, to);
    }

    private void checkAccountingRole(String role) {
        if (!"ACCOUNTANT".equals(role) && !"ADMIN".equals(role) && !"DIRECTOR".equals(role)) {
            throw new AccessDeniedException("Доступ только для ACCOUNTANT, ADMIN и DIRECTOR");
        }
    }
}


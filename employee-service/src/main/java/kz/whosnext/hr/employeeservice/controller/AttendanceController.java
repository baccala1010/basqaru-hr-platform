package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.model.dto.request.IngestAttendanceEventRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.AttendanceIngestionResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.AttendancePeriodSummaryResponse;
import kz.whosnext.hr.employeeservice.model.dto.request.MarkAttendanceRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.AttendanceResponse;
import kz.whosnext.hr.employeeservice.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public Page<AttendanceResponse> list(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID employeeId,
            @PageableDefault(size = 20) Pageable pageable) {
        checkAccountingOrHrAccess(role);
        return attendanceService.list(employeeId, pageable);
    }

    @GetMapping("/by-date")
    public List<AttendanceResponse> getByDate(
            @RequestHeader("X-User-Role") String role,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        checkAccountingOrHrAccess(role);
        return attendanceService.getByDate(date);
    }

    @GetMapping("/summary")
    public AttendancePeriodSummaryResponse getSummary(
            @RequestHeader("X-User-Role") String role,
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        checkAccountingOrHrAccess(role);
        return attendanceService.getSummary(employeeId, periodStart, periodEnd);
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(
            @RequestParam UUID employeeId,
            @RequestParam(required = false) String location,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(attendanceService.checkIn(employeeId, location, userId, email, role));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(
            @RequestParam UUID employeeId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(attendanceService.checkOut(employeeId, userId, email, role));
    }

    @PostMapping("/ingestion/events")
    public ResponseEntity<AttendanceIngestionResponse> ingest(
            @RequestHeader("X-Attendance-Api-Key") String apiKey,
            @Valid @RequestBody IngestAttendanceEventRequest request) {
        return ResponseEntity.ok(attendanceService.ingestExternalEvent(request, apiKey));
    }

    @PostMapping("/mark")
    public ResponseEntity<AttendanceResponse> mark(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody MarkAttendanceRequest request) {
        checkAccountingOrHrAccess(role);
        return ResponseEntity.ok(attendanceService.mark(request, userId, email, role));
    }

    @DeleteMapping("/mark")
    public ResponseEntity<Void> deleteRecord(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        checkAccountingOrHrAccess(role);
        attendanceService.deleteRecord(employeeId, date, userId, email, role);
        return ResponseEntity.noContent().build();
    }

    private void checkAccountingOrHrAccess(String role) {
        if (!"ACCOUNTANT".equals(role) && !"HR".equals(role) && !"ADMIN".equals(role) && !"DIRECTOR".equals(role)) {
            throw new AccessDeniedException("Доступ только для ACCOUNTANT, HR, ADMIN и DIRECTOR");
        }
    }
}


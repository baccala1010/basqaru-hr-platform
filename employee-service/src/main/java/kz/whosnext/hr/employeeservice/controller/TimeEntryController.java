package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.model.dto.request.TimeEntryRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.TimeEntryResponse;
import kz.whosnext.hr.employeeservice.service.TimeEntryService;
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
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @GetMapping
    public Page<TimeEntryResponse> list(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @PageableDefault(size = 20) Pageable pageable) {
        return timeEntryService.list(employeeId, month, year, pageable);
    }

    @GetMapping("/my")
    public Page<TimeEntryResponse> myEntries(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return timeEntryService.listByUserId(userId, pageable);
    }

    @GetMapping("/period")
    public List<TimeEntryResponse> getByPeriod(
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return timeEntryService.getByEmployeeAndPeriod(employeeId, from, to);
    }

    @PostMapping
    public ResponseEntity<TimeEntryResponse> save(@RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role,
                                                  @Valid @RequestBody TimeEntryRequest req) {
        return ResponseEntity.ok(timeEntryService.save(req, userId, email, role));
    }
}


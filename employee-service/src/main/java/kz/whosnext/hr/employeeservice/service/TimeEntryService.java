package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.TimeEntryMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.TimeEntryRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.TimeEntryResponse;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.entity.TimeEntry;
import kz.whosnext.hr.employeeservice.model.enums.TimeEntryStatus;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import kz.whosnext.hr.employeeservice.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final TimeEntryMapper timeEntryMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public Page<TimeEntryResponse> list(UUID employeeId, Integer month, Integer year, Pageable pageable) {
        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();
            if (employeeId != null) {
                return timeEntryRepository.findByEmployeeIdAndDateBetween(employeeId, from, to, pageable)
                        .map(timeEntryMapper::toResponse);
            }
            return timeEntryRepository.findByDateBetween(from, to, pageable).map(timeEntryMapper::toResponse);
        }
        if (employeeId != null) {
            return timeEntryRepository.findByEmployeeId(employeeId, pageable).map(timeEntryMapper::toResponse);
        }
        return timeEntryRepository.findAll(pageable).map(timeEntryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TimeEntryResponse> listByUserId(UUID userId, Pageable pageable) {
        Employee employee = employeeRepository.findByUserId(userId).orElse(null);
        if (employee == null) {
            return Page.empty(pageable);
        }
        return timeEntryRepository.findByEmployeeId(employee.getId(), pageable).map(timeEntryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> getByEmployeeAndPeriod(UUID employeeId, LocalDate from, LocalDate to) {
        return timeEntryRepository.findByEmployeeIdAndDateBetween(employeeId, from, to).stream()
                .map(timeEntryMapper::toResponse)
                .toList();
    }

    @Transactional
    public TimeEntryResponse save(TimeEntryRequest req, UUID actorId, String actorEmail, String actorRole) {
        Employee employee = employeeService.findById(req.employeeId());
        TimeEntryStatus status = req.status() != null ? TimeEntryStatus.valueOf(req.status()) : TimeEntryStatus.PRESENT;

        BigDecimal hoursWorked = null;
        if (req.checkIn() != null && req.checkOut() != null) {
            long minutes = ChronoUnit.MINUTES.between(req.checkIn(), req.checkOut());
            hoursWorked = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        }

        TimeEntry entry = timeEntryRepository.findByEmployeeIdAndDate(req.employeeId(), req.date())
                .orElse(TimeEntry.builder().employee(employee).date(req.date()).build());

        entry.setCheckIn(req.checkIn());
        entry.setCheckOut(req.checkOut());
        entry.setHoursWorked(hoursWorked);
        entry.setStatus(status);
        entry.setNote(req.note());

        entry = timeEntryRepository.save(entry);
        log.info("Запись табеля сохранена: employeeId={}, date={}", req.employeeId(), req.date());

        activityLogProducer.log(
                ActivityAction.TIME_ENTRY_SAVED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "TimeEntry", entry.getId().toString(),
                "Time entry saved: employeeId=" + req.employeeId() + ", date=" + req.date());

        return timeEntryMapper.toResponse(entry);
    }

    public TimeEntry findById(UUID id) {
        return timeEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись табеля не найдена: " + id));
    }
}


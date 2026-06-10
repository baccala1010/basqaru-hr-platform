package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.mapper.AttendanceMapper;
import kz.whosnext.hr.employeeservice.event.AttendanceIngestedEvent;
import kz.whosnext.hr.employeeservice.exception.AccessDeniedException;
import kz.whosnext.hr.employeeservice.exception.BadRequestException;
import kz.whosnext.hr.employeeservice.model.dto.request.MarkAttendanceRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.IngestAttendanceEventRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.AttendanceIngestionResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.AttendancePeriodSummaryResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.AttendanceResponse;
import kz.whosnext.hr.employeeservice.model.entity.AttendanceIngestionEvent;
import kz.whosnext.hr.employeeservice.model.entity.AttendanceRecord;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceEventType;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceIngestionSource;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceIngestionStatus;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.AttendanceIngestionEventRepository;
import kz.whosnext.hr.employeeservice.repository.AttendanceRecordRepository;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceIngestionEventRepository attendanceIngestionEventRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final KafkaProducerService kafkaProducerService;
    private final AttendanceMapper attendanceMapper;
    private final ActivityLogProducer activityLogProducer;

    @Value("${app.attendance.ingestion.api-key}")
    private String attendanceIngestionApiKey;

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(UUID employeeId, Pageable pageable) {
        if (employeeId != null) {
            return attendanceRecordRepository.findByEmployeeId(employeeId, pageable).map(attendanceMapper::toResponse);
        }
        return attendanceRecordRepository.findAll(pageable).map(attendanceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getByDate(LocalDate date) {
        return attendanceRecordRepository.findByDate(date).stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendancePeriodSummaryResponse getSummary(UUID employeeId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        employeeService.findById(employeeId);

        long workingDays = countWeekDays(periodStart, periodEnd);
        long presentDays = attendanceRecordRepository
                .countByEmployeeIdAndDateBetweenAndPresentTrue(employeeId, periodStart, periodEnd);

        return new AttendancePeriodSummaryResponse(employeeId, periodStart, periodEnd, workingDays, presentDays);
    }

    @Transactional
    public AttendanceResponse checkIn(UUID employeeId, String location, UUID actorId, String actorEmail, String actorRole) {
        Employee employee = employeeService.findById(employeeId);
        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElse(AttendanceRecord.builder().employee(employee).date(today).build());

        record.setPresent(true);
        record.setCheckIn(LocalTime.now());
        record.setLocation(location);

        record = attendanceRecordRepository.save(record);
        log.info("Отметка прихода: employeeId={}", employeeId);

        activityLogProducer.log(
                ActivityAction.ATTENDANCE_CHECK_IN, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Attendance", record.getId().toString(),
                "Check-in: employeeId=" + employeeId);

        return attendanceMapper.toResponse(record);
    }

    @Transactional
    public AttendanceResponse checkOut(UUID employeeId, UUID actorId, String actorEmail, String actorRole) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException(
                        "Нет записи прихода на сегодня для сотрудника: " + employeeId));

        record.setCheckOut(LocalTime.now());
        record = attendanceRecordRepository.save(record);
        log.info("Отметка ухода: employeeId={}", employeeId);

        activityLogProducer.log(
                ActivityAction.ATTENDANCE_CHECK_OUT, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Attendance", record.getId().toString(),
                "Check-out: employeeId=" + employeeId);

        return attendanceMapper.toResponse(record);
    }

    @Transactional
    public AttendanceIngestionResponse ingestExternalEvent(IngestAttendanceEventRequest request, String apiKey) {
        validateIngestionApiKey(apiKey);

        if (request.employeeId() == null && (request.iin() == null || request.iin().isBlank())) {
            throw new BadRequestException("Нужно передать employeeId или iin");
        }

        AttendanceIngestionSource source = parseSource(request.source());
        AttendanceEventType eventType = parseEventType(request.eventType());

        AttendanceIngestionEvent duplicate = attendanceIngestionEventRepository
                .findBySourceAndExternalEventId(source, request.externalEventId())
                .orElse(null);
        if (duplicate != null) {
            return new AttendanceIngestionResponse(
                    duplicate.getId(),
                    null,
                    true,
                    duplicate.getIngestionStatus().name(),
                    "Событие уже обработано"
            );
        }

        Employee employee = resolveEmployee(request.employeeId(), request.iin());
        LocalDateTime occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDateTime.now();
        LocalDate day = occurredAt.toLocalDate();

        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndDate(employee.getId(), day)
                .orElse(AttendanceRecord.builder().employee(employee).date(day).build());

        record.setPresent(true);
        record.setLocation(request.location());
        if (eventType == AttendanceEventType.CHECK_IN) {
            record.setCheckIn(occurredAt.toLocalTime());
        } else {
            record.setCheckOut(occurredAt.toLocalTime());
            if (record.getCheckIn() == null) {
                record.setCheckIn(LocalTime.of(9, 0));
            }
        }
        record = attendanceRecordRepository.save(record);

        AttendanceIngestionEvent ingestionEvent = AttendanceIngestionEvent.builder()
                .employee(employee)
                .source(source)
                .externalEventId(request.externalEventId())
                .eventType(eventType)
                .location(request.location())
                .occurredAt(occurredAt)
                .ingestionStatus(AttendanceIngestionStatus.PROCESSED)
                .rawPayload(request.rawPayload())
                .build();
        ingestionEvent = attendanceIngestionEventRepository.save(ingestionEvent);

        kafkaProducerService.sendAttendanceIngested(new AttendanceIngestedEvent(
                ingestionEvent.getId(),
                employee.getId(),
                source.name(),
                eventType.name(),
                occurredAt,
                AttendanceIngestionStatus.PROCESSED.name()
        ));

        return new AttendanceIngestionResponse(
                ingestionEvent.getId(),
                record.getId(),
                false,
                AttendanceIngestionStatus.PROCESSED.name(),
                "Событие обработано"
        );
    }

    @Transactional
    public AttendanceResponse mark(MarkAttendanceRequest request, UUID actorId, String actorEmail, String actorRole) {
        Employee employee = employeeService.findById(request.employeeId());
        AttendanceRecord record = attendanceRecordRepository
                .findByEmployeeIdAndDate(request.employeeId(), request.date())
                .orElse(AttendanceRecord.builder()
                        .employee(employee)
                        .date(request.date())
                        .build());

        record.setPresent(request.present());
        if (request.present() && record.getCheckIn() == null) {
            record.setCheckIn(LocalTime.of(9, 0));
        }
        record = attendanceRecordRepository.save(record);
        log.info("Ручная отметка: employeeId={}, date={}, present={}",
                request.employeeId(), request.date(), request.present());

        activityLogProducer.log(
                ActivityAction.ATTENDANCE_MARKED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Attendance", record.getId().toString(),
                "Manual attendance mark: employeeId=" + request.employeeId() + ", date=" + request.date());

        return attendanceMapper.toResponse(record);
    }

    @Transactional
    public void deleteRecord(UUID employeeId, LocalDate date, UUID actorId, String actorEmail, String actorRole) {
        attendanceRecordRepository.findByEmployeeIdAndDate(employeeId, date)
                .ifPresent(record -> {
                    attendanceRecordRepository.delete(record);
                    log.info("Удалена запись посещаемости: employeeId={}, date={}", employeeId, date);

                    activityLogProducer.log(
                            ActivityAction.ATTENDANCE_RECORD_DELETED, ActivitySource.EMPLOYEE_SERVICE,
                            actorId, actorEmail, actorRole,
                            "Attendance", employeeId + "@" + date,
                            "Attendance record deleted: employeeId=" + employeeId + ", date=" + date);
                });
    }

    private Employee resolveEmployee(UUID employeeId, String iin) {
        if (employeeId != null) {
            return employeeService.findById(employeeId);
        }
        return employeeRepository.findByIin(iin)
                .orElseThrow(() -> new kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException(
                        "Сотрудник не найден по iin: " + iin));
    }

    private AttendanceIngestionSource parseSource(String source) {
        try {
            return AttendanceIngestionSource.valueOf(source.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Некорректный source. Ожидается TURNSTILE, FACE_ID или PASS_CARD");
        }
    }

    private AttendanceEventType parseEventType(String eventType) {
        try {
            return AttendanceEventType.valueOf(eventType.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Некорректный eventType. Ожидается CHECK_IN или CHECK_OUT");
        }
    }

    private void validateIngestionApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(attendanceIngestionApiKey)) {
            throw new AccessDeniedException("Неверный API key для attendance ingestion");
        }
    }

    private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new BadRequestException("periodStart и periodEnd обязательны");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new BadRequestException("periodStart не может быть позже periodEnd");
        }
    }

    private long countWeekDays(LocalDate periodStart, LocalDate periodEnd) {
        return periodStart.datesUntil(periodEnd.plus(1, ChronoUnit.DAYS))
                .filter(date -> date.getDayOfWeek().getValue() <= 5)
                .count();
    }
}


package kz.whosnext.hr.employeeservice.service;

import jakarta.persistence.criteria.Predicate;
import kz.whosnext.hr.employeeservice.exception.BadRequestException;
import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.LeaveMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.LeaveRequestDto;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateLeaveStatusRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.LeavePeriodSummaryResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.LeaveResponse;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.entity.Leave;
import kz.whosnext.hr.employeeservice.model.enums.LeaveStatus;
import kz.whosnext.hr.employeeservice.model.enums.LeaveType;
import kz.whosnext.hr.employeeservice.model.enums.NotificationType;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import kz.whosnext.hr.employeeservice.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final int ANNUAL_LEAVE_DAYS = 24;

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final LeaveMapper leaveMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public Page<LeaveResponse> list(UUID employeeId, String status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LeaveStatus leaveStatus = null;
        if (status != null) {
            try { leaveStatus = LeaveStatus.valueOf(status); } catch (Exception ignored) {}
        }
        final LeaveStatus finalStatus = leaveStatus;
        Specification<Leave> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (finalStatus != null) {
                predicates.add(cb.equal(root.get("status"), finalStatus));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return leaveRepository.findAll(spec, pageable).map(leaveMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse> getMyLeaves(UUID userId, Pageable pageable) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден по userId: " + userId));
        return leaveRepository.findByEmployeeId(employee.getId(), pageable).map(leaveMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBalance(UUID userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден по userId: " + userId));

        int usedDays = leaveRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.APPROVED)
                .stream()
                .filter(l -> l.getLeaveType() == LeaveType.ANNUAL)
                .mapToInt(l -> l.getDaysCount() != null ? l.getDaysCount() : 0)
                .sum();

        int totalDays = ANNUAL_LEAVE_DAYS;
        int availableDays = Math.max(0, totalDays - usedDays);

        Map<String, Object> balance = new LinkedHashMap<>();
        balance.put("employeeId", employee.getId());
        balance.put("totalDays", totalDays);
        balance.put("usedDays", usedDays);
        balance.put("availableDays", availableDays);
        return balance;
    }

    @Transactional(readOnly = true)
    public LeaveResponse getById(UUID id) {
        return leaveMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public LeavePeriodSummaryResponse getLeaveSummary(UUID employeeId, LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new BadRequestException("periodStart и periodEnd обязательны");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new BadRequestException("periodStart не может быть позже periodEnd");
        }
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден: " + employeeId));

        long annual = 0;
        long sick = 0;
        long unpaid = 0;
        long maternity = 0;
        long paternity = 0;
        long other = 0;

        for (Leave leave : leaveRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED)) {
            long overlapDays = overlapDays(leave.getStartDate(), leave.getEndDate(), periodStart, periodEnd);
            if (overlapDays <= 0) {
                continue;
            }
            switch (leave.getLeaveType()) {
                case ANNUAL -> annual += overlapDays;
                case SICK -> sick += overlapDays;
                case UNPAID -> unpaid += overlapDays;
                case MATERNITY -> maternity += overlapDays;
                case PATERNITY -> paternity += overlapDays;
                case OTHER -> other += overlapDays;
            }
        }

        return new LeavePeriodSummaryResponse(
                employeeId,
                periodStart,
                periodEnd,
                annual,
                sick,
                unpaid,
                maternity,
                paternity,
                other
        );
    }

    @Transactional
    public LeaveResponse create(UUID userId, String actorEmail, String actorRole, LeaveRequestDto req) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден по userId: " + userId));

        if (req.startDate().isAfter(req.endDate())) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }
        int days = (int) ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;

        Leave leave = Leave.builder()
                .employee(employee)
                .leaveType(LeaveType.valueOf(req.leaveType()))
                .startDate(req.startDate())
                .endDate(req.endDate())
                .daysCount(days)
                .reason(req.reason())
                .status(LeaveStatus.PENDING)
                .build();

        leave = leaveRepository.save(leave);
        log.info("Отпуск создан: id={}, employeeId={}", leave.getId(), employee.getId());

        activityLogProducer.log(
                ActivityAction.LEAVE_REQUESTED, ActivitySource.EMPLOYEE_SERVICE,
                userId, actorEmail, actorRole,
                "Leave", leave.getId().toString(),
                "Leave requested: " + leave.getLeaveType() + " " + leave.getStartDate() + " - " + leave.getEndDate());

        notifyHr(employee, leave);

        return leaveMapper.toResponse(leave);
    }

    @Transactional
    public LeaveResponse updateStatus(UUID id, UpdateLeaveStatusRequest req, UUID actorId, String actorEmail, String actorRole) {
        Leave leave = findById(id);
        LeaveStatus newStatus = LeaveStatus.valueOf(req.status());

        leave.setStatus(newStatus);
        if (newStatus == LeaveStatus.APPROVED) {
            leave.setApprovedBy(actorId);
            leave.setApprovedAt(LocalDateTime.now());
            notificationService.create(leave.getEmployee().getUserId(),
                    "Отпуск одобрен",
                    "Ваш запрос на отпуск с " + leave.getStartDate() + " по " + leave.getEndDate() + " одобрен",
                    NotificationType.LEAVE_APPROVED, leave.getId(), "LEAVE");
        } else if (newStatus == LeaveStatus.REJECTED) {
            leave.setRejectReason(req.rejectReason());
            notificationService.create(leave.getEmployee().getUserId(),
                    "Отпуск отклонён",
                    "Ваш запрос на отпуск отклонён. Причина: " + req.rejectReason(),
                    NotificationType.LEAVE_REJECTED, leave.getId(), "LEAVE");
        }

        leave = leaveRepository.save(leave);
        log.info("Статус отпуска изменён: id={}, status={}", id, newStatus);

        activityLogProducer.log(
                newStatus == LeaveStatus.APPROVED ? ActivityAction.LEAVE_APPROVED : ActivityAction.LEAVE_REJECTED,
                ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Leave", id.toString(),
                "Leave status updated: " + newStatus);

        return leaveMapper.toResponse(leave);
    }

    @Transactional
    public void cancel(UUID id, UUID userId, String actorEmail, String actorRole) {
        Leave leave = findById(id);
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден по userId: " + userId));

        if (!leave.getEmployee().getId().equals(employee.getId())) {
            throw new BadRequestException("Нельзя отменить чужой отпуск");
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Можно отменить только ожидающий отпуск");
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepository.save(leave);
        log.info("Отпуск отменён: id={}, userId={}", id, userId);

        activityLogProducer.log(
                ActivityAction.LEAVE_CANCELLED, ActivitySource.EMPLOYEE_SERVICE,
                userId, actorEmail, actorRole,
                "Leave", id.toString(),
                "Leave cancelled: " + leave.getLeaveType() + " " + leave.getStartDate() + " - " + leave.getEndDate());
    }

    private void notifyHr(Employee employee, Leave leave) {
        employeeRepository.findAll().stream()
                .filter(e -> {
                    String pos = e.getPositionName() != null ? e.getPositionName().toLowerCase() : "";
                    return pos.contains("hr") || pos.contains("admin") || pos.contains("director");
                })
                .forEach(emp -> notificationService.createByEmployeeId(emp.getId(),
                        "Новая заявка на отпуск",
                        employee.getLastName() + " " + employee.getFirstName() + " подал(а) заявку на отпуск: "
                                + leave.getLeaveType().name() + " с " + leave.getStartDate() + " по " + leave.getEndDate(),
                        NotificationType.LEAVE_REQUEST, leave.getId(), "LEAVE"));
    }

    public Leave findById(UUID id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Отпуск не найден: " + id));
    }

    private long overlapDays(LocalDate leaveStart, LocalDate leaveEnd, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate start = leaveStart.isAfter(periodStart) ? leaveStart : periodStart;
        LocalDate end = leaveEnd.isBefore(periodEnd) ? leaveEnd : periodEnd;
        if (start.isAfter(end)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end) + 1;
    }
}


package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.BadRequestException;
import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateHolidayRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.PayrollCalculationRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpsertPayrollProfileRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.HolidayCalendarResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.PayrollDayBreakdownResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.PayrollProfileResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.PayrollSummaryResponse;
import kz.whosnext.hr.employeeservice.model.entity.*;
import kz.whosnext.hr.employeeservice.model.enums.LeaveStatus;
import kz.whosnext.hr.employeeservice.model.enums.LeaveType;
import kz.whosnext.hr.employeeservice.model.enums.PayrollDayType;
import kz.whosnext.hr.employeeservice.model.enums.TimeEntryStatus;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EmployeeService employeeService;
    private final EmployeePayrollProfileRepository payrollProfileRepository;
    private final HolidayCalendarDayRepository holidayRepository;
    private final PayrollCalculationRepository payrollCalculationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final LeaveRepository leaveRepository;
    private final ActivityLogProducer activityLogProducer;

    @Value("${app.payroll.tax.opv-rate:0.10}")
    private BigDecimal opvRate;

    @Value("${app.payroll.tax.osms-rate:0.02}")
    private BigDecimal osmsRate;

    @Value("${app.payroll.tax.iit-rate:0.10}")
    private BigDecimal iitRate;

    @Value("${app.payroll.sick-leave-paid-rate:0.80}")
    private BigDecimal sickLeavePaidRate;

    @Transactional
    public PayrollProfileResponse upsertProfile(UUID employeeId, UpsertPayrollProfileRequest request,
                                                UUID actorId, String actorEmail, String actorRole) {
        Employee employee = employeeService.findById(employeeId);
        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeId(employeeId)
                .orElse(EmployeePayrollProfile.builder().employee(employee).build());

        profile.setMonthlySalary(request.monthlySalary());
        profile.setMonthlyBonus(request.monthlyBonus() == null ? BigDecimal.ZERO : request.monthlyBonus());
        profile.setEffectiveFrom(request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom());

        profile = payrollProfileRepository.save(profile);

        activityLogProducer.log(
                ActivityAction.PAYROLL_PROFILE_UPSERTED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "PayrollProfile", profile.getId().toString(),
                "Payroll profile upserted for employeeId=" + employeeId);

        return toProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public PayrollProfileResponse getProfile(UUID employeeId) {
        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll profile не найден для employeeId=" + employeeId));
        return toProfileResponse(profile);
    }

    @Transactional
    public HolidayCalendarResponse createHoliday(CreateHolidayRequest request, UUID actorId, String actorEmail, String actorRole) {
        HolidayCalendarDay holiday = holidayRepository.findByHolidayDate(request.holidayDate())
                .orElse(HolidayCalendarDay.builder().holidayDate(request.holidayDate()).build());
        holiday.setName(request.name());
        holiday.setIsPaid(request.isPaid() == null || request.isPaid());
        holiday = holidayRepository.save(holiday);

        activityLogProducer.log(
                ActivityAction.HOLIDAY_CREATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Holiday", holiday.getId().toString(),
                "Holiday created: " + request.name() + " on " + request.holidayDate());

        return toHolidayResponse(holiday);
    }

    @Transactional(readOnly = true)
    public List<HolidayCalendarResponse> listHolidays(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return holidayRepository.findByHolidayDateBetween(from, to).stream()
                .sorted(Comparator.comparing(HolidayCalendarDay::getHolidayDate))
                .map(this::toHolidayResponse)
                .toList();
    }

    @Transactional
    public PayrollSummaryResponse calculate(PayrollCalculationRequest request, UUID actorId, String actorEmail, String actorRole) {
        validatePeriod(request.periodStart(), request.periodEnd());

        Employee employee = employeeService.findById(request.employeeId());
        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new BadRequestException("Для сотрудника не задан payroll profile"));

        Map<LocalDate, AttendanceRecord> attendanceByDay = attendanceRecordRepository
                .findByEmployeeIdAndDateBetween(employee.getId(), request.periodStart(), request.periodEnd())
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getDate(), v), HashMap::putAll);

        Map<LocalDate, TimeEntry> timeEntryByDay = timeEntryRepository
                .findByEmployeeIdAndDateBetween(employee.getId(), request.periodStart(), request.periodEnd())
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getDate(), v), HashMap::putAll);

        Map<LocalDate, LeaveType> leaveByDay = buildLeaveCalendar(employee.getId(), request.periodStart(), request.periodEnd());

        Set<LocalDate> holidaySet = new HashSet<>(holidayRepository
                .findByHolidayDateBetween(request.periodStart(), request.periodEnd())
                .stream()
                .map(HolidayCalendarDay::getHolidayDate)
                .toList());

        int workingDays = 0;
        int workedDays = 0;
        int sickDays = 0;
        int unpaidDays = 0;
        int vacationDays = 0;
        int businessTripDays = 0;
        int weekendDays = 0;
        int holidayDays = 0;
        int absentDays = 0;
        List<PayrollDayBreakdownResponse> breakdown = new ArrayList<>();

        for (LocalDate day = request.periodStart(); !day.isAfter(request.periodEnd()); day = day.plusDays(1)) {
            PayrollDayType dayType;
            String note = null;

            boolean isHoliday = holidaySet.contains(day);
            boolean isWeekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (!isHoliday && !isWeekend) {
                workingDays++;
            }

            LeaveType leaveType = leaveByDay.get(day);
            TimeEntry timeEntry = timeEntryByDay.get(day);
            AttendanceRecord attendance = attendanceByDay.get(day);

            if (isHoliday) {
                dayType = PayrollDayType.HOLIDAY;
                holidayDays++;
            } else if (isWeekend) {
                dayType = PayrollDayType.WEEKEND;
                weekendDays++;
            } else if (leaveType != null) {
                dayType = mapLeaveType(leaveType);
                switch (dayType) {
                    case SICK_LEAVE -> sickDays++;
                    case UNPAID_LEAVE -> unpaidDays++;
                    case VACATION -> vacationDays++;
                    default -> {
                    }
                }
            } else if (timeEntry != null && timeEntry.getStatus() == TimeEntryStatus.BUSINESS_TRIP) {
                dayType = PayrollDayType.BUSINESS_TRIP;
                businessTripDays++;
            } else if (isWorkedDay(timeEntry, attendance)) {
                dayType = PayrollDayType.WORKED;
                workedDays++;
            } else {
                dayType = PayrollDayType.ABSENT;
                absentDays++;
                note = "Нет отметки посещаемости";
            }

            breakdown.add(new PayrollDayBreakdownResponse(day, dayType.name(), note));
        }

        BigDecimal monthlySalary = profile.getMonthlySalary();
        BigDecimal monthlyBonus = profile.getMonthlyBonus() == null ? BigDecimal.ZERO : profile.getMonthlyBonus();
        BigDecimal dailyRate = workingDays == 0
                ? BigDecimal.ZERO
                : monthlySalary.divide(BigDecimal.valueOf(workingDays), 6, RoundingMode.HALF_UP);

        BigDecimal paidDayEquivalent = BigDecimal.valueOf(workedDays + businessTripDays + vacationDays)
                .add(BigDecimal.valueOf(sickDays).multiply(sickLeavePaidRate));

        BigDecimal grossSalary = scale2(dailyRate.multiply(paidDayEquivalent).add(monthlyBonus));
        BigDecimal opv = scale2(grossSalary.multiply(opvRate));
        BigDecimal osms = scale2(grossSalary.multiply(osmsRate));
        BigDecimal iitBase = grossSalary.subtract(opv).max(BigDecimal.ZERO);
        BigDecimal iit = scale2(iitBase.multiply(iitRate));
        BigDecimal netSalary = scale2(grossSalary.subtract(opv).subtract(osms).subtract(iit));

        PayrollCalculation saved = null;
        if (request.saveSnapshot() == null || request.saveSnapshot()) {
            PayrollCalculation calculation = PayrollCalculation.builder()
                    .employee(employee)
                    .periodStart(request.periodStart())
                    .periodEnd(request.periodEnd())
                    .workingDays(workingDays)
                    .workedDays(workedDays)
                    .sickDays(sickDays)
                    .unpaidDays(unpaidDays)
                    .vacationDays(vacationDays)
                    .businessTripDays(businessTripDays)
                    .weekendDays(weekendDays)
                    .holidayDays(holidayDays)
                    .absentDays(absentDays)
                    .monthlySalary(monthlySalary)
                    .monthlyBonus(monthlyBonus)
                    .grossSalary(grossSalary)
                    .opvAmount(opv)
                    .osmsAmount(osms)
                    .iitAmount(iit)
                    .netSalary(netSalary)
                    .build();
            saved = payrollCalculationRepository.save(calculation);
        }

        activityLogProducer.log(
                ActivityAction.PAYROLL_CALCULATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "PayrollCalculation", employee.getId().toString(),
                "Payroll calculated for employeeId=" + request.employeeId()
                        + " period " + request.periodStart() + " - " + request.periodEnd());

        return new PayrollSummaryResponse(
                saved != null ? saved.getId() : null,
                employee.getId(),
                employee.getLastName() + " " + employee.getFirstName(),
                request.periodStart(),
                request.periodEnd(),
                workingDays,
                workedDays,
                sickDays,
                unpaidDays,
                vacationDays,
                businessTripDays,
                weekendDays,
                holidayDays,
                absentDays,
                monthlySalary,
                monthlyBonus,
                grossSalary,
                opv,
                osms,
                iit,
                netSalary,
                LocalDateTime.now(),
                breakdown
        );
    }

    private PayrollProfileResponse toProfileResponse(EmployeePayrollProfile profile) {
        return new PayrollProfileResponse(
                profile.getId(),
                profile.getEmployee().getId(),
                profile.getMonthlySalary(),
                profile.getMonthlyBonus(),
                profile.getEffectiveFrom(),
                profile.getUpdatedAt()
        );
    }

    private HolidayCalendarResponse toHolidayResponse(HolidayCalendarDay holiday) {
        return new HolidayCalendarResponse(
                holiday.getId(),
                holiday.getHolidayDate(),
                holiday.getName(),
                Boolean.TRUE.equals(holiday.getIsPaid())
        );
    }

    private Map<LocalDate, LeaveType> buildLeaveCalendar(UUID employeeId, LocalDate periodStart, LocalDate periodEnd) {
        Map<LocalDate, LeaveType> leaveCalendar = new HashMap<>();
        for (Leave leave : leaveRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED)) {
            if (leave.getEndDate().isBefore(periodStart) || leave.getStartDate().isAfter(periodEnd)) {
                continue;
            }
            LocalDate start = leave.getStartDate().isBefore(periodStart) ? periodStart : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(periodEnd) ? periodEnd : leave.getEndDate();
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                leaveCalendar.put(day, leave.getLeaveType());
            }
        }
        return leaveCalendar;
    }

    private PayrollDayType mapLeaveType(LeaveType leaveType) {
        return switch (leaveType) {
            case SICK -> PayrollDayType.SICK_LEAVE;
            case UNPAID -> PayrollDayType.UNPAID_LEAVE;
            case ANNUAL -> PayrollDayType.VACATION;
            default -> PayrollDayType.OTHER_LEAVE;
        };
    }

    private boolean isWorkedDay(TimeEntry timeEntry, AttendanceRecord attendanceRecord) {
        if (timeEntry != null) {
            return timeEntry.getStatus() == TimeEntryStatus.PRESENT || timeEntry.getStatus() == TimeEntryStatus.LATE;
        }
        return attendanceRecord != null && Boolean.TRUE.equals(attendanceRecord.getPresent());
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("periodStart и periodEnd обязательны");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("periodStart не может быть позже periodEnd");
        }
    }

    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}


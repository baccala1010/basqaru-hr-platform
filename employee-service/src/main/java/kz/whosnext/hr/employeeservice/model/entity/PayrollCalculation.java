package kz.whosnext.hr.employeeservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_calculations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "working_days", nullable = false)
    private Integer workingDays;

    @Column(name = "worked_days", nullable = false)
    private Integer workedDays;

    @Column(name = "sick_days", nullable = false)
    private Integer sickDays;

    @Column(name = "unpaid_days", nullable = false)
    private Integer unpaidDays;

    @Column(name = "vacation_days", nullable = false)
    private Integer vacationDays;

    @Column(name = "business_trip_days", nullable = false)
    private Integer businessTripDays;

    @Column(name = "weekend_days", nullable = false)
    private Integer weekendDays;

    @Column(name = "holiday_days", nullable = false)
    private Integer holidayDays;

    @Column(name = "absent_days", nullable = false)
    private Integer absentDays;

    @Column(name = "monthly_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "monthly_bonus", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyBonus;

    @Column(name = "gross_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "opv_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal opvAmount;

    @Column(name = "osms_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal osmsAmount;

    @Column(name = "iit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal iitAmount;

    @Column(name = "net_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal netSalary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}


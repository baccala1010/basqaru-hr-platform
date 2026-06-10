package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.model.dto.response.AnalyticsResponse;
import kz.whosnext.hr.employeeservice.model.enums.EmployeeStatus;
import kz.whosnext.hr.employeeservice.repository.AttendanceRecordRepository;
import kz.whosnext.hr.employeeservice.repository.DepartmentRepository;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getOverview() {
        long total = employeeRepository.count();
        long active = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long onLeave = employeeRepository.countByStatus(EmployeeStatus.ON_LEAVE);
        long dismissed = employeeRepository.countByStatus(EmployeeStatus.DISMISSED);
        long presentToday = attendanceRecordRepository.countByDateAndPresentTrue(LocalDate.now());
        long absentToday = active - presentToday;

        Map<String, Long> byDepartment = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(
                        d -> d.getName(),
                        d -> employeeRepository.countByDepartmentId(d.getId()),
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put(EmployeeStatus.PROBATION.name(), employeeRepository.countByStatus(EmployeeStatus.PROBATION));
        byStatus.put(EmployeeStatus.ACTIVE.name(), active);
        byStatus.put(EmployeeStatus.ON_LEAVE.name(), onLeave);
        byStatus.put(EmployeeStatus.DISMISSED.name(), dismissed);

        return new AnalyticsResponse(total, active, onLeave, dismissed,
                presentToday, Math.max(0, absentToday), byDepartment, byStatus, List.of());
    }
}


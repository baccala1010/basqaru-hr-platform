package kz.whosnext.hr.employeeservice.model.dto.response;

import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        long totalEmployees,
        long activeEmployees,
        long onLeaveEmployees,
        long dismissedEmployees,
        long presentToday,
        long absentToday,
        Map<String, Long> byDepartment,
        Map<String, Long> byStatus,
        List<MonthlyAttendance> monthlyAttendance
) {
    public record MonthlyAttendance(
            String month,
            long presentDays,
            long absentDays
    ) {}
}


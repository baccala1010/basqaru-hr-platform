package kz.whosnext.hr.documentservice.model.dto.request;

import java.util.List;

public record AttendanceReportRequest(
        String month,
        String year,
        String departmentName,
        List<AttendanceRow> rows
) {
    public record AttendanceRow(
            String employeeName,
            String department,
            List<Boolean> dailyPresence,
            int totalDays,
            double attendancePercent
    ) {}
}


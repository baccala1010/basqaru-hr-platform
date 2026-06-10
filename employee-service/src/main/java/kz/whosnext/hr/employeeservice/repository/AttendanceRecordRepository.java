package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);

    Page<AttendanceRecord> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<AttendanceRecord> findByDate(LocalDate date);

    List<AttendanceRecord> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);

    long countByDateAndPresentTrue(LocalDate date);

    long countByEmployeeIdAndDateBetweenAndPresentTrue(UUID employeeId, LocalDate startDate, LocalDate endDate);
}


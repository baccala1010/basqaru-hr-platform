package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.TimeEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

    Page<TimeEntry> findByEmployeeId(UUID employeeId, Pageable pageable);

    Optional<TimeEntry> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);

    List<TimeEntry> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);

    Page<TimeEntry> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to, Pageable pageable);

    Page<TimeEntry> findByDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    @Query("SELECT COUNT(t) FROM TimeEntry t WHERE t.date = :date AND t.status = 'PRESENT'")
    long countPresentByDate(@Param("date") LocalDate date);
}


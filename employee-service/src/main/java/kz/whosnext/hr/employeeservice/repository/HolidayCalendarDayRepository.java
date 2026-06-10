package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.HolidayCalendarDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HolidayCalendarDayRepository extends JpaRepository<HolidayCalendarDay, UUID> {

    Optional<HolidayCalendarDay> findByHolidayDate(LocalDate holidayDate);

    List<HolidayCalendarDay> findByHolidayDateBetween(LocalDate from, LocalDate to);
}


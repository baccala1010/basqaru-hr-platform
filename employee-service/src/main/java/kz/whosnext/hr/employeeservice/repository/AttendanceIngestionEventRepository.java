package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.AttendanceIngestionEvent;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceIngestionSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceIngestionEventRepository extends JpaRepository<AttendanceIngestionEvent, UUID> {

    Optional<AttendanceIngestionEvent> findBySourceAndExternalEventId(AttendanceIngestionSource source, String externalEventId);
}


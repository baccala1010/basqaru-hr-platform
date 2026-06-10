package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.EmployeeNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EmployeeNotificationRepository extends JpaRepository<EmployeeNotification, UUID> {

    Page<EmployeeNotification> findByEmployeeId(UUID employeeId, Pageable pageable);

    Page<EmployeeNotification> findByEmployeeIdAndReadFalse(UUID employeeId, Pageable pageable);

    java.util.Optional<EmployeeNotification> findByIdAndEmployeeId(UUID id, UUID employeeId);

    long countByEmployeeIdAndReadFalse(UUID employeeId);

    @Modifying
    @Query("UPDATE EmployeeNotification n SET n.read = true WHERE n.employeeId = :employeeId AND n.read = false")
    void markAllReadByEmployeeId(@Param("employeeId") UUID employeeId);
}


package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.Leave;
import kz.whosnext.hr.employeeservice.model.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface LeaveRepository extends JpaRepository<Leave, UUID>, JpaSpecificationExecutor<Leave> {

    Page<Leave> findByEmployeeId(UUID employeeId, Pageable pageable);

    Page<Leave> findByStatus(LeaveStatus status, Pageable pageable);

    List<Leave> findByEmployeeIdAndStatus(UUID employeeId, LeaveStatus status);

    long countByEmployeeIdAndStatus(UUID employeeId, LeaveStatus status);
}

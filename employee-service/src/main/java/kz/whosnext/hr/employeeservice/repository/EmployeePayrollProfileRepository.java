package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.EmployeePayrollProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeePayrollProfileRepository extends JpaRepository<EmployeePayrollProfile, UUID> {

    Optional<EmployeePayrollProfile> findByEmployeeId(UUID employeeId);
}


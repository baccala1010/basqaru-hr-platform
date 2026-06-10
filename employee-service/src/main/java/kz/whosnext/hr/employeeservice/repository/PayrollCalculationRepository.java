package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.PayrollCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayrollCalculationRepository extends JpaRepository<PayrollCalculation, UUID> {

    Page<PayrollCalculation> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId, Pageable pageable);
}


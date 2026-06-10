package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.CertificateRequest;
import kz.whosnext.hr.employeeservice.model.enums.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, UUID> {

    Page<CertificateRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

    Page<CertificateRequest> findByStatus(CertificateStatus status, Pageable pageable);
}


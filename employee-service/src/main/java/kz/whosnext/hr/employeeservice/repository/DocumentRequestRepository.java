package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.DocumentRequest;
import kz.whosnext.hr.employeeservice.model.enums.DocumentRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, UUID> {

    Page<DocumentRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

    Page<DocumentRequest> findByStatus(DocumentRequestStatus status, Pageable pageable);
}


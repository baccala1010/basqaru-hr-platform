package kz.whosnext.hr.auth.repository;

import kz.whosnext.hr.auth.model.entity.LoginAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, UUID> {

    Page<LoginAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<LoginAuditLog> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);
}


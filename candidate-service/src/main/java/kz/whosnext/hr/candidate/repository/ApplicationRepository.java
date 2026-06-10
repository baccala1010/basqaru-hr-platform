package kz.whosnext.hr.candidate.repository;

import kz.whosnext.hr.candidate.model.entity.Application;
import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Page<Application> findByCandidateUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);
    Page<Application> findAllByOrderByCreatedAtDesc(Pageable pageable);
    void deleteAllByCandidate_Id(UUID candidateId);
}


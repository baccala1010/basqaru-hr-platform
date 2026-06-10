package kz.whosnext.hr.documentservice.repository;

import kz.whosnext.hr.documentservice.model.entity.Document;
import kz.whosnext.hr.documentservice.model.enums.DocumentStatus;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByApplicationId(UUID applicationId);

    List<Document> findByCandidateId(UUID candidateId);

    Page<Document> findByApplicationId(UUID applicationId, Pageable pageable);

    Page<Document> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<Document> findByDocumentType(DocumentType documentType, Pageable pageable);

    List<Document> findByApplicationIdAndDocumentTypeIn(UUID applicationId, List<DocumentType> types);

    Page<Document> findByUploadedBy(UUID uploadedBy, Pageable pageable);

    long countByApplicationIdAndStatusNot(UUID applicationId, DocumentStatus status);

    long countByApplicationIdAndStatus(UUID applicationId, DocumentStatus status);
}


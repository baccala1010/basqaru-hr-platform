package kz.whosnext.hr.documentservice.repository;

import kz.whosnext.hr.documentservice.model.entity.DocumentTemplate;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {

    List<DocumentTemplate> findByActiveTrue();

    Optional<DocumentTemplate> findByDocumentTypeAndActiveTrue(DocumentType documentType);

    List<DocumentTemplate> findByDocumentType(DocumentType documentType);

    boolean existsByDocumentTypeAndActiveTrue(DocumentType documentType);
}


package kz.whosnext.hr.candidate.repository;

import kz.whosnext.hr.candidate.model.entity.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContractTypeRepository extends JpaRepository<ContractType, UUID> {
    boolean existsByName(String name);
}


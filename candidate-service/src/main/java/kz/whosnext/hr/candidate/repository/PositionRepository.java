package kz.whosnext.hr.candidate.repository;

import kz.whosnext.hr.candidate.model.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    boolean existsByName(String name);
}


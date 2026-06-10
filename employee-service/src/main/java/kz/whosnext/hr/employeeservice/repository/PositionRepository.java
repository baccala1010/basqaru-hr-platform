package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    boolean existsByName(String name);
}


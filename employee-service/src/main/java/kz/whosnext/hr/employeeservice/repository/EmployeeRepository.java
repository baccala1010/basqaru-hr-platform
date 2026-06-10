package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByUserId(UUID userId);

    Optional<Employee> findByIin(String iin);

    boolean existsByUserId(UUID userId);

    List<Employee> findByDepartmentId(UUID departmentId);

    Page<Employee> findByDepartmentId(UUID departmentId, Pageable pageable);

    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    long countByStatus(EmployeeStatus status);

    long countByDepartmentId(UUID departmentId);

    @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(e.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Employee> search(@Param("q") String query, Pageable pageable);
}


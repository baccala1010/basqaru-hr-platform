package kz.whosnext.hr.employeeservice.repository;

import kz.whosnext.hr.employeeservice.model.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, UUID> {
    Page<News> findByPublishedTrue(Pageable pageable);
}


package kz.whosnext.hr.auth.service;

import jakarta.persistence.criteria.Predicate;
import kz.whosnext.hr.auth.mapper.ActivityLogMapper;
import kz.whosnext.hr.auth.model.dto.request.ActivityLogFilter;
import kz.whosnext.hr.auth.model.dto.response.ActivityLogResponse;
import kz.whosnext.hr.auth.model.entity.ActivityLog;
import kz.whosnext.hr.auth.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogQueryService {

    private final ActivityLogRepository repository;

    public Page<ActivityLogResponse> getLogs(ActivityLogFilter filter, Pageable pageable) {
        Specification<ActivityLog> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filter.actorId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("actorId"), filter.actorId()));
            }
            if (filter.action() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("action"), filter.action()));
            }
            if (filter.entityType() != null && !filter.entityType().isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("entityType"), filter.entityType()));
            }
            if (filter.source() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("source"), filter.source()));
            }
            if (filter.dateFrom() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), filter.dateFrom()));
            }
            if (filter.dateTo() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), filter.dateTo()));
            }

            return predicate;
        };

        return repository.findAll(spec, pageable).map(ActivityLogMapper::toResponse);
    }

    public ActivityLogResponse getById(UUID id) {
        return repository.findById(id)
                .map(ActivityLogMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Activity log not found: " + id));
    }
}

package kz.whosnext.hr.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.whosnext.hr.auth.model.dto.request.ActivityLogFilter;
import kz.whosnext.hr.auth.model.dto.response.ActivityLogResponse;
import kz.whosnext.hr.auth.model.enums.ActivityAction;
import kz.whosnext.hr.auth.model.enums.ActivitySource;
import kz.whosnext.hr.auth.service.ActivityLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "Activity log viewing (ADMIN only)")
public class ActivityLogController {

    private final ActivityLogQueryService queryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get activity logs with filtering and pagination")
    public ResponseEntity<Page<ActivityLogResponse>> getLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) ActivityAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) ActivitySource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        ActivityLogFilter filter = new ActivityLogFilter(actorId, action, entityType, source, dateFrom, dateTo);
        return ResponseEntity.ok(queryService.getLogs(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get activity log details by ID")
    public ResponseEntity<ActivityLogResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getById(id));
    }
}

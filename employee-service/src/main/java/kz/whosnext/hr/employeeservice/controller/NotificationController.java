package kz.whosnext.hr.employeeservice.controller;

import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.NotificationResponse;
import kz.whosnext.hr.employeeservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> list(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(userId, unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markRead(@PathVariable UUID id,
                                                    @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markRead(id, userId);
        return ResponseEntity.ok(new MessageResponse("Уведомление отмечено прочитанным"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllRead(@RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(new MessageResponse("Все уведомления прочитаны"));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<MessageResponse> markAllReadLegacy(@RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(new MessageResponse("Все уведомления прочитаны"));
    }
}


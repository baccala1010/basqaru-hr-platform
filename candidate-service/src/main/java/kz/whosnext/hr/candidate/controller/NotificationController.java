package kz.whosnext.hr.candidate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.whosnext.hr.candidate.model.dto.response.MessageResponse;
import kz.whosnext.hr.candidate.model.dto.response.NotificationResponse;
import kz.whosnext.hr.candidate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/v1/candidate-notifications"})
@RequiredArgsConstructor
@Tag(name = "Уведомления")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Уведомления текущего пользователя")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(userId, unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Счётчик непрочитанных")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Отметить прочитанным")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable UUID id,
                                                        @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(new MessageResponse("Отмечено как прочитанное"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Отметить все прочитанными")
    public ResponseEntity<MessageResponse> markAllAsRead(@RequestHeader("X-User-Id") UUID userId) {
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new MessageResponse("Отмечено: " + count));
    }
}


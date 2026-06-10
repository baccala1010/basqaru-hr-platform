package kz.whosnext.hr.candidate.service;

import kz.whosnext.hr.candidate.mapper.NotificationMapper;
import kz.whosnext.hr.candidate.model.dto.response.NotificationResponse;
import kz.whosnext.hr.candidate.model.entity.Notification;
import kz.whosnext.hr.candidate.model.enums.NotificationType;
import kz.whosnext.hr.candidate.exception.ResourceNotFoundException;
import kz.whosnext.hr.candidate.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public void create(UUID userId, String title, String message, NotificationType type, UUID refId, String refType) {
        notificationRepository.save(Notification.builder()
                .userId(userId).title(title).message(message)
                .type(type).referenceId(refId).referenceType(refType).build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(notificationMapper::toResponse);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID id, UUID userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Уведомление не найдено"));
        if (!n.getUserId().equals(userId)) throw new ResourceNotFoundException("Уведомление не найдено");
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId);
    }
}


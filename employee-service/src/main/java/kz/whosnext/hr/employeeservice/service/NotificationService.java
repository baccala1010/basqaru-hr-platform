package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.mapper.NotificationMapper;
import kz.whosnext.hr.employeeservice.model.dto.response.NotificationResponse;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.entity.EmployeeNotification;
import kz.whosnext.hr.employeeservice.model.enums.NotificationType;
import kz.whosnext.hr.employeeservice.repository.EmployeeNotificationRepository;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import kz.whosnext.hr.employeeservice.ws.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmployeeNotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, boolean unreadOnly, Pageable pageable) {
        return employeeRepository.findByUserId(userId).map(employee -> {
            if (unreadOnly) {
                return notificationRepository.findByEmployeeIdAndReadFalse(employee.getId(), pageable)
                        .map(notificationMapper::toResponse);
            }
            return notificationRepository.findByEmployeeId(employee.getId(), pageable)
                    .map(notificationMapper::toResponse);
        }).orElse(Page.empty(pageable));
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return employeeRepository.findByUserId(userId)
                .map(e -> notificationRepository.countByEmployeeIdAndReadFalse(e.getId()))
                .orElse(0L);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        employeeRepository.findByUserId(userId)
                .ifPresent(e -> notificationRepository.markAllReadByEmployeeId(e.getId()));
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        employeeRepository.findByUserId(userId).ifPresent(employee ->
                notificationRepository.findByIdAndEmployeeId(notificationId, employee.getId())
                        .ifPresent(notification -> {
                            notification.setRead(true);
                            notificationRepository.save(notification);
                        })
        );
    }

    @Transactional
    public void create(UUID userId, String title, String message, NotificationType type,
                       UUID referenceId, String referenceType) {
        employeeRepository.findByUserId(userId).ifPresent(employee ->
                saveAndNotify(EmployeeNotification.builder()
                        .employeeId(employee.getId())
                        .title(title)
                        .message(message)
                        .type(type)
                        .referenceId(referenceId)
                        .referenceType(referenceType)
                        .build())
        );
    }

    @Transactional
    public void createByEmployeeId(UUID employeeId, String title, String message, NotificationType type,
                                   UUID referenceId, String referenceType) {
        saveAndNotify(EmployeeNotification.builder()
                .employeeId(employeeId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());
    }

    private Employee findEmployeeByUserId(UUID userId) {
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден для userId: " + userId));
    }

    private void saveAndNotify(EmployeeNotification notification) {
        EmployeeNotification saved = notificationRepository.save(notification);
        notificationWebSocketHandler.broadcast(
                "{\"type\":\"NOTIFICATION\",\"employeeId\":\"" + saved.getEmployeeId() + "\",\"count\":1}"
        );
    }
}


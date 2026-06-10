package kz.whosnext.hr.auth.service;

import kz.whosnext.hr.auth.exception.BadRequestException;
import kz.whosnext.hr.auth.exception.ResourceNotFoundException;
import kz.whosnext.hr.auth.mapper.UserMapper;
import kz.whosnext.hr.auth.model.enums.ActivityAction;
import kz.whosnext.hr.auth.model.enums.ActivitySource;
import kz.whosnext.hr.auth.model.dto.request.ChangePasswordRequest;
import kz.whosnext.hr.auth.model.dto.request.ChangeRoleRequest;
import kz.whosnext.hr.auth.model.dto.request.UpdateProfileRequest;
import kz.whosnext.hr.auth.model.dto.response.MessageResponse;
import kz.whosnext.hr.auth.model.dto.response.UserResponse;
import kz.whosnext.hr.auth.model.entity.User;
import kz.whosnext.hr.auth.model.enums.AuditAction;
import kz.whosnext.hr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AvatarMinioService avatarMinioService;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = findUserOrThrow(userId);

        if (req.firstName() != null) user.setFirstName(req.firstName().trim());
        if (req.lastName() != null) user.setLastName(req.lastName().trim());
        if (req.phone() != null) user.setPhone(req.phone().trim());
        if (req.photoUrl() != null) user.setPhotoUrl(req.photoUrl().trim());

        user = userRepository.save(user);
        auditService.log(userId, user.getEmail(), AuditAction.PROFILE_UPDATED, true, null);

        activityLogProducer.log(
                ActivityAction.USER_PROFILE_UPDATED, ActivitySource.AUTH_SERVICE,
                userId, user.getEmail(), user.getRole().name(),
                "User", userId.toString(),
                "Profile updated");

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = findUserOrThrow(userId);
        avatarMinioService.deleteOldAvatar(user.getPhotoUrl());
        // Загружаем новый
        String presignedUrl = avatarMinioService.uploadAvatar(userId, file);
        user.setPhotoUrl(presignedUrl);
        user = userRepository.save(user);
        auditService.log(userId, user.getEmail(), AuditAction.PROFILE_UPDATED, true, "Фото профиля обновлено");

        activityLogProducer.log(
                ActivityAction.USER_PHOTO_UPLOADED, ActivitySource.AUTH_SERVICE,
                userId, user.getEmail(), user.getRole().name(),
                "User", userId.toString(),
                "Profile photo uploaded");

        log.info("Фото профиля обновлено: userId={}", userId);
        return userMapper.toResponse(user);
    }

    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest req) {
        User user = findUserOrThrow(userId);

        if (user.getPasswordHash() == null) {
            throw new BadRequestException("Пароль не установлен (OAuth2 аккаунт). Используйте сброс пароля");
        }
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный текущий пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        emailService.sendPasswordChangedNotification(user.getEmail());
        auditService.log(userId, user.getEmail(), AuditAction.PASSWORD_CHANGE, true, null);

        activityLogProducer.log(
                ActivityAction.USER_PASSWORD_CHANGED, ActivitySource.AUTH_SERVICE,
                userId, user.getEmail(), user.getRole().name(),
                "User", userId.toString(),
                "Password changed");

        return new MessageResponse("Пароль успешно изменён");
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Transactional
    public MessageResponse deleteUser(UUID id, UUID adminId) {
        User user = findUserOrThrow(id);
        String email = user.getEmail();
        userRepository.delete(user);

        emailService.sendAccountDeletedNotification(email);
        auditService.log(adminId, email, AuditAction.ACCOUNT_DELETED, true,
                "Удалён администратором (adminId=" + adminId + ")");

        User admin = findUserOrThrow(adminId);
        activityLogProducer.log(
                ActivityAction.USER_DELETED, ActivitySource.AUTH_SERVICE,
                adminId, admin.getEmail(), admin.getRole().name(),
                "User", id.toString(),
                "User deleted by admin " + adminId + ": email=" + email);

        log.info("Пользователь удалён: id={}, email={}, admin={}", id, email, adminId);
        return new MessageResponse("Пользователь удалён");
    }

    @Transactional
    public UserResponse changeRole(UUID id, ChangeRoleRequest req, UUID adminId) {
        User user = findUserOrThrow(id);
        var oldRole = user.getRole();
        user.setRole(req.role());
        user = userRepository.save(user);

        auditService.log(adminId, user.getEmail(), AuditAction.ROLE_CHANGED, true,
                "Роль: " + oldRole + " → " + req.role() + " (adminId=" + adminId + ")");

        User admin = findUserOrThrow(adminId);
        activityLogProducer.log(
                ActivityAction.USER_ROLE_CHANGED, ActivitySource.AUTH_SERVICE,
                adminId, admin.getEmail(), admin.getRole().name(),
                "User", id.toString(),
                "Role changed: " + oldRole + " → " + req.role());

        log.info("Роль изменена: userId={}, {} → {}", id, oldRole, req.role());
        return userMapper.toResponse(user);
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + id + " не найден"));
    }
}


package kz.whosnext.hr.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.whosnext.hr.auth.model.dto.request.ChangePasswordRequest;
import kz.whosnext.hr.auth.model.dto.request.ChangeRoleRequest;
import kz.whosnext.hr.auth.model.dto.request.UpdateProfileRequest;
import kz.whosnext.hr.auth.model.dto.response.MessageResponse;
import kz.whosnext.hr.auth.model.dto.response.UserResponse;
import kz.whosnext.hr.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Профиль, управление пользователями (ADMIN)")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Профиль текущего пользователя", description = "Возвращает данные из JWT")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль", description = "Можно менять: firstName, lastName, phone")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                       Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить фото профиля",
               description = "Принимает изображение (JPEG/PNG/WebP/GIF, max 5 МБ). Возвращает обновлённый профиль с полем photoUrl.")
    public ResponseEntity<UserResponse> uploadPhoto(@RequestParam("file") MultipartFile file,
                                                     Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(userService.uploadAvatar(userId, file));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Сменить пароль", description = "Требует текущий и новый пароль")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                           Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(userService.changePassword(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'DIRECTOR', 'ACCOUNTANT', 'EMPLOYEE')")
    @Operation(summary = "Список пользователей", description = "Пагинация + поиск")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'DIRECTOR', 'ACCOUNTANT', 'EMPLOYEE')")
    @Operation(summary = "Детали пользователя")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Удалить пользователя", description = "Уведомление на email")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID id,
                                                       Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(userService.deleteUser(id, adminId));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Сменить роль пользователя")
    public ResponseEntity<UserResponse> changeRole(@PathVariable UUID id,
                                                    @Valid @RequestBody ChangeRoleRequest request,
                                                    Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(userService.changeRole(id, request, adminId));
    }
}


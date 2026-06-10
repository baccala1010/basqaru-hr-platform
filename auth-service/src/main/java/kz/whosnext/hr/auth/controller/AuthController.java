package kz.whosnext.hr.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kz.whosnext.hr.auth.model.dto.request.*;
import kz.whosnext.hr.auth.model.dto.response.AuthResponse;
import kz.whosnext.hr.auth.model.dto.response.MessageResponse;
import kz.whosnext.hr.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, вход, токены, сброс пароля")
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя",
            description = "Создаёт пользователя с ролью CANDIDATE, отправляет email для подтверждения")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, httpRequest));
    }

    @GetMapping("/confirm")
    @Operation(summary = "Подтверждение email",
            description = "Подтверждает email по токену из письма, редиректит на фронтенд")
    public void confirmEmail(@RequestParam String token, HttpServletResponse response) throws java.io.IOException {
        try {
            authService.confirmEmail(token);
            response.sendRedirect(frontendUrl + "/login?confirmed=true");
        } catch (Exception e) {
            response.sendRedirect(frontendUrl + "/login?confirmed=false");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Авторизация",
            description = "Проверяет email/пароль, возвращает access + refresh токены")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токенов",
            description = "Принимает refresh token, возвращает новую пару токенов")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы",
            description = "Инвалидирует access и refresh токены")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        return ResponseEntity.ok(authService.logout(accessToken, request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Запрос на сброс пароля",
            description = "Отправляет на email ссылку для сброса пароля")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Сброс пароля",
            description = "Устанавливает новый пароль по токену из письма")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}


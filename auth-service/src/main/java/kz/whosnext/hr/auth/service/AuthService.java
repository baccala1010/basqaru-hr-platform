package kz.whosnext.hr.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.whosnext.hr.auth.event.UserRegisteredEvent;
import kz.whosnext.hr.auth.exception.*;
import kz.whosnext.hr.auth.model.dto.request.*;
import kz.whosnext.hr.auth.model.dto.response.AuthResponse;
import kz.whosnext.hr.auth.model.dto.response.MessageResponse;
import kz.whosnext.hr.auth.model.entity.User;
import kz.whosnext.hr.auth.model.enums.ActivityAction;
import kz.whosnext.hr.auth.model.enums.ActivitySource;
import kz.whosnext.hr.auth.model.enums.AuditAction;
import kz.whosnext.hr.auth.model.enums.OAuthProvider;
import kz.whosnext.hr.auth.model.enums.Role;
import kz.whosnext.hr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final EmailService emailService;
    private final KafkaProducerService kafkaProducer;
    private final AuditService auditService;
    private final ActivityLogProducer activityLogProducer;

    @Value("${app.email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Transactional
    public MessageResponse register(RegisterRequest req, HttpServletRequest httpReq) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }

        User user = User.builder()
                .email(req.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .phone(req.phone())
                .role(Role.CANDIDATE)
                .oauthProvider(OAuthProvider.LOCAL)
                .personalDataConsent(req.personalDataConsent())
                .pushNotificationConsent(req.pushNotificationConsent() != null && req.pushNotificationConsent())
                .emailVerified(!emailVerificationEnabled)
                .build();

        user = userRepository.save(user);

        if (emailVerificationEnabled) {
            String confirmToken = redisTokenService.createConfirmationToken(user.getId());
            emailService.sendConfirmationEmail(user.getEmail(), confirmToken);
        }

        kafkaProducer.sendUserRegistered(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()));

        auditService.log(user.getId(), user.getEmail(), AuditAction.REGISTER, true, null, httpReq);

        activityLogProducer.log(
                ActivityAction.USER_REGISTERED, ActivitySource.AUTH_SERVICE,
                user.getId(), user.getEmail(), user.getRole().name(),
                "User", user.getId().toString(),
                "User registered: " + user.getEmail());

        log.info("Пользователь зарегистрирован: {}", user.getEmail());
        String message = emailVerificationEnabled
                ? "Регистрация успешна. Проверьте email для подтверждения"
                : "Регистрация успешна. Можете войти в систему";
        return new MessageResponse(message);
    }

    @Transactional
    public MessageResponse confirmEmail(String token) {
        String userId = redisTokenService.validateConfirmationToken(token);
        if (userId == null) {
            throw new InvalidTokenException("Недействительный или просроченный токен подтверждения");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        user.setEmailVerified(true);
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), AuditAction.EMAIL_CONFIRMED, true, null);

        activityLogProducer.log(
                ActivityAction.USER_EMAIL_CONFIRMED, ActivitySource.AUTH_SERVICE,
                user.getId(), user.getEmail(), user.getRole().name(),
                "User", user.getId().toString(),
                "Email confirmed: " + user.getEmail());

        log.info("Email подтверждён: {}", user.getEmail());
        return new MessageResponse("Email успешно подтверждён");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req, HttpServletRequest httpReq) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> {
                    auditService.log(null, req.email(), AuditAction.LOGIN_FAILURE, false,
                            "Пользователь не найден", httpReq);
                    return new BadCredentialsException("Неверный email или пароль");
                });

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            auditService.log(user.getId(), user.getEmail(), AuditAction.LOGIN_FAILURE, false,
                    "Неверный пароль", httpReq);
            throw new BadCredentialsException("Неверный email или пароль");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            auditService.log(user.getId(), user.getEmail(), AuditAction.LOGIN_FAILURE, false,
                    "Email не подтверждён", httpReq);
            throw new EmailNotVerifiedException();
        }

        AuthResponse response = generateTokenPair(user);

        auditService.log(user.getId(), user.getEmail(), AuditAction.LOGIN_SUCCESS, true, null, httpReq);

        activityLogProducer.log(
                ActivityAction.USER_LOGGED_IN, ActivitySource.AUTH_SERVICE,
                user.getId(), user.getEmail(), user.getRole().name(),
                "User", user.getId().toString(),
                "User logged in");

        return response;
    }


    public AuthResponse refresh(RefreshTokenRequest req) {
        String token = req.refreshToken();
        if (!jwtService.isTokenValid(token) || !"refresh".equals(jwtService.getTokenType(token))) {
            throw new InvalidTokenException("Недействительный refresh token");
        }

        String jti = jwtService.getTokenId(token);
        String userId = redisTokenService.validateRefreshToken(jti);
        if (userId == null) {
            throw new InvalidTokenException("Refresh token не найден или отозван");
        }

        redisTokenService.deleteRefreshToken(jti);

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        auditService.log(user.getId(), user.getEmail(), AuditAction.TOKEN_REFRESH, true, null);
        return generateTokenPair(user);
    }

    public MessageResponse logout(String accessToken, RefreshTokenRequest req) {
        if (accessToken != null && jwtService.isTokenValid(accessToken)) {
            String jti = jwtService.getTokenId(accessToken);
            long remainingMs = jwtService.getRemainingExpirationMs(accessToken);
            redisTokenService.blacklistAccessToken(jti, Duration.ofMillis(remainingMs));
        }

        if (req != null && req.refreshToken() != null && jwtService.isTokenValid(req.refreshToken())) {
            String refreshJti = jwtService.getTokenId(req.refreshToken());
            redisTokenService.deleteRefreshToken(refreshJti);
        }

        return new MessageResponse("Выход выполнен успешно");
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email().toLowerCase().trim()).ifPresent(user -> {
            String resetToken = redisTokenService.createResetToken(user.getId());
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Токен сброса пароля отправлен: {}", user.getEmail());
        });
        return new MessageResponse("Если email зарегистрирован, вы получите письмо для сброса пароля");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        String userId = redisTokenService.validateResetToken(req.token());
        if (userId == null) {
            throw new InvalidTokenException("Недействительный или просроченный токен сброса пароля");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        emailService.sendPasswordChangedNotification(user.getEmail());
        auditService.log(user.getId(), user.getEmail(), AuditAction.PASSWORD_RESET, true, null);

        activityLogProducer.log(
                ActivityAction.USER_PASSWORD_RESET, ActivitySource.AUTH_SERVICE,
                user.getId(), user.getEmail(), user.getRole().name(),
                "User", user.getId().toString(),
                "Password reset");

        return new MessageResponse("Пароль успешно изменён");
    }


    @Transactional
    public User processOAuth2User(String email, String firstName, String lastName,
                                  String providerId, OAuthProvider provider) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .map(existingUser -> {
                    if (existingUser.getOauthProvider() == null || existingUser.getOauthProvider() == OAuthProvider.LOCAL) {
                        existingUser.setOauthProvider(provider);
                        existingUser.setOauthProviderId(providerId);
                    }
                    existingUser.setEmailVerified(true);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email.toLowerCase().trim())
                            .firstName(firstName != null ? firstName : "")
                            .lastName(lastName != null ? lastName : "")
                            .role(Role.CANDIDATE)
                            .emailVerified(true)
                            .oauthProvider(provider)
                            .oauthProviderId(providerId)
                            .personalDataConsent(true)
                            .build();
                    newUser = userRepository.save(newUser);

                    kafkaProducer.sendUserRegistered(new UserRegisteredEvent(
                            newUser.getId(), newUser.getEmail(), newUser.getFirstName(), newUser.getLastName()));

                    return newUser;
                });
    }

    public AuthResponse generateTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String refreshJti = jwtService.getTokenId(refreshToken);
        redisTokenService.saveRefreshToken(user.getId(), refreshJti,
                Duration.ofMillis(jwtService.getRefreshExpirationMs()));

        return new AuthResponse(accessToken, refreshToken);
    }
}


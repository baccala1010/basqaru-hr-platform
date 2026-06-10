package kz.whosnext.hr.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private static final String CONFIRM_PREFIX = "auth:confirm:";
    private static final String RESET_PREFIX = "auth:reset:";
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private static final Duration CONFIRM_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    public String createConfirmationToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(CONFIRM_PREFIX + token, userId.toString(), CONFIRM_TTL);
        log.debug("Создан токен подтверждения email для userId={}", userId);
        return token;
    }

    public String validateConfirmationToken(String token) {
        return redis.opsForValue().getAndDelete(CONFIRM_PREFIX + token);
    }

    public String createResetToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(RESET_PREFIX + token, userId.toString(), RESET_TTL);
        log.debug("Создан токен сброса пароля для userId={}", userId);
        return token;
    }

    public String validateResetToken(String token) {
        return redis.opsForValue().getAndDelete(RESET_PREFIX + token);
    }

    public void saveRefreshToken(UUID userId, String jti, Duration ttl) {
        redis.opsForValue().set(REFRESH_PREFIX + jti, userId.toString(), ttl);
    }

    public String validateRefreshToken(String jti) {
        return redis.opsForValue().get(REFRESH_PREFIX + jti);
    }

    public void deleteRefreshToken(String jti) {
        redis.delete(REFRESH_PREFIX + jti);
    }

    public void blacklistAccessToken(String jti, Duration ttl) {
        if (ttl.isPositive()) {
            redis.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
            log.debug("Access token добавлен в blacklist, jti={}", jti);
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + jti));
    }
}

